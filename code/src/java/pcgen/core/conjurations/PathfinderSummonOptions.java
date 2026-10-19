/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package pcgen.core.conjurations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import pcgen.cdom.base.CDOMList;
import pcgen.core.AbilityCategory;
import pcgen.core.Equipment;
import pcgen.core.PCClass;
import pcgen.core.PObject;
import pcgen.core.PlayerCharacter;
import pcgen.core.SettingsHandler;
import pcgen.core.character.CharacterSpell;
import pcgen.core.spell.Spell;

/**
 * The published Pathfinder 1 summoning tables, including additions granted by
 * feats and rings. Only options the character can currently access are added;
 * fixed feat choices must already be recorded in the personal catalog.
 */
public final class PathfinderSummonOptions
{
	private static final String RESOURCE = "/pcgen/conjurations/pathfinder1-options.tsv";
	private static final String VERSATILE_MONSTER_RULE =
		"https://aonprd.com/FeatDisplay.aspx?ItemName=Versatile+Summon+Monster";
	private static final String VERSATILE_NATURE_RULE =
		"https://aonprd.com/FeatDisplay.aspx?ItemName=Versatile+Summon+Nature%27s+Ally";
	private static final String SKELETON_RULE =
		"https://aonprd.com/FeatDisplay.aspx?ItemName=Skeleton+Summoner";
	private static final List<String> MONSTER_TEMPLATES = List.of("Aerial Creature", "Aqueous Creature",
		"Chthonic Creature", "Dark Creature", "Fiery Creature", "Primordial Creature");
	private static final List<String> NATURE_TEMPLATES = List.of("Aerial Creature", "Aqueous Creature",
		"Chthonic Creature", "Fiery Creature", "Primordial Creature");
	private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};
	private static final List<Option> OPTIONS = readOptions();

	private record Option(String family, int level, String printedName, String race, String templatePolicy,
		String rulesUrl, String kit, String raceType)
	{
	}

	private PathfinderSummonOptions()
	{
	}

	/** Every published row, including choices and rows with no loaded race. */
	public static int optionCount()
	{
		return OPTIONS.size();
	}

	/**
	 * Adds rulebook groups to a handwritten catalog. The character's feats and
	 * equipped rings determine which supplemental tables appear. The character
	 * must know a summon spell of the corresponding family and at least the
	 * option's level; higher-tier spells can summon from lower-tier lists.
	 */
	public static SummonCatalog withPublishedOptions(PlayerCharacter character, Optional<SummonCatalog> authored)
	{
		var chosenMonsterTemplates = SummonSelections.versatileMonster(
			character.getDisplay().getConjurationCatalog().orElse(""));
		int[] knownLevels = knownSummonLevels(character);
		List<SummonGroup> groups = new ArrayList<>();
		authored.ifPresent(catalog -> groups.addAll(catalog.groups()));
		Map<String, List<SummonTemplate>> generated = new LinkedHashMap<>();
		for (Option option : optionsFor(character))
		{
			if (!enabled(character, option.family()) || option.level() > knownLevels[familyIndex(option.family())]
				|| "Expanded".equals(option.family()))
			{
				continue;
			}
			String title = title(option.family(), option.level());
			for (String template : templatesFor(character, option.templatePolicy()))
			{
				if ("Versatile SM".equals(option.family()) && !chosenMonsterTemplates.contains(template))
				{
					continue;
				}
				String suffix = template.isEmpty() ? "" : " · " + template;
				String name = option.printedName() + suffix + " [" + option.family() + " " + ROMAN[option.level()] + "]";
				Map<String, String> metadata = new LinkedHashMap<>();
				metadata.put("race", option.race().isBlank() ? option.printedName() : option.race());
				metadata.put("level", ROMAN[option.level()]);
				metadata.put("source", option.rulesUrl());
				metadata.put("family", option.family());
				metadata.put("publishedName", option.printedName());
				PathfinderSummonRankings.Ranking ranking = PathfinderSummonRankings.forOption(
					option.family(), option.level(), option.race());
				if (ranking != null)
				{
					metadata.put("ranking", ranking.color());
					metadata.put("rankingSource", ranking.source());
				}
				if ("Skeleton 1/day".equals(option.family()) && character.getDisplay()
					.getConjurationSession().skeletonUsed())
				{
					metadata.put("dailyUsed", "true");
				}
				switch (option.family())
				{
					case "Good" -> metadata.put("note", "Summon Good Monster grants Diehard to this creature.");
					case "Evil" -> metadata.put("note", "Summon Evil Monster: standard-action casting; the creature cannot act until your next turn, but may make attacks of opportunity.");
					case "Skeleton 1/day" -> metadata.put("note", "Skeleton Summoner: this skeletal version uses the feat's once-per-day option.");
					case "Neutral", "Counterpoised" -> {
						metadata.put("note", "Summon Neutral Monster grants a +2 resistance bonus on Will saves.");
						metadata.put("willBonus", "2");
					}
					default -> { }
				}
				if (!option.kit().isBlank())
				{
					metadata.put("kit", option.kit());
				}
				if ("Versatile SNA".equals(option.family()))
				{
					metadata.put("suppressAugment", "true");
				}
				SummonTemplate summon = new SummonTemplate(name, template.isEmpty() ? List.of() : List.of(template),
					0, List.of(), metadata);
				if (authored.isPresent() && authored.get().find(summon.name()).isPresent())
				{
					continue;
				}
				if (!CatalogResolution.unresolvedIn(summon).isEmpty())
				{
					continue;
				}
				generated.computeIfAbsent(title, ignored -> new ArrayList<>()).add(summon);
			}
		}
		generated.forEach((title, summons) -> groups.add(new SummonGroup(title, summons)));
		return new SummonCatalog(groups);
	}

	static int[] knownSummonLevels(PlayerCharacter character)
	{
		int[] highest = new int[2];
		for (PObject source : character.getSpellClassList())
		{
			for (int spellLevel = 1; spellLevel <= 9; spellLevel++)
			{
				for (CharacterSpell known : character.getCharacterSpells(source, spellLevel))
				{
					recordKnownLevel(highest, known.getSpell().getKeyName());
				}
			}
		}
		for (PCClass source : character.getClassSet())
		{
			for (CDOMList<Spell> list : character.getSpellLists(source))
			{
				for (int spellLevel = 1; spellLevel <= 9; spellLevel++)
				{
					for (Spell spell : character.getSpellsIn(list, spellLevel))
					{
						if (isSummoningSpell(spell.getKeyName()) && character.getSpellSupport(source)
							.isAutoKnownSpell(spell, spellLevel, false, character))
						{
							recordKnownLevel(highest, spell.getKeyName());
						}
					}
				}
			}
		}
		AbilityCategory specialAbilities = SettingsHandler.getGameAsProperty().get()
			.getAbilityCategory("Special Ability");
		if (specialAbilities != null)
		{
			if (character.hasAbilityKeyed(specialAbilities, "Summoner ~ Summon Monster"))
			{
				highest[0] = Math.max(highest[0], Math.min(9,
					character.getVariableValue("SummonMonsterLVL", "").intValue()));
			}
			if (character.hasAbilityKeyed(specialAbilities, "Unchained Summoner ~ Summon Monster"))
			{
				highest[0] = Math.max(highest[0], Math.min(9,
					character.getVariableValue("SummonerSummonMonsterLVL", "").intValue()));
			}
		}
		return highest;
	}

	private static boolean isSummoningSpell(String name)
	{
		return name.regionMatches(true, 0, "Summon Monster ", 0, "Summon Monster ".length())
			|| name.regionMatches(true, 0, "Summon Nature's Ally ", 0, "Summon Nature's Ally ".length());
	}

	private static void recordKnownLevel(int[] highest, String name)
	{
		for (int tier = 1; tier < ROMAN.length; tier++)
		{
			if (("Summon Monster " + ROMAN[tier]).equalsIgnoreCase(name))
			{
				highest[0] = Math.max(highest[0], tier);
			}
			else if (("Summon Nature's Ally " + ROMAN[tier]).equalsIgnoreCase(name))
			{
				highest[1] = Math.max(highest[1], tier);
			}
		}
	}

	private static int familyIndex(String family)
	{
		return "SNA".equals(family) || "Versatile SNA".equals(family) || "Spider SNA".equals(family)
			|| "Plant".equals(family) || family.startsWith("Ring of Natural Attunement") ? 1 : 0;
	}

	private static List<Option> optionsFor(PlayerCharacter character)
	{
		List<Option> options = new ArrayList<>(OPTIONS);
		if (character.hasAbilityKeyed(AbilityCategory.FEAT, "Versatile Summon Monster"))
		{
			for (Option original : OPTIONS)
			{
				if ("SM".equals(original.family()) && "auto".equals(original.templatePolicy()))
				{
					for (String template : MONSTER_TEMPLATES)
					{
						options.add(new Option("Versatile SM", original.level(), original.printedName(),
							original.race(), "explicit:" + template, VERSATILE_MONSTER_RULE,
							original.kit(), original.raceType()));
					}
				}
			}
		}
		if (character.hasAbilityKeyed(AbilityCategory.FEAT, "Summon Neutral Monster"))
		{
			for (Option original : OPTIONS)
			{
				if ("SM".equals(original.family()) && "auto".equals(original.templatePolicy()))
				{
					options.add(new Option("Counterpoised", original.level(), original.printedName(),
						original.race(), "explicit:Counterpoised Creature",
						"https://aonprd.com/FeatDisplay.aspx?ItemName=Summon+Neutral+Monster",
						original.kit(), original.raceType()));
				}
			}
		}
		if (character.hasAbilityKeyed(AbilityCategory.FEAT, "Skeleton Summoner"))
		{
			for (Option original : OPTIONS)
			{
				if ("SM".equals(original.family()) && "Animal".equals(original.raceType())
					&& !original.printedName().matches("(?i).*(?:octopus|squid|jellyfish).*"))
				{
					options.add(new Option("Skeleton 1/day", original.level(), original.printedName(),
						original.race(), "explicit:Skeleton", SKELETON_RULE,
						original.kit(), original.raceType()));
				}
			}
		}
		if (character.hasAbilityKeyed(AbilityCategory.FEAT, "Versatile Summon Nature's Ally"))
		{
			for (Option original : OPTIONS)
			{
				if ("SNA".equals(original.family()) && List.of("Animal", "Humanoid", "Vermin")
					.contains(original.raceType()))
				{
					for (String template : NATURE_TEMPLATES)
					{
						options.add(new Option("Versatile SNA", original.level(), original.printedName(),
							original.race(), "explicit:" + template, VERSATILE_NATURE_RULE,
							original.kit(), original.raceType()));
					}
				}
			}
		}
		return options;
	}

	private static boolean enabled(PlayerCharacter character, String family)
	{
		return switch (family)
		{
			case "SM", "SNA" -> true;
			case "Good" -> character.hasAbilityKeyed(AbilityCategory.FEAT, "Summon Good Monster");
			case "Evil" -> character.hasAbilityKeyed(AbilityCategory.FEAT, "Summon Evil Monster");
			case "Neutral" -> character.hasAbilityKeyed(AbilityCategory.FEAT, "Summon Neutral Monster");
			case "Counterpoised" -> character.hasAbilityKeyed(AbilityCategory.FEAT, "Summon Neutral Monster");
			case "Expanded" -> character.hasAbilityKeyed(AbilityCategory.FEAT, "Expanded Summon Monster");
			case "Versatile SM" -> character.hasAbilityKeyed(AbilityCategory.FEAT, "Versatile Summon Monster");
			case "Versatile SNA" -> character.hasAbilityKeyed(AbilityCategory.FEAT, "Versatile Summon Nature's Ally");
			case "Plant" -> character.hasAbilityKeyed(AbilityCategory.FEAT, "Summon Plant Ally");
			case "Skeleton" -> character.hasAbilityKeyed(AbilityCategory.FEAT, "Skeleton Summoner");
			case "Skeleton 1/day" -> character.hasAbilityKeyed(AbilityCategory.FEAT, "Skeleton Summoner");
			case "Spider SM", "Spider SNA" -> character.hasAbilityKeyed(AbilityCategory.FEAT, "Spider Summoner");
			default -> family.startsWith("Ring of ") && character.getEquipmentOfType("Ring", 1).stream()
				.map(Equipment::getKeyName).anyMatch(family::equalsIgnoreCase);
		};
	}

	private static String title(String family, int level)
	{
		return switch (family)
		{
			case "SM" -> "Summon Monster " + ROMAN[level];
			case "SNA" -> "Summon Nature's Ally " + ROMAN[level];
			default -> family + " · " + ROMAN[level];
		};
	}

	private static List<String> templatesFor(PlayerCharacter character, String policy)
	{
		if (policy.startsWith("explicit:"))
		{
			return List.of(policy.substring("explicit:".length()));
		}
		return switch (policy)
		{
			case "celestial" -> List.of("Celestial Creature");
			case "fiendish" -> List.of("Fiendish Creature");
			case "auto" -> alignmentTemplates(character);
			default -> List.of("");
		};
	}

	private static List<String> alignmentTemplates(PlayerCharacter character)
	{
		List<String> templates = new ArrayList<>();
		if (character.getDisplay().getPCAlignment() == null)
		{
			return List.of("Celestial Creature", "Fiendish Creature", "Entropic Creature", "Resolute Creature");
		}
		String alignment = character.getDisplay().getPCAlignment().getKeyName().toUpperCase(Locale.ROOT);
		if (!alignment.contains("E"))
		{
			templates.add("Celestial Creature");
		}
		if (!alignment.contains("G"))
		{
			templates.add("Fiendish Creature");
		}
		if (!alignment.contains("L"))
		{
			templates.add("Entropic Creature");
		}
		if (!alignment.contains("C"))
		{
			templates.add("Resolute Creature");
		}
		return templates;
	}

	private static List<Option> readOptions()
	{
		InputStream resource = PathfinderSummonOptions.class.getResourceAsStream(RESOURCE);
		if (resource == null)
		{
			throw new IllegalStateException("Missing Pathfinder summoning tables: " + RESOURCE);
		}
		List<Option> options = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.isBlank() || line.startsWith("#"))
				{
					continue;
				}
				String[] fields = line.split("\\t", -1);
				if (fields.length != 8)
				{
					throw new IllegalStateException("Invalid summoning row: " + line);
				}
				options.add(new Option(fields[0], Integer.parseInt(fields[1]), fields[2], fields[3], fields[4],
					fields[5], fields[6], fields[7]));
			}
		}
		catch (IOException error)
		{
			throw new IllegalStateException("Could not read Pathfinder summoning tables", error);
		}
		return List.copyOf(options);
	}
}
