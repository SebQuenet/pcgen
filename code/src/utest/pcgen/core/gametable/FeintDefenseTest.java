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
package pcgen.core.gametable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

/**
 * The difficulty a Bluff check must reach to feint a creature, as the Pathfinder Core Rulebook sets it.
 */
class FeintDefenseTest
{
	private static final boolean HUMANOID = true;
	private static final boolean NONHUMANOID = false;

	private static int difficultyOf(FeintDefense defense)
	{
		return assertInstanceOf(FeintDefense.Difficulty.class, defense).difficulty();
	}

	@Test
	void anUntrainedHumanoidIsFeintedAtTenPlusBaseAttackAndWisdom()
	{
		assertEquals(15, difficultyOf(FeintDefense.of(3, 2, OptionalInt.empty(), OptionalInt.of(10), HUMANOID)));
	}

	@Test
	void aHigherTrainedSenseMotiveReplacesBaseAttackAndWisdom()
	{
		assertEquals(20, difficultyOf(FeintDefense.of(3, 2, OptionalInt.of(10), OptionalInt.of(10), HUMANOID)));
	}

	@Test
	void aLowerTrainedSenseMotiveIsIgnored()
	{
		assertEquals(15, difficultyOf(FeintDefense.of(3, 2, OptionalInt.of(1), OptionalInt.of(10), HUMANOID)));
	}

	@Test
	void aNonhumanoidIsFourHarderToFeint()
	{
		assertEquals(19, difficultyOf(FeintDefense.of(3, 2, OptionalInt.empty(), OptionalInt.of(10), NONHUMANOID)));
	}

	@Test
	void anAnimalIntelligenceIsEightHarderToFeintEvenWhenNonhumanoid()
	{
		assertEquals(23, difficultyOf(FeintDefense.of(3, 2, OptionalInt.empty(), OptionalInt.of(2), NONHUMANOID)));
	}

	@Test
	void aCreatureWithoutIntelligenceCannotBeFeinted()
	{
		assertInstanceOf(FeintDefense.Impossible.class,
			FeintDefense.of(3, 2, OptionalInt.empty(), OptionalInt.empty(), NONHUMANOID));
	}
}
