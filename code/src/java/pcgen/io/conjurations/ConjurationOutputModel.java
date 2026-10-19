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
package pcgen.io.conjurations;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import pcgen.core.PlayerCharacter;
import pcgen.core.bonus.TemporaryBonusSources;
import pcgen.core.conjurations.AppliedBonus;
import pcgen.core.conjurations.ConjurationSession;
import pcgen.core.conjurations.ConjuredCreature;
import pcgen.core.conjurations.SummonCatalog;
import pcgen.core.conjurations.SummonCatalogParser;
import pcgen.core.conjurations.SummonGroup;
import pcgen.core.conjurations.SummonParseError;
import pcgen.core.conjurations.SummonParseFailure;
import pcgen.core.conjurations.SummonParseSuccess;
import pcgen.core.conjurations.SummonTemplate;
import pcgen.system.LanguageBundle;

/**
 * The conjurations page as an output sheet template sees it: the catalog read
 * off the character, and the creatures it has conjured into the fight, each
 * matched with its stat line.
 *
 * <p>
 * Nested maps and lists rather than the records themselves, because
 * FreeMarker's default wrapper reads bean properties and a record has none.
 * The values are the text as written; escaping belongs to the template, which
 * declares {@code output_format="HTML"}.
 */
public final class ConjurationOutputModel
{
	private static final String KEY = "key";
	private static final String LABEL = "label";
	private static final List<String> METADATA_FIELDS = List.of("speed", "hp", "ac", "attack", "damage",
		"special", "cmb", "cmd", "ref", "fort", "will", "init", "spells", "note");

	private ConjurationOutputModel()
	{
	}

	/**
	 * Build the model for a character.
	 *
	 * @param character the character whose conjurations are being rendered
	 * @return the data model, with {@code present} false when the character has
	 *         no catalog at all
	 */
	public static Map<String, Object> of(PlayerCharacter character)
	{
		Optional<String> source = character.getDisplay().getConjurationCatalog();
		ConjurationSession fight = character.getDisplay().getConjurationSession();
		Map<String, Object> model = new LinkedHashMap<>();
		model.put("present", source.isPresent());
		model.put("duration", fight.duration());
		model.put("labels", labels());
		model.put("statLabels", statLabels());
		model.put("metadataFields", METADATA_FIELDS);
		model.put("groupBonuses", bonusesOf(fight.groupBonuses()));

		Optional<SummonCatalog> catalog = Optional.empty();
		List<Map<String, Object>> errors = List.of();
		if (source.isPresent())
		{
			switch (SummonCatalogParser.parse(source.get()))
			{
				case SummonParseSuccess success -> catalog = Optional.of(success.catalog());
				case SummonParseFailure failure -> errors = errorsOf(failure);
			}
		}
		model.put("errors", errors);
		model.put("groups", catalog.map(ConjurationOutputModel::groupsOf).orElse(List.of()));
		model.put("summons", catalog.map(ConjurationOutputModel::summonsOf).orElse(List.of()));
		model.put("conjured", conjuredOf(fight, catalog, errors.isEmpty()));
		return model;
	}

	/**
	 * The page's own wording, in the user's language. The catalog's text is the
	 * writer's; these are the headings and buttons around it.
	 */
	private static Map<String, Object> labels()
	{
		return Map.ofEntries(Map.entry("conjure", LanguageBundle.getString("in_conjurations_conjure")),
			Map.entry("inPlay", LanguageBundle.getString("in_conjurations_in_play")),
			Map.entry("nextRound", LanguageBundle.getString("in_conjurations_next_round")),
			Map.entry("dismiss", LanguageBundle.getString("in_conjurations_dismiss")),
			Map.entry("dismissAll", LanguageBundle.getString("in_conjurations_dismiss_all")),
			Map.entry("duration", LanguageBundle.getString("in_conjurations_duration")),
			Map.entry("rounds", LanguageBundle.getString("in_conjurations_rounds")),
			Map.entry("gone", LanguageBundle.getString("in_conjurations_gone")),
			Map.entry("harm", LanguageBundle.getString("in_conjurations_harm")),
			Map.entry("heal", LanguageBundle.getString("in_conjurations_heal")),
			Map.entry("nobody", LanguageBundle.getString("in_conjurations_nobody")),
			Map.entry("empty", LanguageBundle.getString("in_conjurations_empty")),
			Map.entry("doesNotRead", LanguageBundle.getString("in_conjurations_does_not_read")),
			Map.entry("creature", LanguageBundle.getString("in_conjurations_creature")),
			Map.entry("damage", LanguageBundle.getString("in_conjurations_damage")),
			Map.entry("group", LanguageBundle.getString("in_conjurations_group")),
			Map.entry("ownBonuses", LanguageBundle.getString("in_conjurations_own_bonuses")),
			Map.entry("addBonus", LanguageBundle.getString("in_conjurations_add_bonus")),
			Map.entry("value", LanguageBundle.getString("in_conjurations_value")),
			Map.entry("building", LanguageBundle.getString("in_conjurations_building")),
			Map.entry("authoredStats", LanguageBundle.getString("in_conjurations_authored_stats")));
	}

	/**
	 * The names of the numbers a card shows, which the page's script fills in
	 * once PCGen has built the creature.
	 */
	private static Map<String, Object> statLabels()
	{
		Map<String, Object> labels = new LinkedHashMap<>();
		for (String stat : List.of("speed", "hp", "ac", "attack", "cmb", "cmd", "ref", "fort", "will", "init"))
		{
			labels.put(stat, LanguageBundle.getString("in_conjurations_" + stat));
		}
		return labels;
	}

	private static List<Map<String, Object>> bonusesOf(List<AppliedBonus> bonuses)
	{
		return bonuses.stream().map(bonus -> Map.<String, Object>of("key", bonus.source().text(), "label",
			TemporaryBonusSources.labelOf(bonus.source()))).toList();
	}

	private static List<Map<String, Object>> errorsOf(SummonParseFailure failure)
	{
		List<Map<String, Object>> errors = new ArrayList<>();
		for (SummonParseError error : failure.errors())
		{
			errors.add(Map.of("line", error.line(), "message", error.message(), "text",
				LanguageBundle.getFormattedString("in_conjurations_error", error.line(), error.message())));
		}
		return errors;
	}

	private static List<Map<String, Object>> groupsOf(SummonCatalog catalog)
	{
		List<Map<String, Object>> groups = new ArrayList<>();
		for (SummonGroup group : catalog.groups())
		{
			groups.add(Map.of("title", group.title(), "summons",
				group.summons().stream().map(ConjurationOutputModel::summonOf).toList()));
		}
		return groups;
	}

	private static List<Map<String, Object>> summonsOf(SummonCatalog catalog)
	{
		return catalog.groups().stream().flatMap(group -> group.summons().stream())
			.map(ConjurationOutputModel::summonOf).toList();
	}

	private static Map<String, Object> summonOf(SummonTemplate summon)
	{
		Map<String, Object> described = new LinkedHashMap<>();
		described.put("name", summon.name());
		described.put("race", summon.raceKey());
		described.put("templates", summon.templateKeys());
		described.put("roles", summon.roles());
		described.put("metadata", summon.metadata());
		described.put("authored", summon.hasAuthoredStats());
		described.put("hpMax", summon.authoredHitPoints().orElse(0));
		return described;
	}

	/**
	 * @param catalogReads false when the catalog does not read, so that a
	 *                     creature in play is not reported missing from a
	 *                     catalog that merely has a typo in it
	 */
	private static List<Map<String, Object>> conjuredOf(ConjurationSession fight, Optional<SummonCatalog> catalog,
		boolean catalogReads)
	{
		List<Map<String, Object>> conjured = new ArrayList<>();
		for (ConjuredCreature creature : fight.conjured())
		{
			Optional<SummonTemplate> summon = catalog.flatMap(read -> read.find(creature.creature()));
			Map<String, Object> described = new LinkedHashMap<>();
			described.put("name", creature.creature());
			described.put("number", creature.number());
			described.put("damage", creature.damage());
			described.put("counted", creature.roundsLeft().isPresent());
			described.put("rounds", creature.roundsLeft().orElse(0));
			described.put("found", summon.isPresent());
			described.put("bonuses", bonusesOf(creature.bonuses()));
			described.put("summon", summon.map(ConjurationOutputModel::summonOf).orElse(Map.of()));
			described.put("missing", (summon.isEmpty() && catalogReads)
				? LanguageBundle.getFormattedString("in_conjurations_missing", creature.creature()) : "");
			conjured.add(described);
		}
		return conjured;
	}
}
