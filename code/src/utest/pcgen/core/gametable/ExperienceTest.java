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

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * The Pathfinder experience award table, read both ways.
 */
class ExperienceTest
{
	@Test
	void aChallengeRatingOneIsWorthFourHundred()
	{
		assertEquals(400, Experience.experienceOf("1"));
	}

	@Test
	void aFractionalChallengeRatingIsRead()
	{
		assertEquals(135, Experience.experienceOf("1/3"));
	}

	@Test
	void anUnknownChallengeRatingIsWorthNothing()
	{
		assertEquals(0, Experience.experienceOf("—"));
	}

	@Test
	void threeChallengeRatingOneMakeAChallengeRatingFourEncounter()
	{
		assertEquals(Optional.of("4"), Experience.challengeRatingOf(1200));
	}

	@Test
	void anEncounterBetweenTwoRatingsTakesTheLower()
	{
		assertEquals(Optional.of("3"), Experience.challengeRatingOf(1000));
	}

	@Test
	void tooLittleExperienceHasNoChallengeRating()
	{
		assertEquals(Optional.empty(), Experience.challengeRatingOf(20));
	}

	@Test
	void thePlayersShareTheAwardRoundedDown()
	{
		assertEquals(266, Experience.share(800, 3));
	}

	@Test
	void nobodyToShareWithKeepsTheWhole()
	{
		assertEquals(800, Experience.share(800, 0));
	}
}
