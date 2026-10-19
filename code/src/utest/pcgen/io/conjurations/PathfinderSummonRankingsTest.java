/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package pcgen.io.conjurations;

import pcgen.core.conjurations.PathfinderSummonRankings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class PathfinderSummonRankingsTest
{
	@Test
	void ratingsKeepSpellAndLevelContext()
	{
		var monster = PathfinderSummonRankings.forOption("SM", 1, "Rat (Dire)");
		var nature = PathfinderSummonRankings.forOption("SNA", 1, "Rat (Dire)");
		assertNotNull(monster);
		assertNotNull(nature);
		assertEquals("red", monster.color());
		assertEquals("orange", nature.color());
		assertEquals(1, monster.score());
		assertEquals(2, nature.score());
		assertNull(PathfinderSummonRankings.forOption("Expanded", 1, "Rat (Dire)"));
	}
}
