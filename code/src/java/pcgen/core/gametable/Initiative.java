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

import java.util.Objects;
import java.util.OptionalInt;

/**
 * Where a combatant stands in the order.
 *
 * @param score    the rolled or announced initiative; empty until known
 * @param modifier the initiative modifier, which breaks ties
 * @param dual     whether it acts twice a round, the second time twenty lower
 */
public record Initiative(OptionalInt score, int modifier, boolean dual)
{
	/** Twenty lower: when a dual initiative acts the second time. */
	public static final int SECOND_TURN_DROP = 20;

	public Initiative
	{
		Objects.requireNonNull(score);
	}

	/**
	 * @param newScore the initiative rolled or announced.
	 * @return this initiative with that score.
	 */
	public Initiative withScore(int newScore)
	{
		return new Initiative(OptionalInt.of(newScore), modifier, dual);
	}
}
