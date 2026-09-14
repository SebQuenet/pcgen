/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package pcgen.session.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pcgen.facade.core.CharacterFacade;
import pcgen.facade.core.EquipmentFacade;
import pcgen.facade.core.EquipmentSetFacade;
import pcgen.gui2.facade.EquipNode;
import pcgen.session.PcgenSession;
import pcgen.session.service.model.EquipFailure;
import pcgen.session.service.model.EquipOutcome;
import pcgen.session.service.model.EquipmentSetCreated;
import pcgen.session.service.model.EquipmentSetSummary;
import pcgen.session.service.model.EquippedSlot;
import pcgen.session.service.model.ItemEquipped;
import pcgen.session.service.model.ItemUnequipped;

/**
 * What a character is wearing, and the sets of gear they switch between.
 */
public final class EquipmentSetService
{
	private static final String NO_ACTIVE_SET = "No active equipment set";

	private final CharacterLookup characters;

	public EquipmentSetService(PcgenSession session)
	{
		this.characters = new CharacterLookup(session);
	}

	public ServiceResult<List<EquipmentSetSummary>> listEquipmentSets(String characterId)
	{
		return characters.byId(characterId).map(character -> {
			EquipmentSetFacade inUse = character.getEquipmentSetRef().get();
			List<EquipmentSetSummary> sets = new ArrayList<>();
			for (EquipmentSetFacade set : character.getEquipmentSets())
			{
				sets.add(new EquipmentSetSummary(set.getNameRef().get(), set == inUse));
			}
			return List.copyOf(sets);
		});
	}

	public ServiceResult<EquipmentSetCreated> createEquipmentSet(String characterId, String name)
	{
		return characters.byId(characterId).map(character -> {
			EquipmentSetFacade created = character.createEquipmentSet(name);
			character.setEquipmentSet(created);
			return new EquipmentSetCreated(name);
		});
	}

	public ServiceResult<List<EquippedSlot>> getEquippedItems(String characterId)
	{
		return characters.byId(characterId).andThen(character ->
			activeSet(character).map(equipmentSet -> {
				List<EquippedSlot> slots = new ArrayList<>();
				for (EquipNode node : equipmentSet.getNodes())
				{
					EquipmentFacade worn = node.getEquipment();
					slots.add(new EquippedSlot(
						node.toString(),
						node.getNodeType().name(),
						worn == null ? null : worn.toString(),
						worn == null ? null : worn.getKeyName(),
						worn == null ? null : equipmentSet.getQuantity(node)));
				}
				return List.copyOf(slots);
			}));
	}

	public ServiceResult<ItemEquipped> equipItem(String characterId, String equipmentKey, String slot, int quantity)
	{
		return characters.byId(characterId).andThen(character ->
			activeSet(character).andThen(equipmentSet -> {
				EquipmentFacade owned = EquipmentService.findInInventory(character, equipmentKey);
				if (owned == null)
				{
					return ServiceResult.failure(
						new ServiceError.EntryNotFound("Item in inventory", equipmentKey));
				}
				EquipNode target = slotFor(equipmentSet, slot, owned);
				if (target == null)
				{
					return ServiceResult.failure(new ServiceError.EntryNotFound("Slot", slot));
				}
				if (!equipmentSet.canEquip(target, owned))
				{
					return ServiceResult.failure(
						new ServiceError.NotAllowed("Cannot equip " + owned + " in slot " + slot));
				}
				EquipmentFacade worn = equipmentSet.addEquipment(target, owned, quantity);
				return worn == null
					? ServiceResult.failure(new ServiceError.NotAllowed("Failed to equip item"))
					: ServiceResult.success(new ItemEquipped(worn.toString(), slot));
			}));
	}

	public ServiceResult<ItemUnequipped> unequipItem(String characterId, String slot, int quantity)
	{
		return characters.byId(characterId).andThen(character ->
			activeSet(character).andThen(equipmentSet -> {
				EquipNode worn = wornAt(equipmentSet, slot);
				if (worn == null)
				{
					return ServiceResult.failure(
						new ServiceError.EntryNotFound("Equipped item at", slot));
				}
				EquipmentFacade removed = equipmentSet.removeEquipment(worn, quantity);
				return ServiceResult.success(new ItemUnequipped(removed == null ? "item" : removed.toString()));
			}));
	}

	/** Wear several items at once; one that will not go on does not stop the rest. */
	public ServiceResult<EquipOutcome> equipItems(String characterId, List<EquipRequest> requests)
	{
		return characters.byId(characterId).andThen(character ->
			activeSet(character).map(equipmentSet -> {
				List<ItemEquipped> equipped = new ArrayList<>();
				List<EquipFailure> refused = new ArrayList<>();

				for (EquipRequest request : requests)
				{
					EquipmentFacade owned = EquipmentService.findInInventory(character, request.equipmentKey());
					if (owned == null)
					{
						refused.add(new EquipFailure(request.equipmentKey(), request.slot(),
							"Item not found in inventory"));
						continue;
					}
					EquipNode target = slotFor(equipmentSet, request.slot(), owned);
					if (target == null)
					{
						refused.add(new EquipFailure(request.equipmentKey(), request.slot(), "Slot not found"));
						continue;
					}
					if (!equipmentSet.canEquip(target, owned))
					{
						refused.add(new EquipFailure(request.equipmentKey(), request.slot(),
							"Cannot equip in this slot"));
						continue;
					}
					EquipmentFacade worn = equipmentSet.addEquipment(target, owned, request.quantity());
					if (worn == null)
					{
						refused.add(new EquipFailure(request.equipmentKey(), request.slot(), "Equip failed"));
					}
					else
					{
						equipped.add(new ItemEquipped(worn.toString(), request.slot()));
					}
				}
				return new EquipOutcome(equipped.size(), List.copyOf(equipped), List.copyOf(refused));
			}));
	}

	/** One line of a batch: which item goes in which slot, and how many. */
	public record EquipRequest(String equipmentKey, String slot, int quantity)
	{
	}

	private static ServiceResult<EquipmentSetFacade> activeSet(CharacterFacade character)
	{
		EquipmentSetFacade equipmentSet = character.getEquipmentSetRef().get();
		return equipmentSet == null
			? ServiceResult.failure(new ServiceError.NotAllowed(NO_ACTIVE_SET))
			: ServiceResult.success(equipmentSet);
	}

	/**
	 * The slot a caller named. A slot that will actually take the item wins over
	 * one that merely has the right name, and an exact name wins over a partial one.
	 */
	private static EquipNode slotFor(EquipmentSetFacade equipmentSet, String slot, EquipmentFacade item)
	{
		EquipNode namedExactly = null;
		for (EquipNode node : equipmentSet.getNodes())
		{
			if (node.toString().equalsIgnoreCase(slot))
			{
				if (equipmentSet.canEquip(node, item))
				{
					return node;
				}
				namedExactly = namedExactly == null ? node : namedExactly;
			}
		}
		if (namedExactly != null)
		{
			return namedExactly;
		}

		EquipNode namedInPart = null;
		for (EquipNode node : equipmentSet.getNodes())
		{
			if (node.toString().toLowerCase(Locale.ROOT).contains(slot.toLowerCase(Locale.ROOT)))
			{
				if (equipmentSet.canEquip(node, item))
				{
					return node;
				}
				namedInPart = namedInPart == null ? node : namedInPart;
			}
		}
		return namedInPart;
	}

	/** The node holding what a caller named, whether they named the slot or the item. */
	private static EquipNode wornAt(EquipmentSetFacade equipmentSet, String slotOrItem)
	{
		for (EquipNode node : equipmentSet.getNodes())
		{
			EquipmentFacade worn = node.getEquipment();
			if (worn != null && (node.toString().equalsIgnoreCase(slotOrItem)
				|| worn.toString().equalsIgnoreCase(slotOrItem)
				|| worn.getKeyName().equalsIgnoreCase(slotOrItem)))
			{
				return node;
			}
		}
		return null;
	}
}
