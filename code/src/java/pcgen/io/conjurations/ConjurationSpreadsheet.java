/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package pcgen.io.conjurations;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;
import java.util.stream.Collectors;

import pcgen.core.conjurations.CreatureAttack;
import pcgen.core.conjurations.CreatureStats;
import pcgen.core.conjurations.PathfinderSummonRankings;
import pcgen.core.conjurations.SummonCatalog;
import pcgen.core.conjurations.SummonForms;
import pcgen.core.conjurations.SummonGroup;
import pcgen.core.conjurations.SummonTemplate;
import pcgen.system.LanguageBundle;

/**
 * The catalog of conjurations as the rows of a workbook: one creature per row,
 * sorted by level, with the most creatures one spell brings after its name.
 */
public final class ConjurationSpreadsheet
{
	static final List<String> COLUMNS = List.of("level", "roles", "creature", "maximum_count", "ranking", "speed",
		"hit_points", "armor_class", "attacks", "damage", "special", "combat_maneuver_bonus",
		"combat_maneuver_defense", "reflex", "fortitude", "will", "initiative", "spells");
	private static final String ARMOR_SEPARATOR = " / ";
	private static final String VARIANT_MARK = "\u21B3 ";
	private static final String FORM_SEPARATOR = " \u2014 ";

	private ConjurationSpreadsheet()
	{
	}

	private record Entry(SummonTemplate summon, String level, int rank, String name)
	{
	}

	/**
	 * The rows of the workbook, and what their look depends on.
	 *
	 * @param rows    the header row, then one row per creature
	 * @param folded  the indexes in {@code rows} of the variants folded under their base
	 * @param levels  each row's summoning level, 0 for the header
	 * @param ratings each row's RPGBOT rating color, or an empty string
	 */
	record Layout(List<List<Object>> rows, Set<Integer> folded, List<Integer> levels, List<String> ratings)
	{
	}

	/**
	 * @param catalog       the conjurer's catalog, its published options included
	 * @param maximumCount  the most creatures of a kind one spell brings, given its level
	 * @param statsOf       the numbers PCGen computes for a creature, if it can build it
	 * @return the workbook's bytes.
	 */
	public static byte[] workbookOf(SummonCatalog catalog, ToIntBiFunction<SummonTemplate, Integer> maximumCount,
		Function<SummonTemplate, Optional<CreatureStats>> statsOf)
	{
		return XlsxWorkbook.of(ConjurationSheetStyle.sheetOf(LanguageBundle.getString("in_conjurations_sheet_name"),
			layoutOf(catalog, maximumCount, statsOf)));
	}

	/**
	 * One row per creature, by level then by name; the variants of a creature
	 * follow its base in catalog order, folded under it.
	 */
	static Layout layoutOf(SummonCatalog catalog, ToIntBiFunction<SummonTemplate, Integer> maximumCount,
		Function<SummonTemplate, Optional<CreatureStats>> statsOf)
	{
		List<List<Object>> rows = new ArrayList<>();
		rows.add(COLUMNS.stream().<Object>map(column -> LanguageBundle.getString("in_conjurations_sheet_" + column))
			.toList());
		List<Entry> entries = new ArrayList<>();
		for (SummonGroup group : catalog.groups())
		{
			for (SummonTemplate summon : group.summons())
			{
				String level = ConjurationOutputModel.levelOf(summon, group);
				entries.add(new Entry(summon, level, ConjurationOutputModel.romanRank(level),
					ConjurationOutputModel.displayName(summon)));
			}
		}
		Map<SummonTemplate, Integer> ranks = new HashMap<>();
		entries.forEach(entry -> ranks.put(entry.summon(), entry.rank()));
		Map<String, SummonTemplate> bases = VariantGroups.basesOf(entries.stream().map(Entry::summon).toList(),
			ranks::get);
		Collator alphabetical = Collator.getInstance(Locale.getDefault());
		List<Entry> baseEntries = entries.stream().filter(entry -> bases.containsValue(entry.summon()))
			.sorted(Comparator.comparingInt(Entry::rank).thenComparing(Entry::name, alphabetical)).toList();
		Set<Integer> folded = new HashSet<>();
		List<Integer> levels = new ArrayList<>(List.of(0));
		List<String> ratings = new ArrayList<>(List.of(""));
		for (Entry base : baseEntries)
		{
			String variantGroup = VariantGroups.keyOf(base.summon(), base.rank());
			List<Entry> line = new ArrayList<>(List.of(base));
			entries.stream().filter(variant -> variant != base
				&& VariantGroups.keyOf(variant.summon(), variant.rank()).equals(variantGroup)).forEach(line::add);
			for (Entry entry : line)
			{
				boolean variant = entry != base;
				String rating = ratingOf(entry);
				int count = maximumCount.applyAsInt(entry.summon(), entry.rank());
				Optional<CreatureStats> stats = entry.summon().hasAuthoredStats() ? Optional.empty()
					: statsOf.apply(entry.summon());
				if (variant)
				{
					folded.add(rows.size());
				}
				levels.add(entry.rank());
				ratings.add(rating);
				rows.add(rowOf(entry, variant ? VARIANT_MARK + entry.name() : entry.name(), rating, count, stats));
				// Each alternate form, such as the hound archon's beast shapes, folded under its creature
				for (SummonForms.Form form : stats.isPresent() ? SummonForms.forSummon(entry.summon())
					: List.<SummonForms.Form>of())
				{
					folded.add(rows.size());
					levels.add(entry.rank());
					ratings.add(rating);
					rows.add(rowOf(entry, VARIANT_MARK + entry.name() + FORM_SEPARATOR + form.label(), rating, count,
						stats.map(computed -> SummonForms.statsOf(entry.summon(), form.key(), computed))));
				}
			}
		}
		return new Layout(rows, Set.copyOf(folded), levels, ratings);
	}

	private static String ratingOf(Entry entry)
	{
		PathfinderSummonRankings.Ranking ranking = PathfinderSummonRankings.forOption(
			entry.summon().metadata().getOrDefault("family", ""), entry.rank(), entry.summon().sourceRaceKey());
		return ranking == null ? "" : ranking.color();
	}

	private static List<Object> rowOf(Entry entry, String name, String rating, int maximumCount,
		Optional<CreatureStats> computed)
	{
		SummonTemplate summon = entry.summon();
		List<Object> row = new ArrayList<>();
		row.add(entry.level());
		row.add(String.join(", ", ConjurationOutputModel.rolesOf(summon)));
		row.add(name);
		row.add(maximumCount);
		row.add(rating.isEmpty() ? "" : LanguageBundle.getString("in_conjurations_rating_" + rating + "_meaning"));
		if (computed.isPresent())
		{
			CreatureStats stats = computed.get();
			row.add(stats.speed());
			row.add(stats.maximumHitPoints());
			row.add(stats.armorClass() + ARMOR_SEPARATOR + stats.touchArmorClass() + ARMOR_SEPARATOR
				+ stats.flatFootedArmorClass());
			row.add(stats.attacks().stream().map(attack -> attack.name() + " " + attack.toHit())
				.collect(Collectors.joining("\n")));
			row.add(stats.attacks().stream().map(CreatureAttack::damage).collect(Collectors.joining("\n")));
			row.add(specialOf(summon, stats.specialAbilities()));
			row.add(stats.combatManeuverBonus());
			row.add(stats.combatManeuverDefense());
			row.add(stats.reflex());
			row.add(stats.fortitude());
			row.add(stats.will());
			row.add(stats.initiative());
		}
		else
		{
			for (String field : List.of("speed", "hp", "ac", "attack", "damage"))
			{
				row.add(summon.metadata().getOrDefault(field, ""));
			}
			row.add(specialOf(summon, ""));
			for (String field : List.of("cmb", "cmd", "ref", "fort", "will", "init"))
			{
				row.add(summon.metadata().getOrDefault(field, ""));
			}
		}
		row.add(summon.metadata().getOrDefault("spells", ""));
		return row;
	}

	/** The writer's own line wins over PCGen's list; a mythic variant first says what it costs. */
	private static String specialOf(SummonTemplate summon, String computed)
	{
		String special = summon.metadata().getOrDefault("special", computed);
		if (!summon.metadata().containsKey("mythicTemplate"))
		{
			return special;
		}
		String cost = LanguageBundle.getString("in_conjurations_sheet_mythic_cost");
		return special.isBlank() ? cost : cost + "\n" + special;
	}
}
