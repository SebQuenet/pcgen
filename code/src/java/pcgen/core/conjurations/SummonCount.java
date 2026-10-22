/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package pcgen.core.conjurations;

/**
 * The most creatures one summon spell brings, the dice rolling their highest:
 * one of its own level, 1d3 of the level below, 1d4+1 of any lower level.
 */
public final class SummonCount
{
	private static final int ONE_LEVEL_LOWER = 3;
	private static final int LOWER_LEVELS = 5;

	private SummonCount()
	{
	}

	/**
	 * @param creatureLevel    the summoning list the creature is on
	 * @param highestLevel     the highest summon spell known in that family
	 * @param extraCreatures   the creatures feats and abilities add when a spell brings several
	 * @return the most creatures of that kind one spell brings, or 0 when none can be summoned.
	 */
	public static int maximum(int creatureLevel, int highestLevel, int extraCreatures)
	{
		int below = highestLevel - creatureLevel;
		if (below < 0)
		{
			return 0;
		}
		if (below == 0)
		{
			return 1;
		}
		return (below == 1 ? ONE_LEVEL_LOWER : LOWER_LEVELS) + extraCreatures;
	}
}
