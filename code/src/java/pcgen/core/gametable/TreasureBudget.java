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

/**
 * The treasure values per encounter of the medium progression (Core Rulebook, table 12–5).
 * The table stops at 20; a higher challenge rating keeps the value of 20, and a fractional
 * one takes its share of the value of 1.
 */
public final class TreasureBudget
{
	private static final long[] MEDIUM = {260, 550, 800, 1_150, 1_550, 2_000, 2_600, 3_350, 4_250, 5_450, 7_000, 9_000,
		11_600, 15_000, 19_500, 25_000, 32_000, 41_000, 53_000, 67_000};

	private TreasureBudget()
	{
	}

	/**
	 * @param challengeRating a challenge rating as PCGen writes it, such as "1/3" or "7".
	 * @return the treasure value of that rating, in gold pieces; nothing for one that cannot be read.
	 */
	public static long forChallengeRating(String challengeRating)
	{
		String rating = challengeRating.strip();
		try
		{
			if (rating.startsWith("1/"))
			{
				return MEDIUM[0] / Integer.parseInt(rating.substring(2));
			}
			int whole = Integer.parseInt(rating);
			return whole < 1 ? 0 : MEDIUM[Math.min(whole, MEDIUM.length) - 1];
		}
		catch (NumberFormatException unreadable)
		{
			return 0;
		}
	}
}
