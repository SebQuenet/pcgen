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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import pcgen.core.AbilityCategory;
import pcgen.core.PlayerCharacter;
import pcgen.core.Globals;
import pcgen.core.PCTemplate;
import pcgen.core.Race;
import pcgen.core.bonus.TemporaryBonusSources;
import pcgen.core.conjurations.AppliedBonus;
import pcgen.core.conjurations.CatalogResolution;
import pcgen.core.conjurations.ConjurationSession;
import pcgen.core.conjurations.ConjuredCreature;
import pcgen.core.conjurations.PathfinderSummonOptions;
import pcgen.core.conjurations.PathfinderSummonRankings;
import pcgen.core.conjurations.SummonCatalog;
import pcgen.core.conjurations.SummonCatalogParser;
import pcgen.core.conjurations.SummonForms;
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
		model.put("duration", fight.duration());
		model.put("skeletonFeat", character.hasAbilityKeyed(AbilityCategory.FEAT, "Skeleton Summoner"));
		model.put("skeletonUsed", fight.skeletonUsed());
		model.put("labels", labels());
		model.put("statLabels", statLabels());
		model.put("fieldLabels", fieldLabels());
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
		if (errors.isEmpty())
		{
			catalog = Optional.of(PathfinderSummonOptions.withPublishedOptions(character, catalog));
		}
		model.put("present", catalog.filter(found -> !found.groups().isEmpty()).isPresent() || !errors.isEmpty());
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
			Map.entry("level", LanguageBundle.getString("in_conjurations_level")),
			Map.entry("role", LanguageBundle.getString("in_conjurations_role")),
			Map.entry("ranking", LanguageBundle.getString("in_conjurations_ranking")),
			Map.entry("unranked", LanguageBundle.getString("in_conjurations_unranked")),
			Map.entry("rating_red", LanguageBundle.getString("in_conjurations_rating_red")),
			Map.entry("rating_orange", LanguageBundle.getString("in_conjurations_rating_orange")),
			Map.entry("rating_green", LanguageBundle.getString("in_conjurations_rating_green")),
			Map.entry("rating_blue", LanguageBundle.getString("in_conjurations_rating_blue")),
			Map.entry("rating_red_meaning", LanguageBundle.getString("in_conjurations_rating_red_meaning")),
			Map.entry("rating_orange_meaning", LanguageBundle.getString("in_conjurations_rating_orange_meaning")),
			Map.entry("rating_green_meaning", LanguageBundle.getString("in_conjurations_rating_green_meaning")),
			Map.entry("rating_blue_meaning", LanguageBundle.getString("in_conjurations_rating_blue_meaning")),
			Map.entry("hitDice", LanguageBundle.getString("in_conjurations_hit_dice")),
			Map.entry("expectedDamage", LanguageBundle.getString("in_conjurations_expected_damage")),
			Map.entry("targetArmor", LanguageBundle.getString("in_conjurations_target_armor")),
			Map.entry("touchArmor", LanguageBundle.getString("in_conjurations_touch_armor")),
			Map.entry("flatArmor", LanguageBundle.getString("in_conjurations_flat_armor")),
			Map.entry("details", LanguageBundle.getString("in_conjurations_details")),
			Map.entry("publishedGroup", LanguageBundle.getString("in_conjurations_published_group")),
			Map.entry("form", LanguageBundle.getString("in_conjurations_form")),
			Map.entry("normalForm", LanguageBundle.getString("in_conjurations_normal_form")),
			Map.entry("skeletonReset", LanguageBundle.getString("in_conjurations_skeleton_reset")),
			Map.entry("skeletonUsed", LanguageBundle.getString("in_conjurations_skeleton_used")),
			Map.entry("selectChoice", LanguageBundle.getString("in_conjurations_select_choice")),
			Map.entry("choiceRequired", LanguageBundle.getString("in_conjurations_choice_required")),
			Map.entry("sourceUnavailable", LanguageBundle.getString("in_conjurations_source_unavailable")),
			Map.entry("publishedRules", LanguageBundle.getString("in_conjurations_published_rules")),
			Map.entry("closeDetails", LanguageBundle.getString("in_conjurations_close_details")),
			Map.entry("interest", LanguageBundle.getString("in_conjurations_interest")),
			Map.entry("usage", LanguageBundle.getString("in_conjurations_usage")),
			Map.entry("limits", LanguageBundle.getString("in_conjurations_limits")),
			Map.entry("abilities", LanguageBundle.getString("in_conjurations_abilities")),
			Map.entry("spells", LanguageBundle.getString("in_conjurations_spells")),
			Map.entry("authoredNote", LanguageBundle.getString("in_conjurations_authored_note")),
			Map.entry("rulesNote", LanguageBundle.getString("in_conjurations_rules_note")),
			Map.entry("guideSource", LanguageBundle.getString("in_conjurations_guide_source")),
			Map.entry("rulesSource", LanguageBundle.getString("in_conjurations_rules_source")),
			Map.entry("bestiarySource", LanguageBundle.getString("in_conjurations_bestiary_source")),
			Map.entry("sourceRace", LanguageBundle.getString("in_conjurations_source_race")),
			Map.entry("templates", LanguageBundle.getString("in_conjurations_templates")),
			Map.entry("fullStats", LanguageBundle.getString("in_conjurations_full_stats")),
			Map.entry("computedInApp", LanguageBundle.getString("in_conjurations_computed_in_app")),
			Map.entry("damage", LanguageBundle.getString("in_conjurations_damage")),
			Map.entry("group", LanguageBundle.getString("in_conjurations_group")),
			Map.entry("ownBonuses", LanguageBundle.getString("in_conjurations_own_bonuses")),
			Map.entry("addBonus", LanguageBundle.getString("in_conjurations_add_bonus")),
			Map.entry("applyBonus", LanguageBundle.getString("in_conjurations_apply_bonus")),
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

	private static Map<String, Object> fieldLabels()
	{
		Map<String, Object> fields = new LinkedHashMap<>(statLabels());
		fields.put("damage", LanguageBundle.getString("in_conjurations_damage"));
		fields.put("special", LanguageBundle.getString("in_conjurations_abilities"));
		fields.put("spells", LanguageBundle.getString("in_conjurations_spells"));
		fields.put("note", LanguageBundle.getString("in_conjurations_authored_note"));
		return fields;
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
				group.summons().stream().map(summon -> summonOf(summon, group)).toList()));
		}
		return groups;
	}

	private static List<Map<String, Object>> summonsOf(SummonCatalog catalog)
	{
		return catalog.groups().stream().flatMap(group -> group.summons().stream()
			.map(summon -> summonOf(summon, group))).toList();
	}

	private static Map<String, Object> summonOf(SummonTemplate summon, SummonGroup group)
	{
		Map<String, Object> described = new LinkedHashMap<>();
		described.put("name", summon.name());
		described.put("displayName", displayName(summon));
		described.put("race", summon.sourceRaceKey());
		Race sourceRace = Globals.getContext().getReferenceContext()
			.silentlyGetConstructedCDOMObject(Race.class, summon.sourceRaceKey());
		described.put("displayRace", sourceRace == null || !"fr".equals(Locale.getDefault().getLanguage())
			? summon.sourceRaceKey() : sourceRace.getLocalizedName());
		described.put("templates", summon.templateKeys());
		described.put("displayTemplates", summon.templateKeys().stream().map(key -> {
			PCTemplate template = Globals.getContext().getReferenceContext()
				.silentlyGetConstructedCDOMObject(PCTemplate.class, key);
			return template == null || !"fr".equals(Locale.getDefault().getLanguage())
				? key : template.getLocalizedName();
		}).toList());
		described.put("roles", summon.roles());
		described.put("metadata", summon.metadata());
		described.put("note", summon.metadata().containsKey("publishedName")
			? publishedNote(summon.metadata()) : summon.metadata().getOrDefault("note", ""));
		described.put("family", summon.metadata().getOrDefault("family", ""));
		described.put("forms", SummonForms.forSummon(summon).stream().map(form -> Map.of("key", form.key(),
			"label", form.label(), "description", form.description(), "source", form.source())).toList());
		described.put("collectiveForm", SummonForms.collectiveForm(summon).map(form -> Map.of("label", form.label(),
			"description", form.description(), "source", form.source())).orElse(Map.of()));
		PathfinderSummonGuides.Guide guide = PathfinderSummonGuides.forRace(summon.sourceRaceKey());
		described.put("guide", guide == null ? Map.of() : Map.of("interest", guide.interest(), "usage",
			guide.usage(), "limits", guide.limits(), "bestiary", guide.bestiary()));
		described.put("authored", summon.hasAuthoredStats());
		boolean choiceRequired = "true".equals(summon.metadata().get("choiceRequired"));
		described.put("choiceRequired", choiceRequired);
		described.put("choiceHint", "Versatile SM".equals(summon.metadata().get("family"))
			? LanguageBundle.getString("in_conjurations_versatile_choice_required")
			: LanguageBundle.getString("in_conjurations_choice_required"));
		boolean dailyUsed = "true".equals(summon.metadata().get("dailyUsed"));
		described.put("available", !choiceRequired && !dailyUsed && CatalogResolution.unresolvedIn(summon).isEmpty());
		described.put("unavailableHint", dailyUsed ? LanguageBundle.getString("in_conjurations_skeleton_used")
			: choiceRequired ? described.get("choiceHint") : LanguageBundle.getString("in_conjurations_source_unavailable"));
		described.put("hpMax", summon.authoredHitPoints().orElse(0));
		String level = summon.metadata().getOrDefault("level", levelIn(group.title()));
		described.put("level", level);
		described.put("levelRank", romanRank(level));
		PathfinderSummonRankings.Ranking ranking = PathfinderSummonRankings.forOption(
			summon.metadata().getOrDefault("family", ""), romanRank(level), summon.sourceRaceKey());
		described.put("ranking", ranking == null ? Map.of() : Map.of("color", ranking.color(),
			"score", ranking.score(), "source", ranking.source()));
		described.put("group", group.title());
		described.put("displayGroup", summon.metadata().containsKey("publishedName")
			? publishedGroup(summon.metadata()) : group.title());
		return described;
	}

	private static String displayName(SummonTemplate summon)
	{
		if (!summon.metadata().containsKey("publishedName"))
		{
			return summon.name();
		}
		Race race = Globals.getContext().getReferenceContext()
			.silentlyGetConstructedCDOMObject(Race.class, summon.sourceRaceKey());
		String name = race == null || !"fr".equals(Locale.getDefault().getLanguage())
			? summon.metadata().get("publishedName") : race.getLocalizedName();
		if (name == null || name.isBlank())
		{
			name = summon.metadata().get("publishedName");
		}
		StringBuilder display = new StringBuilder(name);
		for (String key : summon.templateKeys())
		{
			PCTemplate template = Globals.getContext().getReferenceContext()
				.silentlyGetConstructedCDOMObject(PCTemplate.class, key);
			display.append(" · ").append(template == null ? key : template.getLocalizedName());
		}
		return display.append(" [").append(publishedGroup(summon.metadata())).append(']').toString();
	}

	private static String publishedGroup(Map<String, String> metadata)
	{
		String family = metadata.getOrDefault("family", "");
		return LanguageBundle.getTerm("in_conjurations_family_", family) + " "
			+ metadata.getOrDefault("level", "");
	}

	private static String publishedNote(Map<String, String> metadata)
	{
		String key = switch (metadata.getOrDefault("family", ""))
		{
			case "Good" -> "in_conjurations_note_good";
			case "Evil" -> "in_conjurations_note_evil";
			case "Skeleton 1/day" -> "in_conjurations_note_skeleton";
			case "Neutral", "Counterpoised" -> "in_conjurations_note_neutral";
			default -> "";
		};
		return key.isEmpty() ? "" : LanguageBundle.getString(key);
	}

	private static String levelIn(String title)
	{
		String[] parts = title.strip().split("\\s+");
		String candidate = parts[parts.length - 1];
		return romanRank(candidate) > 0 ? candidate : "";
	}

	private static int romanRank(String level)
	{
		return switch (level.toUpperCase(java.util.Locale.ROOT))
		{
			case "I" -> 1;
			case "II" -> 2;
			case "III" -> 3;
			case "IV" -> 4;
			case "V" -> 5;
			case "VI" -> 6;
			case "VII" -> 7;
			case "VIII" -> 8;
			case "IX" -> 9;
			default -> 0;
		};
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
			described.put("form", creature.form());
			described.put("counted", creature.roundsLeft().isPresent());
			described.put("rounds", creature.roundsLeft().orElse(0));
			described.put("found", summon.isPresent());
			described.put("bonuses", bonusesOf(creature.bonuses()));
			described.put("summon", summon.map(found -> catalog.get().groups().stream()
				.filter(group -> group.summons().contains(found)).findFirst()
				.map(group -> summonOf(found, group)).orElse(Map.of())).orElse(Map.of()));
			described.put("missing", (summon.isEmpty() && catalogReads)
				? LanguageBundle.getFormattedString("in_conjurations_missing", creature.creature()) : "");
			conjured.add(described);
		}
		return conjured;
	}
}
