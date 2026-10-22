/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package pcgen.core.conjurations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Compares a creature with its templates against the same creature without
 * them, statistic by statistic, in the order a creature's card shows them.
 */
public final class TemplateDifferences
{
	private static final String ABILITY_SEPARATOR = ", ";
	private static final Map<String, Function<CreatureStats, String>> STATS = statistics();

	private TemplateDifferences()
	{
	}

	private static Map<String, Function<CreatureStats, String>> statistics()
	{
		Map<String, Function<CreatureStats, String>> statistics = new LinkedHashMap<>();
		statistics.put("speed", CreatureStats::speed);
		statistics.put("hp", stats -> String.valueOf(stats.maximumHitPoints()));
		statistics.put("ac", stats -> stats.armorClass() + " / " + stats.touchArmorClass() + " / "
			+ stats.flatFootedArmorClass());
		statistics.put("attack", stats -> stats.attacks().stream()
			.map(attack -> attack.name() + " " + attack.toHit() + " · " + attack.damage())
			.collect(Collectors.joining("; ")));
		statistics.put("cmb", CreatureStats::combatManeuverBonus);
		statistics.put("cmd", stats -> String.valueOf(stats.combatManeuverDefense()));
		statistics.put("ref", CreatureStats::reflex);
		statistics.put("fort", CreatureStats::fortitude);
		statistics.put("will", CreatureStats::will);
		statistics.put("init", CreatureStats::initiative);
		return statistics;
	}

	/**
	 * @param withoutTemplates the creature built from its race alone
	 * @param withTemplates    the same creature with its templates
	 * @return each statistic that differs, then the special abilities the templates add.
	 */
	public static List<TemplateDifference> between(CreatureStats withoutTemplates, CreatureStats withTemplates)
	{
		List<TemplateDifference> differences = new ArrayList<>();
		STATS.forEach((stat, read) -> {
			String before = read.apply(withoutTemplates);
			String after = read.apply(withTemplates);
			if (!before.equals(after))
			{
				differences.add(new TemplateDifference(stat, before, after));
			}
		});
		List<String> before = abilitiesOf(withoutTemplates);
		String added = abilitiesOf(withTemplates).stream().filter(ability -> !before.contains(ability))
			.collect(Collectors.joining(ABILITY_SEPARATOR));
		if (!added.isEmpty())
		{
			differences.add(new TemplateDifference("abilities", "", added));
		}
		return differences;
	}

	private static List<String> abilitiesOf(CreatureStats stats)
	{
		return stats.specialAbilities().isBlank() ? List.of()
			: Arrays.stream(stats.specialAbilities().split(ABILITY_SEPARATOR)).map(String::strip).toList();
	}
}
