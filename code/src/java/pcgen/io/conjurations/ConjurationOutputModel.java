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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import pcgen.core.PlayerCharacter;
import pcgen.core.conjurations.ConjurationSession;
import pcgen.core.conjurations.ConjuredCreature;
import pcgen.core.conjurations.SummonCatalog;
import pcgen.core.conjurations.SummonCatalogParser;
import pcgen.core.conjurations.SummonField;
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

	/** The columns of a catalog table, after the creature's name. */
	private static final List<SummonField> COLUMNS = List.of(SummonField.SPEED, SummonField.HIT_POINTS,
		SummonField.ARMOR_CLASS, SummonField.ATTACK, SummonField.DAMAGE, SummonField.CMB, SummonField.CMD,
		SummonField.REFLEX, SummonField.FORTITUDE, SummonField.WILL, SummonField.INITIATIVE);

	/** The fields too long for a column, shown on a line under the creature. */
	private static final List<SummonField> DETAILS = List.of(SummonField.SPECIAL, SummonField.SPELLS,
		SummonField.NOTE);

	/** What a card in play repeats of its stat line: all of it but the hit points it tracks. */
	private static final List<SummonField> CARD_FIELDS = Arrays.stream(SummonField.values())
		.filter(field -> field != SummonField.HIT_POINTS).toList();

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
		model.put("columns", labelled(COLUMNS));
		model.put("details", labelled(DETAILS));
		model.put("cardFields", labelled(CARD_FIELDS));
		model.put("labels", labels());

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
		model.put("roles", catalog.map(SummonCatalog::roles).orElse(List.of()));
		model.put("conjured", conjuredOf(fight, catalog, errors.isEmpty()));
		return model;
	}

	private static List<Map<String, Object>> labelled(List<SummonField> fields)
	{
		return fields.stream()
			.map(field -> Map.<String, Object>of(KEY, field.key(), LABEL, LanguageBundle.getString(labelKeyOf(field))))
			.toList();
	}

	private static String labelKeyOf(SummonField field)
	{
		return switch (field)
		{
			case SPEED -> "in_conjurations_speed";
			case HIT_POINTS -> "in_conjurations_hp";
			case ARMOR_CLASS -> "in_conjurations_ac";
			case ATTACK -> "in_conjurations_attack";
			case DAMAGE -> "in_conjurations_damage";
			case SPECIAL -> "in_conjurations_special";
			case CMB -> "in_conjurations_cmb";
			case CMD -> "in_conjurations_cmd";
			case REFLEX -> "in_conjurations_ref";
			case FORTITUDE -> "in_conjurations_fort";
			case WILL -> "in_conjurations_will";
			case INITIATIVE -> "in_conjurations_init";
			case SPELLS -> "in_conjurations_spells";
			case NOTE -> "in_conjurations_note";
		};
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
			Map.entry("everyRole", LanguageBundle.getString("in_conjurations_every_role")),
			Map.entry("creature", LanguageBundle.getString("in_conjurations_creature")),
			Map.entry("damage", LanguageBundle.getString("in_conjurations_damage")));
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
		Map<String, Object> fields = new LinkedHashMap<>();
		for (SummonField field : SummonField.values())
		{
			fields.put(field.key(), summon.field(field));
		}
		Map<String, Object> described = new LinkedHashMap<>();
		described.put("name", summon.name());
		described.put("roles", summon.roles());
		described.put("fields", fields);
		described.put("hasMax", summon.maximumHitPoints().isPresent());
		described.put("hpMax", summon.maximumHitPoints().orElse(0));
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
			described.put("summon", summon.map(ConjurationOutputModel::summonOf).orElse(Map.of()));
			described.put("missing", (summon.isEmpty() && catalogReads)
				? LanguageBundle.getFormattedString("in_conjurations_missing", creature.creature()) : "");
			conjured.add(described);
		}
		return conjured;
	}
}
