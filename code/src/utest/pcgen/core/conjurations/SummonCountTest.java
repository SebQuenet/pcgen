/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package pcgen.core.conjurations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * How many creatures one summoning brings at most, the dice rolling their highest.
 */
class SummonCountTest
{
	@Test
	void aCreatureOfTheSpellsOwnLevelComesAloneWhateverTheBonuses()
	{
		assertEquals(1, SummonCount.maximum(6, 6, 2));
	}

	@Test
	void aCreatureOneLevelLowerComesThreeAtMost()
	{
		assertEquals(3, SummonCount.maximum(5, 6, 0));
	}

	@Test
	void aCreatureTwoLevelsLowerComesFiveAtMost()
	{
		assertEquals(5, SummonCount.maximum(2, 6, 0));
	}

	@Test
	void eachExtraCreatureAddsOneWhenTheSpellBringsSeveral()
	{
		assertEquals(7, SummonCount.maximum(4, 6, 2));
	}

	@Test
	void aCreatureAboveTheHighestKnownLevelCannotBeSummoned()
	{
		assertEquals(0, SummonCount.maximum(7, 6, 2));
	}
}
