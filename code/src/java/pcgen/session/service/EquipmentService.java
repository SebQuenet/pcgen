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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import pcgen.facade.core.CharacterFacade;
import pcgen.facade.core.EquipmentFacade;
import pcgen.facade.core.EquipmentListFacade;
import pcgen.facade.core.EquipmentSetFacade;
import pcgen.facade.core.InfoFactory;
import pcgen.gui2.facade.EquipNode;
import pcgen.session.HeadlessUIDelegate;
import pcgen.session.PcgenSession;
import pcgen.session.service.model.BatchPurchase;
import pcgen.session.service.model.FundsSet;
import pcgen.session.service.model.Inventory;
import pcgen.session.service.model.InventoryItem;
import pcgen.session.service.model.PurchasedItem;
import pcgen.session.service.model.SoldItem;

/**
 * Buying, selling, and looking at what a character carries.
 *
 * <p>
 * PCGen reports a refused purchase through the chooser delegate rather than by
 * throwing, so what the delegate last said is read back after every purchase and
 * turned into a refusal.
 */
public final class EquipmentService
{
	private final PcgenSession session;
	private final CharacterLookup characters;

	public EquipmentService(PcgenSession session)
	{
		this.session = session;
		this.characters = new CharacterLookup(session);
	}

	public ServiceResult<PurchasedItem> buyEquipment(String characterId, String equipmentKey, int quantity,
		boolean free)
	{
		return characters.byId(characterId).andThen(character ->
			findInDataSet(character, equipmentKey).andThen(item -> {
				EquipmentFacade sized = character.getEquipmentSizedForCharacter(item);
				HeadlessUIDelegate delegate = session.getDelegate(characterId);
				forgetWhatTheDelegateSaid(delegate);
				character.addPurchasedEquipment(sized, quantity, false, free);

				String refusal = whatTheDelegateSaid(delegate);
				if (refusal != null)
				{
					return ServiceResult.failure(new ServiceError.NotAllowed(refusal));
				}
				return ServiceResult.success(new PurchasedItem(sized.toString(), quantity,
					character.getInfoFactory().getCost(sized), character.getFundsRef().get()));
			}));
	}

	public ServiceResult<SoldItem> sellEquipment(String characterId, String equipmentKey, int quantity, boolean free)
	{
		return characters.byId(characterId).andThen(character -> {
			EquipmentFacade owned = findInInventory(character, equipmentKey);
			if (owned == null)
			{
				return ServiceResult.failure(new ServiceError.NotAllowed("Character does not own: " + equipmentKey));
			}
			character.removePurchasedEquipment(owned, quantity, free);
			return ServiceResult.success(new SoldItem(owned.toString(), quantity, character.getFundsRef().get()));
		});
	}

	public ServiceResult<Inventory> getInventory(String characterId)
	{
		return characters.byId(characterId).map(character -> {
			EquipmentListFacade owned = character.getPurchasedEquipment();
			InfoFactory info = character.getInfoFactory();
			List<InventoryItem> items = new ArrayList<>();
			for (EquipmentFacade item : owned)
			{
				items.add(new InventoryItem(item.getKeyName(), item.toString(), owned.getQuantity(item),
					info.getCost(item), info.getWeight(item)));
			}
			return new Inventory(List.copyOf(items), character.getFundsRef().get(),
				character.getCarriedWeightRef().get(), character.getLoadRef().get());
		});
	}

	public ServiceResult<FundsSet> setFunds(String characterId, double amount)
	{
		return characters.byId(characterId).map(character -> {
			character.setFunds(BigDecimal.valueOf(amount));
			return new FundsSet(character.getFundsRef().get());
		});
	}

	/**
	 * Buy a list of items, each optionally worn straight away. Wearing is
	 * best-effort: an item that cannot go in the slot asked for is still bought.
	 */
	public ServiceResult<BatchPurchase> batchBuyEquipment(String characterId, List<PurchaseRequest> requests)
	{
		return characters.byId(characterId).map(character -> {
			HeadlessUIDelegate delegate = session.getDelegate(characterId);
			List<String> errors = new ArrayList<>();
			int accepted = 0;

			for (PurchaseRequest request : requests)
			{
				EquipmentFacade item = matching(character.getDataSet().getEquipment(), request.equipmentKey());
				if (item == null)
				{
					errors.add("Equipment not found: " + request.equipmentKey());
					continue;
				}
				try
				{
					EquipmentFacade sized = character.getEquipmentSizedForCharacter(item);
					forgetWhatTheDelegateSaid(delegate);
					character.addPurchasedEquipment(sized, request.quantity(), false, request.free());

					String refusal = whatTheDelegateSaid(delegate);
					if (refusal != null)
					{
						errors.add("Buy failed: " + request.equipmentKey() + " - " + refusal);
						continue;
					}
					wearIfAsked(character, sized.toString(), request.slot(), request.quantity());
					accepted++;
				}
				catch (RuntimeException e)
				{
					errors.add("Failed: " + request.equipmentKey() + " - " + e.getMessage());
				}
			}
			return new BatchPurchase(accepted, requests.size(), character.getFundsRef().get(), List.copyOf(errors));
		});
	}

	/** One line of a shopping list: what to buy, how many, free, and where to wear it. */
	public record PurchaseRequest(String equipmentKey, int quantity, boolean free, String slot)
	{
	}

	private static void wearIfAsked(CharacterFacade character, String itemName, String slot, int quantity)
	{
		if (slot == null || slot.isBlank())
		{
			return;
		}
		EquipmentSetFacade equipmentSet = character.getEquipmentSetRef().get();
		EquipmentFacade owned = findInInventory(character, itemName);
		if (equipmentSet == null || owned == null)
		{
			return;
		}
		for (EquipNode node : equipmentSet.getNodes())
		{
			if (node.getNodeType() == EquipNode.NodeType.PHANTOM_SLOT
				&& node.toString().equalsIgnoreCase(slot)
				&& equipmentSet.canEquip(node, owned))
			{
				equipmentSet.addEquipment(node, owned, quantity);
				return;
			}
		}
	}

	private static ServiceResult<EquipmentFacade> findInDataSet(CharacterFacade character, String equipmentKey)
	{
		EquipmentFacade item = matching(character.getDataSet().getEquipment(), equipmentKey);
		return item == null
			? ServiceResult.failure(new ServiceError.EntryNotFound("Equipment", equipmentKey))
			: ServiceResult.success(item);
	}

	static EquipmentFacade findInInventory(CharacterFacade character, String equipmentKey)
	{
		return matching(character.getPurchasedEquipment(), equipmentKey);
	}

	private static EquipmentFacade matching(Iterable<EquipmentFacade> candidates, String wanted)
	{
		for (EquipmentFacade candidate : candidates)
		{
			if (candidate.toString().equalsIgnoreCase(wanted) || candidate.getKeyName().equalsIgnoreCase(wanted))
			{
				return candidate;
			}
		}
		return null;
	}

	private static void forgetWhatTheDelegateSaid(HeadlessUIDelegate delegate)
	{
		if (delegate != null)
		{
			delegate.consumeLastError();
			delegate.consumeLastInfo();
		}
	}

	/** The complaint PCGen made through the delegate, error or notice, or null. */
	private static String whatTheDelegateSaid(HeadlessUIDelegate delegate)
	{
		if (delegate == null)
		{
			return null;
		}
		String error = delegate.consumeLastError();
		return error != null ? error : delegate.consumeLastInfo();
	}
}
