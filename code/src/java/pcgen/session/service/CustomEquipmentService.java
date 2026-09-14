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
import java.util.Map;

import pcgen.cdom.enumeration.ObjectKey;
import pcgen.cdom.enumeration.StringKey;
import pcgen.cdom.reference.CDOMDirectSingleRef;
import pcgen.core.Equipment;
import pcgen.core.EquipmentModifier;
import pcgen.core.Globals;
import pcgen.core.PlayerCharacter;
import pcgen.core.analysis.ChooseActivation;
import pcgen.facade.core.CharacterFacade;
import pcgen.facade.core.DataSetFacade;
import pcgen.facade.core.EquipmentBuilderFacade.EquipmentHead;
import pcgen.facade.core.EquipmentFacade;
import pcgen.facade.util.ListFacade;
import pcgen.gui2.facade.EquipmentBuilderFacadeImpl;
import pcgen.rules.context.LoadContext;
import pcgen.session.HeadlessUIDelegate;
import pcgen.session.PcgenSession;
import pcgen.session.service.model.CustomItemCreated;
import pcgen.session.service.model.NamedEntry;

/**
 * Building magic and masterwork items out of a base item and a set of modifiers.
 *
 * <p>
 * A custom item is added to the loaded data, not to the character. Buying it is
 * a separate step, which is what lets the same item be bought more than once.
 */
public final class CustomEquipmentService
{
	private final PcgenSession session;
	private final CharacterLookup characters;

	public CustomEquipmentService(PcgenSession session)
	{
		this.session = session;
		this.characters = new CharacterLookup(session);
	}

	public ServiceResult<List<NamedEntry>> listEquipmentModifiers(String characterId, String equipmentKey,
		String head)
	{
		return characters.byId(characterId).andThen(character ->
			baseItem(character.getDataSet(), equipmentKey).map(baseItem -> {
				EquipmentBuilderFacadeImpl builder = builderFor(characterId, character, baseItem);
				List<NamedEntry> modifiers = new ArrayList<>();
				for (EquipmentModifier modifier : builder.getAvailList(headNamed(head)))
				{
					modifiers.add(new NamedEntry(modifier.getKeyName(), modifier.getDisplayName()));
				}
				return List.copyOf(modifiers);
			}));
	}

	public ServiceResult<CustomItemCreated> customizeEquipment(String characterId, CustomisationRequest request)
	{
		return characters.byId(characterId).andThen(character ->
			baseItem(character.getDataSet(), request.equipmentKey()).map(baseItem -> {
				EquipmentBuilderFacadeImpl builder = builderFor(characterId, character, baseItem);
				EquipmentHead head = headNamed(request.head());
				HeadlessUIDelegate delegate = session.getDelegate(characterId);

				List<String> applied = new ArrayList<>();
				List<String> refused = new ArrayList<>();
				for (String modifierKey : request.modifierKeys())
				{
					applyModifier(builder, head, modifierKey, request.choices(), delegate, applied, refused);
				}

				if (request.customName() != null && !request.customName().isBlank())
				{
					builder.setName(request.customName());
				}

				Equipment item = finishedItem(builder);
				String itemId = request.customName() == null || request.customName().isBlank()
					? item.toString() : request.customName();
				LoadContext context = Globals.getContext();
				refused.addAll(EquipmentTokenSupport.applySpellAbilities(
					context, item, itemId, request.spellAbilities()));
				refused.addAll(EquipmentTokenSupport.applyTokens(context, item, request.extraTokens()));

				character.getDataSet().addEquipment(item);
				return new CustomItemCreated(item.toString(), List.copyOf(applied), List.copyOf(refused),
					character.getInfoFactory().getCost(item),
					"Item created in dataset. Use buy_equipment with name '" + item + "' to purchase it.");
			}));
	}

	/**
	 * What to build: the base item, the modifiers to put on it, and the answers to
	 * any question those modifiers ask.
	 */
	public record CustomisationRequest(
		String equipmentKey,
		List<String> modifierKeys,
		String head,
		String customName,
		Map<String, List<String>> choices,
		List<EquipmentTokenSupport.SpellAbility> spellAbilities,
		List<String> extraTokens)
	{
	}

	private static void applyModifier(EquipmentBuilderFacadeImpl builder, EquipmentHead head, String modifierKey,
		Map<String, List<String>> choices, HeadlessUIDelegate delegate, List<String> applied, List<String> refused)
	{
		EquipmentModifier modifier = availableModifier(builder, head, modifierKey);
		if (modifier == null)
		{
			refused.add(modifierKey + " (not found/not available)");
			return;
		}
		try
		{
			preSelect(delegate, modifier, modifierKey, choices);
			if (builder.addModToEquipment(modifier, head))
			{
				applied.add(modifier.getDisplayName());
			}
			else
			{
				refused.add(modifierKey + " (add failed)");
			}
		}
		finally
		{
			if (delegate != null)
			{
				delegate.clearPreSelectedChoices();
			}
		}
	}

	/**
	 * Answer the question a modifier asks, if the caller supplied one. Equipment
	 * modifiers carry their choice in CHOICE_STRING rather than in CHOOSE_INFO, so
	 * both are checked.
	 */
	private static void preSelect(HeadlessUIDelegate delegate, EquipmentModifier modifier, String modifierKey,
		Map<String, List<String>> choices)
	{
		boolean asksAQuestion = ChooseActivation.hasNewChooseToken(modifier)
			|| !modifier.getSafe(StringKey.CHOICE_STRING).isEmpty();
		if (!asksAQuestion || choices == null || delegate == null)
		{
			return;
		}
		List<String> answer = choices.get(modifierKey);
		if (answer == null)
		{
			answer = choices.get(modifier.getDisplayName());
		}
		if (answer != null && !answer.isEmpty())
		{
			delegate.setPreSelectedChoices(answer);
		}
	}

	private EquipmentBuilderFacadeImpl builderFor(String characterId, CharacterFacade character, Equipment baseItem)
	{
		PlayerCharacter playerCharacter = session.getPlayerCharacter(characterId);
		Equipment beingBuilt = baseItem.clone();
		beingBuilt.put(ObjectKey.BASE_ITEM, CDOMDirectSingleRef.getRef(baseItem));
		return new EquipmentBuilderFacadeImpl(beingBuilt, playerCharacter, session.getDelegate(characterId));
	}

	private static Equipment finishedItem(EquipmentBuilderFacadeImpl builder)
	{
		EquipmentFacade built = builder.getEquipment();
		if (built instanceof Equipment item)
		{
			return item;
		}
		throw new IllegalStateException("The equipment builder produced something that is not equipment");
	}

	private static EquipmentModifier availableModifier(EquipmentBuilderFacadeImpl builder, EquipmentHead head,
		String modifierKey)
	{
		ListFacade<EquipmentModifier> available = builder.getAvailList(head);
		for (int index = 0; index < available.getSize(); index++)
		{
			EquipmentModifier modifier = available.getElementAt(index);
			if (modifier.getKeyName().equalsIgnoreCase(modifierKey)
				|| modifier.getDisplayName().equalsIgnoreCase(modifierKey))
			{
				return modifier;
			}
		}
		return null;
	}

	private static ServiceResult<Equipment> baseItem(DataSetFacade dataSet, String equipmentKey)
	{
		for (EquipmentFacade candidate : dataSet.getEquipment())
		{
			if (candidate instanceof Equipment item
				&& (item.getKeyName().equalsIgnoreCase(equipmentKey)
					|| item.getDisplayName().equalsIgnoreCase(equipmentKey)
					|| item.toString().equalsIgnoreCase(equipmentKey)))
			{
				return ServiceResult.success(item);
			}
		}
		return ServiceResult.failure(new ServiceError.EntryNotFound("Equipment", equipmentKey));
	}

	private static EquipmentHead headNamed(String head)
	{
		return "SECONDARY".equalsIgnoreCase(head) ? EquipmentHead.SECONDARY : EquipmentHead.PRIMARY;
	}
}
