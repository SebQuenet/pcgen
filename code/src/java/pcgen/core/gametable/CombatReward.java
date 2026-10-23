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

import java.util.Optional;

/**
 * What a finished fight is worth to the players. Nothing is written to their
 * characters: the players keep those.
 *
 * @param experience      what the monsters that entered the fight are worth together
 * @param challengeRating the encounter's challenge rating, when the total reaches one
 * @param players         the characters sharing the award
 * @param perPlayer       each one's part
 */
public record CombatReward(long experience, Optional<String> challengeRating, int players, long perPlayer)
{
	/**
	 * @param experience what the monsters are worth together.
	 * @param players    the characters sharing it.
	 * @return the award, shared.
	 */
	public static CombatReward of(long experience, int players)
	{
		return new CombatReward(experience, Experience.challengeRatingOf(experience), players,
			Experience.share(experience, players));
	}
}
