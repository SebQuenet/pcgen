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
package pcgen.core.gametable.catalog;

import java.util.Locale;

/**
 * A value of a bestiary entry that the game master filters on or shows as a column.
 */
public enum CatalogField
{
	NAME(CatalogGroup.IDENTITY, FieldKind.TEXT, Origin.DIRECT),
	SOURCE_BOOK(CatalogGroup.IDENTITY, FieldKind.TEXT, Origin.DIRECT),
	/** Where the entry comes from: race, file, kit or scenario. */
	ORIGIN(CatalogGroup.IDENTITY, FieldKind.TEXT, Origin.DIRECT),
	/** Monster, NPC or player race. */
	CATEGORY(CatalogGroup.IDENTITY, FieldKind.TEXT, Origin.DIRECT),
	ROLE(CatalogGroup.IDENTITY, FieldKind.ITEMS, Origin.DIRECT),

	TYPE(CatalogGroup.NATURE, FieldKind.ITEMS, Origin.DIRECT),
	SUBTYPES(CatalogGroup.NATURE, FieldKind.ITEMS, Origin.DIRECT),
	/** From Fine (0) to Colossal (8). */
	SIZE(CatalogGroup.NATURE, FieldKind.NUMBER, Origin.COMPUTED),
	/** L, N or C. */
	LAW_CHAOS(CatalogGroup.NATURE, FieldKind.TEXT, Origin.COMPUTED),
	/** G, N or E. */
	GOOD_EVIL(CatalogGroup.NATURE, FieldKind.TEXT, Origin.COMPUTED),

	CHALLENGE_RATING(CatalogGroup.POWER, FieldKind.NUMBER, Origin.DIRECT),
	HIT_DICE(CatalogGroup.POWER, FieldKind.NUMBER, Origin.DIRECT),
	HIT_POINTS(CatalogGroup.POWER, FieldKind.NUMBER, Origin.COMPUTED),
	MYTHIC_RANK(CatalogGroup.POWER, FieldKind.NUMBER, Origin.COMPUTED),
	TEMPLATES(CatalogGroup.POWER, FieldKind.ITEMS, Origin.COMPUTED),

	ARMOR_CLASS(CatalogGroup.DEFENSE, FieldKind.NUMBER, Origin.COMPUTED),
	TOUCH_ARMOR_CLASS(CatalogGroup.DEFENSE, FieldKind.NUMBER, Origin.COMPUTED),
	FLAT_FOOTED_ARMOR_CLASS(CatalogGroup.DEFENSE, FieldKind.NUMBER, Origin.COMPUTED),
	REFLEX(CatalogGroup.DEFENSE, FieldKind.NUMBER, Origin.COMPUTED),
	FORTITUDE(CatalogGroup.DEFENSE, FieldKind.NUMBER, Origin.COMPUTED),
	WILL(CatalogGroup.DEFENSE, FieldKind.NUMBER, Origin.COMPUTED),
	DAMAGE_REDUCTION(CatalogGroup.DEFENSE, FieldKind.NUMBER, Origin.COMPUTED),
	/** What overcomes the damage reduction: magic, silver, good… */
	DAMAGE_REDUCTION_BYPASS(CatalogGroup.DEFENSE, FieldKind.ITEMS, Origin.COMPUTED),
	/** Resistance by energy, such as Fire 10. */
	RESISTANCES(CatalogGroup.DEFENSE, FieldKind.SCORES, Origin.COMPUTED),
	IMMUNITIES(CatalogGroup.DEFENSE, FieldKind.ITEMS, Origin.COMPUTED),
	SPELL_RESISTANCE(CatalogGroup.DEFENSE, FieldKind.NUMBER, Origin.COMPUTED),

	BASE_ATTACK(CatalogGroup.ATTACK, FieldKind.NUMBER, Origin.COMPUTED),
	COMBAT_MANEUVER_BONUS(CatalogGroup.ATTACK, FieldKind.NUMBER, Origin.COMPUTED),
	COMBAT_MANEUVER_DEFENSE(CatalogGroup.ATTACK, FieldKind.NUMBER, Origin.COMPUTED),
	ATTACK_COUNT(CatalogGroup.ATTACK, FieldKind.NUMBER, Origin.COMPUTED),
	ATTACKS(CatalogGroup.ATTACK, FieldKind.ITEMS, Origin.COMPUTED),
	REACH(CatalogGroup.ATTACK, FieldKind.NUMBER, Origin.COMPUTED),
	RANGED_ATTACK(CatalogGroup.ATTACK, FieldKind.FLAG, Origin.COMPUTED),

	LAND_SPEED(CatalogGroup.MOVEMENT, FieldKind.NUMBER, Origin.COMPUTED),
	FLY_SPEED(CatalogGroup.MOVEMENT, FieldKind.NUMBER, Origin.COMPUTED),
	SWIM_SPEED(CatalogGroup.MOVEMENT, FieldKind.NUMBER, Origin.COMPUTED),
	CLIMB_SPEED(CatalogGroup.MOVEMENT, FieldKind.NUMBER, Origin.COMPUTED),
	BURROW_SPEED(CatalogGroup.MOVEMENT, FieldKind.NUMBER, Origin.COMPUTED),
	FLY_MANEUVERABILITY(CatalogGroup.MOVEMENT, FieldKind.TEXT, Origin.COMPUTED),

	DARKVISION(CatalogGroup.SENSES, FieldKind.NUMBER, Origin.COMPUTED),
	LOW_LIGHT_VISION(CatalogGroup.SENSES, FieldKind.FLAG, Origin.COMPUTED),
	TREMORSENSE(CatalogGroup.SENSES, FieldKind.NUMBER, Origin.COMPUTED),
	BLINDSENSE(CatalogGroup.SENSES, FieldKind.NUMBER, Origin.COMPUTED),
	BLINDSIGHT(CatalogGroup.SENSES, FieldKind.NUMBER, Origin.COMPUTED),
	SCENT(CatalogGroup.SENSES, FieldKind.FLAG, Origin.COMPUTED),

	SPELL_LIKE_ABILITIES(CatalogGroup.MAGIC, FieldKind.ITEMS, Origin.COMPUTED),
	SPELLS(CatalogGroup.MAGIC, FieldKind.ITEMS, Origin.COMPUTED),
	CASTER_CLASSES(CatalogGroup.MAGIC, FieldKind.ITEMS, Origin.COMPUTED),
	CASTER_LEVEL(CatalogGroup.MAGIC, FieldKind.NUMBER, Origin.COMPUTED),

	STRENGTH(CatalogGroup.ABILITY_SCORES, FieldKind.NUMBER, Origin.COMPUTED),
	DEXTERITY(CatalogGroup.ABILITY_SCORES, FieldKind.NUMBER, Origin.COMPUTED),
	CONSTITUTION(CatalogGroup.ABILITY_SCORES, FieldKind.NUMBER, Origin.COMPUTED),
	INTELLIGENCE(CatalogGroup.ABILITY_SCORES, FieldKind.NUMBER, Origin.COMPUTED),
	WISDOM(CatalogGroup.ABILITY_SCORES, FieldKind.NUMBER, Origin.COMPUTED),
	CHARISMA(CatalogGroup.ABILITY_SCORES, FieldKind.NUMBER, Origin.COMPUTED),
	NO_CONSTITUTION(CatalogGroup.ABILITY_SCORES, FieldKind.FLAG, Origin.COMPUTED),
	NO_INTELLIGENCE(CatalogGroup.ABILITY_SCORES, FieldKind.FLAG, Origin.COMPUTED),

	LANGUAGES(CatalogGroup.LANGUAGES, FieldKind.ITEMS, Origin.COMPUTED),
	TELEPATHY(CatalogGroup.LANGUAGES, FieldKind.FLAG, Origin.COMPUTED),

	SPECIAL_ABILITIES(CatalogGroup.ABILITIES, FieldKind.ITEMS, Origin.COMPUTED),

	SKILLS(CatalogGroup.SKILLS, FieldKind.SCORES, Origin.COMPUTED),

	HAS_PAWN(CatalogGroup.GAME_TABLE, FieldKind.FLAG, Origin.DIRECT),
	HAS_TACTICS(CatalogGroup.GAME_TABLE, FieldKind.FLAG, Origin.DIRECT);

	/**
	 * Whether a field is read in the data, or known only once the creature is built.
	 */
	public enum Origin
	{
		DIRECT,
		COMPUTED
	}

	private final CatalogGroup group;
	private final FieldKind kind;
	private final Origin origin;

	CatalogField(CatalogGroup group, FieldKind kind, Origin origin)
	{
		this.group = group;
		this.kind = kind;
		this.origin = origin;
	}

	public CatalogGroup group()
	{
		return group;
	}

	public FieldKind kind()
	{
		return kind;
	}

	public Origin origin()
	{
		return origin;
	}

	/**
	 * @return the key of the field's label in the language bundle.
	 */
	public String labelKey()
	{
		return "in_gametable_field_" + name().toLowerCase(Locale.ROOT);
	}
}
