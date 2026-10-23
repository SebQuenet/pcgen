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

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class TreasureBudgetTest
{
	@Test
	void standardIsTheValueOfItsChallengeRating()
	{
		assertEquals(4_250, TreasureType.STANDARD.apply(TreasureBudget.forChallengeRating("9")));
	}

	@Test
	void incidentalIsHalf()
	{
		assertEquals(3_200, TreasureType.INCIDENTAL.apply(6_400));
	}

	@Test
	void tripleIsThreeTimes()
	{
		assertEquals(19_200, TreasureType.TRIPLE.apply(6_400));
	}

	@Test
	void noneAndNpcGearDrawNothing()
	{
		assertEquals(List.of(0L, 0L), List.of(TreasureType.NONE.apply(6_400), TreasureType.NPC_GEAR.apply(6_400)));
	}

	@Test
	void variantsReadAsTheirType()
	{
		assertEquals(List.of(TreasureType.DOUBLE, TreasureType.TRIPLE, TreasureType.INCIDENTAL, TreasureType.INCIDENTAL,
			TreasureType.STANDARD, TreasureType.STANDARD, TreasureType.NPC_GEAR, TreasureType.STANDARD,
			TreasureType.NONE),
			Stream.of("double standard", "Triple standard", "half standard", "50% standard", "?", "",
				"NPC Gear (leather armor, other treasure)", "standard (+1 scimitar, other treasure)", "None")
				.map(TreasureType::parse).toList());
	}

	@Test
	void aFractionalChallengeRatingTakesItsShareOfTheFirst()
	{
		assertEquals(130, TreasureBudget.forChallengeRating("1/2"));
	}

	@Test
	void aboveTwentyKeepsTheValueOfTwenty()
	{
		assertEquals(67_000, TreasureBudget.forChallengeRating("23"));
	}

	@Test
	void anUnreadableChallengeRatingIsWorthNothing()
	{
		assertEquals(0, TreasureBudget.forChallengeRating("-"));
	}
}
