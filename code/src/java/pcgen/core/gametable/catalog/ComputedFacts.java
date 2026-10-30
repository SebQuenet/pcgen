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

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import pcgen.base.util.NamedValue;
import pcgen.cdom.base.CDOMObject;
import pcgen.cdom.content.CNAbility;
import pcgen.core.AbilityCategory;
import pcgen.core.Globals;
import pcgen.core.Language;
import pcgen.core.PCAlignment;
import pcgen.core.PCClass;
import pcgen.core.PCStat;
import pcgen.core.PCTemplate;
import pcgen.core.PlayerCharacter;
import pcgen.core.Skill;
import pcgen.core.Vision;
import pcgen.core.analysis.SkillModifier;
import pcgen.core.analysis.SkillRankControl;
import pcgen.core.character.CharacterSpell;
import pcgen.core.conjurations.CreatureAttack;
import pcgen.core.conjurations.CreatureStats;
import pcgen.core.display.CharacterDisplay;
import pcgen.core.gametable.CombatantSheets;
import pcgen.io.ExportHandler;

/**
 * The values of a built creature, read on the character PCGen computed: its keys, not its translated names.
 */
public final class ComputedFacts
{
	private static final String SPECIAL_ABILITY = "Special Ability";
	private static final String IMMUNITY_PREFIX = "Immunity to ";
	private static final List<String> ENERGIES = List.of("Acid", "Cold", "Electricity", "Fire", "Sonic");
	private static final Map<String, CatalogField> SPEEDS = Map.of("walk", CatalogField.LAND_SPEED, "fly",
		CatalogField.FLY_SPEED, "swim", CatalogField.SWIM_SPEED, "climb", CatalogField.CLIMB_SPEED, "burrow",
		CatalogField.BURROW_SPEED);
	private static final Map<String, CatalogField> STATS = Map.of("STR", CatalogField.STRENGTH, "DEX",
		CatalogField.DEXTERITY, "CON", CatalogField.CONSTITUTION, "INT", CatalogField.INTELLIGENCE, "WIS",
		CatalogField.WISDOM, "CHA", CatalogField.CHARISMA);
	private static final Pattern FIRST_NUMBER = Pattern.compile("-?\\d+");
	private static final Pattern DAMAGE_REDUCTION = Pattern.compile("(\\d+)\\s*/\\s*([^;,]+)");

	private ComputedFacts()
	{
	}

	/**
	 * @param monster a built creature.
	 * @param sheets  what reads its numbers as the game table shows them.
	 * @return its values.
	 */
	public static Map<CatalogField, FieldValue> of(PlayerCharacter monster, CombatantSheets sheets)
	{
		Map<CatalogField, FieldValue> facts = new EnumMap<>(CatalogField.class);
		CharacterDisplay display = monster.getDisplay();
		CreatureStats stats = sheets.stats(monster);
		Set<String> abilities = specialAbilities(monster);

		nature(facts, monster, display);
		number(sheets.challengeRating(monster)).ifPresent(cr -> facts.put(CatalogField.CHALLENGE_RATING, number(cr)));
		facts.put(CatalogField.HIT_DICE, number(stats.hitDice()));
		facts.put(CatalogField.HIT_POINTS, number(stats.maximumHitPoints()));
		facts.put(CatalogField.MYTHIC_RANK, number(tokenNumber(monster, "VAR.MythicTier.INTVAL")));
		facts.put(CatalogField.TEMPLATES, items(display.getTemplateSet().stream().map(PCTemplate::getKeyName)));

		defense(facts, monster, display, stats, abilities);
		attack(facts, monster, stats);
		movement(facts, monster, display);
		senses(facts, monster, display, abilities);
		magic(facts, monster, display);
		abilityScores(facts, display);

		facts.put(CatalogField.LANGUAGES, items(display.getLanguageSet().stream().map(Language::getKeyName)));
		facts.put(CatalogField.TELEPATHY, new FieldValue.Flag(abilities.stream().anyMatch(key -> key.startsWith("Telepathy"))));
		facts.put(CatalogField.SPECIAL_ABILITIES, new FieldValue.Items(abilities));
		facts.put(CatalogField.SKILLS, skills(monster, display));
		facts.put(CatalogField.HAS_TACTICS, new FieldValue.Flag(display.getTacticalPlan().isPresent()));
		return facts;
	}

	private static void nature(Map<CatalogField, FieldValue> facts, PlayerCharacter monster, CharacterDisplay display)
	{
		String type = display.getRaceType();
		if (type != null && !type.isBlank())
		{
			facts.put(CatalogField.TYPE, new FieldValue.Items(Set.of(type)));
		}
		facts.put(CatalogField.SUBTYPES, items(display.getRacialSubTypes().stream().map(Object::toString)));
		facts.put(CatalogField.SIZE, number(monster.sizeInt()));
		PCAlignment alignment = display.getPCAlignment();
		String key = alignment == null ? "" : alignment.getKeyName().toUpperCase(Locale.ROOT);
		if (key.length() == 2 && "LNCT".indexOf(key.charAt(0)) >= 0 && "GNE".indexOf(key.charAt(1)) >= 0)
		{
			facts.put(CatalogField.LAW_CHAOS, new FieldValue.Text(key.charAt(0) == 'T' ? "N" : key.substring(0, 1)));
			facts.put(CatalogField.GOOD_EVIL, new FieldValue.Text(key.substring(1)));
		}
	}

	private static void defense(Map<CatalogField, FieldValue> facts, PlayerCharacter monster, CharacterDisplay display,
		CreatureStats stats, Set<String> abilities)
	{
		facts.put(CatalogField.ARMOR_CLASS, number(stats.armorClass()));
		facts.put(CatalogField.TOUCH_ARMOR_CLASS, number(stats.touchArmorClass()));
		facts.put(CatalogField.FLAT_FOOTED_ARMOR_CLASS, number(stats.flatFootedArmorClass()));
		facts.put(CatalogField.REFLEX, number(firstNumber(stats.reflex())));
		facts.put(CatalogField.FORTITUDE, number(firstNumber(stats.fortitude())));
		facts.put(CatalogField.WILL, number(firstNumber(stats.will())));
		int reduction = 0;
		Set<String> bypass = new HashSet<>();
		Matcher found = DAMAGE_REDUCTION.matcher(display.calcDR() == null ? "" : display.calcDR());
		while (found.find())
		{
			reduction = Math.max(reduction, Integer.parseInt(found.group(1)));
			for (String material : found.group(2).split("\\s+(?:or|and)\\s+"))
			{
				bypass.add(material.strip());
			}
		}
		facts.put(CatalogField.DAMAGE_REDUCTION, number(reduction));
		facts.put(CatalogField.DAMAGE_REDUCTION_BYPASS, new FieldValue.Items(bypass));
		Map<String, Integer> resistances = new HashMap<>();
		for (String energy : ENERGIES)
		{
			int resistance = tokenNumber(monster, "VAR." + energy + "ResistanceBonus.INTVAL");
			if (resistance > 0)
			{
				resistances.put(energy, resistance);
			}
		}
		facts.put(CatalogField.RESISTANCES, new FieldValue.Scores(resistances));
		facts.put(CatalogField.IMMUNITIES, items(abilities.stream().filter(key -> key.startsWith(IMMUNITY_PREFIX))
			.map(key -> key.substring(IMMUNITY_PREFIX.length()))));
		facts.put(CatalogField.SPELL_RESISTANCE, number(monster.calcSR(true)));
	}

	private static void attack(Map<CatalogField, FieldValue> facts, PlayerCharacter monster, CreatureStats stats)
	{
		facts.put(CatalogField.BASE_ATTACK, number(monster.baseAttackBonus()));
		facts.put(CatalogField.COMBAT_MANEUVER_BONUS, number(firstNumber(stats.combatManeuverBonus())));
		facts.put(CatalogField.COMBAT_MANEUVER_DEFENSE, number(stats.combatManeuverDefense()));
		facts.put(CatalogField.ATTACK_COUNT, number(stats.attacks().size()));
		facts.put(CatalogField.ATTACKS, items(stats.attacks().stream().map(CreatureAttack::name)));
		facts.put(CatalogField.RANGED_ATTACK, new FieldValue.Flag(monster.getEquippedEquipmentSet().stream()
			.anyMatch(equipment -> equipment.isWeapon() && equipment.isRanged())));
		facts.put(CatalogField.REACH, number(tokenNumber(monster, "REACH")));
	}

	private static void movement(Map<CatalogField, FieldValue> facts, PlayerCharacter monster, CharacterDisplay display)
	{
		SPEEDS.values().forEach(field -> facts.put(field, number(0)));
		for (NamedValue speed : display.getMovementValues())
		{
			CatalogField field = SPEEDS.get(speed.getName().toLowerCase(Locale.ROOT));
			if (field != null)
			{
				facts.put(field, number(speed.getWeight()));
			}
		}
		monster.getAggregateAbilityListNoDuplicates(specialAbilityCategory()).stream()
			.filter(ability -> ability.isType("Maneuverability")).findFirst()
			.ifPresent(ability -> facts.put(CatalogField.FLY_MANEUVERABILITY, new FieldValue.Text(ability.getKeyName())));
	}

	private static void senses(Map<CatalogField, FieldValue> facts, PlayerCharacter monster, CharacterDisplay display,
		Set<String> abilities)
	{
		Map<String, Integer> ranges = new HashMap<>();
		for (Vision vision : display.getVisionList())
		{
			String type = vision.getType().toString().toLowerCase(Locale.ROOT).replace("-", "").replace(" ", "");
			ranges.merge(type, vision.getDistance().resolve(monster, "").intValue(), Math::max);
		}
		facts.put(CatalogField.DARKVISION, number(ranges.getOrDefault("darkvision", 0)));
		facts.put(CatalogField.LOW_LIGHT_VISION, new FieldValue.Flag(ranges.containsKey("lowlight")));
		facts.put(CatalogField.TREMORSENSE, number(sense(ranges, abilities, "tremorsense")));
		facts.put(CatalogField.BLINDSENSE, number(sense(ranges, abilities, "blindsense")));
		facts.put(CatalogField.BLINDSIGHT, number(sense(ranges, abilities, "blindsight")));
		facts.put(CatalogField.SCENT, new FieldValue.Flag(abilities.stream().anyMatch(key -> key.startsWith("Scent"))));
	}

	private static int sense(Map<String, Integer> ranges, Set<String> abilities, String name)
	{
		if (ranges.containsKey(name))
		{
			return ranges.get(name);
		}
		return abilities.stream().filter(key -> key.toLowerCase(Locale.ROOT).startsWith(name))
			.map(key -> firstNumber(key)).filter(range -> range > 0).findFirst()
			.orElse(abilities.stream().anyMatch(key -> key.toLowerCase(Locale.ROOT).startsWith(name)) ? 1 : 0);
	}

	private static void magic(Map<CatalogField, FieldValue> facts, PlayerCharacter monster, CharacterDisplay display)
	{
		facts.put(CatalogField.SPELL_LIKE_ABILITIES, items(spellKeys(display, monster.getRace())));
		Set<String> spells = new HashSet<>();
		Set<String> casters = new HashSet<>();
		int casterLevel = 0;
		for (PCClass pcClass : display.getClassSet())
		{
			Set<String> known = spellKeys(display, pcClass).collect(Collectors.toSet());
			if (!known.isEmpty())
			{
				spells.addAll(known);
				casters.add(pcClass.getKeyName());
				casterLevel = Math.max(casterLevel, monster.getCasterLevelForClass(pcClass));
			}
		}
		facts.put(CatalogField.SPELLS, new FieldValue.Items(spells));
		facts.put(CatalogField.CASTER_CLASSES, new FieldValue.Items(casters));
		facts.put(CatalogField.CASTER_LEVEL, number(casterLevel));
	}

	private static Stream<String> spellKeys(CharacterDisplay display, CDOMObject source)
	{
		return source == null ? Stream.empty()
			: display.getCharacterSpells(source).stream().map(CharacterSpell::getSpell).filter(Objects::nonNull)
				.map(spell -> spell.getKeyName());
	}

	private static void abilityScores(Map<CatalogField, FieldValue> facts, CharacterDisplay display)
	{
		for (PCStat stat : display.getStatSet())
		{
			CatalogField field = STATS.get(stat.getKeyName().toUpperCase(Locale.ROOT));
			boolean isAbsent = display.isNonAbility(stat);
			if (field != null && !isAbsent)
			{
				facts.put(field, number(display.getTotalStatFor(stat)));
			}
			if ("CON".equalsIgnoreCase(stat.getKeyName()))
			{
				facts.put(CatalogField.NO_CONSTITUTION, new FieldValue.Flag(isAbsent));
			}
			if ("INT".equalsIgnoreCase(stat.getKeyName()))
			{
				facts.put(CatalogField.NO_INTELLIGENCE, new FieldValue.Flag(isAbsent));
			}
		}
	}

	private static FieldValue skills(PlayerCharacter monster, CharacterDisplay display)
	{
		Map<String, Integer> totals = new HashMap<>();
		for (Skill skill : display.getSkillSet())
		{
			Integer modifier = SkillModifier.modifier(skill, monster);
			Float ranks = SkillRankControl.getTotalRank(monster, skill);
			totals.put(skill.getKeyName(), (modifier == null ? 0 : modifier) + (ranks == null ? 0 : ranks.intValue()));
		}
		return new FieldValue.Scores(totals);
	}

	private static Set<String> specialAbilities(PlayerCharacter monster)
	{
		AbilityCategory category = specialAbilityCategory();
		return category == null ? Set.of()
			: monster.getCNAbilities(category).stream().map(CNAbility::getAbilityKey).collect(Collectors.toSet());
	}

	private static AbilityCategory specialAbilityCategory()
	{
		return Globals.getContext().getReferenceContext().silentlyGetConstructedCDOMObject(AbilityCategory.class,
			SPECIAL_ABILITY);
	}

	private static int tokenNumber(PlayerCharacter monster, String token)
	{
		String value = ExportHandler.getTokenString(monster, token);
		return value == null ? 0 : firstNumber(value);
	}

	private static int firstNumber(String text)
	{
		Matcher found = FIRST_NUMBER.matcher(text == null ? "" : text);
		return found.find() ? Integer.parseInt(found.group()) : 0;
	}

	private static FieldValue.Number number(double value)
	{
		return new FieldValue.Number(value);
	}

	private static OptionalDouble number(String text)
	{
		return Criterion.Test.number(text);
	}

	private static FieldValue.Items items(Stream<String> values)
	{
		return new FieldValue.Items(values.collect(Collectors.toSet()));
	}
}
