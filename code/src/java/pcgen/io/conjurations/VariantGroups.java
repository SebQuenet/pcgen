/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package pcgen.io.conjurations;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

import pcgen.core.conjurations.SummonTemplate;

/**
 * Creatures of the same race and level are variants of one another; the one with
 * the fewest templates, first in catalog order on a tie, stands for the group.
 */
final class VariantGroups
{
	private VariantGroups()
	{
	}

	static String keyOf(SummonTemplate summon, int levelRank)
	{
		return summon.sourceRaceKey() + "|" + levelRank;
	}

	/**
	 * @param summons   every creature of the catalog, in catalog order
	 * @param levelRank the summoning level of a creature
	 * @return each group's key with the creature that stands for it.
	 */
	static Map<String, SummonTemplate> basesOf(List<SummonTemplate> summons, ToIntFunction<SummonTemplate> levelRank)
	{
		Map<String, SummonTemplate> bases = new LinkedHashMap<>();
		Comparator<SummonTemplate> fewerTemplates = Comparator.comparingInt(summon -> summon.templateKeys().size());
		for (SummonTemplate summon : summons)
		{
			bases.merge(keyOf(summon, levelRank.applyAsInt(summon)), summon,
				(base, candidate) -> fewerTemplates.compare(candidate, base) < 0 ? candidate : base);
		}
		return bases;
	}
}
