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
package pcgen.core.bonus;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import pcgen.cdom.base.CDOMObject;
import pcgen.cdom.base.Constants;
import pcgen.cdom.enumeration.ListKey;
import pcgen.core.Ability;
import pcgen.core.AbilityCategory;
import pcgen.core.Equipment;
import pcgen.core.GameMode;
import pcgen.core.Globals;
import pcgen.core.PCTemplate;
import pcgen.core.PlayerCharacter;
import pcgen.core.SettingsHandler;
import pcgen.core.character.CharacterSpell;
import pcgen.core.conjurations.BonusSourceKey;
import pcgen.core.spell.Spell;
import pcgen.rules.context.AbstractReferenceContext;

/**
 * The rules objects of the loaded data that grant temporary bonuses to a
 * character: those anyone can lay on another, and those a character holds
 * itself. Equipment-only bonuses are left out.
 */
public final class TemporaryBonusSources
{
	private TemporaryBonusSources()
	{
	}

	/**
	 * @return every ability, spell and template of the loaded data whose bonus can be
	 *         laid on another character, such as Prayer.
	 */
	public static List<CDOMObject> castableOnOthers()
	{
		AbstractReferenceContext data = Globals.getContext().getReferenceContext();
		List<CDOMObject> sources = new ArrayList<>();
		for (AbilityCategory category : SettingsHandler.getGameAsProperty().get().getAllAbilityCategories())
		{
			if (category.getParentCategory() == category)
			{
				data.getManufacturerId(category).getAllObjects().stream().filter(TemporaryBonusSources::isCastable)
					.forEach(sources::add);
			}
		}
		data.getConstructedCDOMObjects(Spell.class).stream().filter(TemporaryBonusSources::isCastable)
			.forEach(sources::add);
		data.getConstructedCDOMObjects(PCTemplate.class).stream().filter(TemporaryBonusSources::isCastable)
			.forEach(sources::add);
		return sources;
	}

	/**
	 * @param character a character, such as a conjured creature.
	 * @return what it can lay on itself: the bonuses of its own abilities, spells and
	 *         items, then everything {@link #castableOnOthers()} holds.
	 */
	public static List<CDOMObject> availableTo(PlayerCharacter character)
	{
		Set<CDOMObject> sources = new LinkedHashSet<>();
		character.getCDOMObjectList().stream().filter(TemporaryBonusSources::grantsACharacter).forEach(sources::add);
		character.aggregateSpellList("", "", "", 0, 9).stream().filter(TemporaryBonusSources::grantsACharacter)
			.forEach(sources::add);
		for (CharacterSpell innate : character.getCharacterSpells(character.getDisplay().getRace(),
			Constants.INNATE_SPELL_BOOK_NAME))
		{
			if (innate != null && grantsACharacter(innate.getSpell()))
			{
				sources.add(innate.getSpell());
			}
		}
		sources.addAll(castableOnOthers());
		return List.copyOf(sources);
	}

	/**
	 * @param source a rules object granting temporary bonuses.
	 * @return the key naming it in a saved character.
	 */
	public static BonusSourceKey keyOf(CDOMObject source)
	{
		return switch (source)
		{
			case Ability ability -> new BonusSourceKey(BonusSourceKey.ABILITY,
				ability.getCDOMCategory().getKeyName(), ability.getKeyName());
			case Spell spell -> new BonusSourceKey(BonusSourceKey.SPELL, "", spell.getKeyName());
			case PCTemplate template -> new BonusSourceKey(BonusSourceKey.TEMPLATE, "", template.getKeyName());
			case Equipment item -> new BonusSourceKey(BonusSourceKey.EQUIPMENT, "", item.getKeyName());
			default -> throw new IllegalArgumentException(
				"A temporary bonus comes from an ability, a spell, a template or an item, not " + source);
		};
	}

	/**
	 * @param key the key of a rules object.
	 * @return that object in the loaded data, or nothing when the data lacks it.
	 */
	public static Optional<CDOMObject> find(BonusSourceKey key)
	{
		AbstractReferenceContext data = Globals.getContext().getReferenceContext();
		return Optional.ofNullable(switch (key.kind())
		{
			case BonusSourceKey.ABILITY -> abilityOf(data, key);
			case BonusSourceKey.SPELL -> data.silentlyGetConstructedCDOMObject(Spell.class, key.key());
			case BonusSourceKey.TEMPLATE -> data.silentlyGetConstructedCDOMObject(PCTemplate.class, key.key());
			default -> data.silentlyGetConstructedCDOMObject(Equipment.class, key.key());
		});
	}

	/**
	 * @param key the key of a rules object.
	 * @return its name in the player's language, or the key itself when the data lacks it.
	 */
	public static String labelOf(BonusSourceKey key)
	{
		return find(key).map(CDOMObject::getDisplayName).orElse(key.key());
	}

	private static Ability abilityOf(AbstractReferenceContext data, BonusSourceKey key)
	{
		GameMode game = SettingsHandler.getGameAsProperty().get();
		AbilityCategory category = game.getAbilityCategory(key.category());
		return (category == null) ? null : data.getManufacturerId(category).getActiveObject(key.key());
	}

	private static boolean isCastable(CDOMObject source)
	{
		return source != null && source.containsListFor(ListKey.BONUS_ANYPC);
	}

	private static boolean grantsACharacter(CDOMObject source)
	{
		return isCastable(source) || (source != null && source.containsListFor(ListKey.BONUS_PC));
	}
}
