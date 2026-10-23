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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The Pathfinder experience award table (Core Rulebook, table 12–2): what a creature
 * of a challenge rating is worth, and the challenge rating of an encounter worth a total.
 */
public final class Experience
{
	private static final Map<String, Long> AWARDS = new LinkedHashMap<>();

	static
	{
		AWARDS.put("1/8", 50L);
		AWARDS.put("1/6", 65L);
		AWARDS.put("1/4", 100L);
		AWARDS.put("1/3", 135L);
		AWARDS.put("1/2", 200L);
		long[] wholeRatings = {400, 600, 800, 1_200, 1_600, 2_400, 3_200, 4_800, 6_400, 9_600, 12_800, 19_200, 25_600,
			38_400, 51_200, 76_800, 102_400, 153_600, 204_800, 307_200, 409_600, 614_400, 819_200, 1_228_800,
			1_638_400};
		for (int rating = 1; rating <= wholeRatings.length; rating++)
		{
			AWARDS.put(String.valueOf(rating), wholeRatings[rating - 1]);
		}
	}

	private Experience()
	{
	}

	/**
	 * @param challengeRating a challenge rating as PCGen writes it, such as "1/3" or "7".
	 * @return what a creature of that rating is worth; nothing for a rating the table lacks.
	 */
	public static long experienceOf(String challengeRating)
	{
		return AWARDS.getOrDefault(challengeRating.strip(), 0L);
	}

	/**
	 * @param totalExperience what the creatures of an encounter are worth together.
	 * @return the highest challenge rating that total reaches; empty below the lowest.
	 */
	public static Optional<String> challengeRatingOf(long totalExperience)
	{
		Optional<String> reached = Optional.empty();
		for (Map.Entry<String, Long> award : AWARDS.entrySet())
		{
			if (award.getValue() <= totalExperience)
			{
				reached = Optional.of(award.getKey());
			}
		}
		return reached;
	}

	/**
	 * @param totalExperience the award of an encounter.
	 * @param players         the characters sharing it.
	 * @return each one's part, rounded down; the whole when nobody shares it.
	 */
	public static long share(long totalExperience, int players)
	{
		return (players < 1) ? totalExperience : totalExperience / players;
	}
}
