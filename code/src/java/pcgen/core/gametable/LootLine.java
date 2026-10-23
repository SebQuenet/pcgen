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
import java.util.Optional;

/**
 * One line of a fight's loot, as the game master reads it to the players.
 *
 * @param name         what the players find.
 * @param quantity     how many of it.
 * @param carrier      the monster that had it.
 * @param drawnAs      the treasure it was drawn for; empty when the monster carried it.
 * @param value        what the line is worth, every item of it together, in gold pieces.
 * @param resoldInFull whether it sells for its whole value: coins, gems, art objects, trade goods.
 * @param selected     whether the game master keeps it in the loot.
 */
public record LootLine(String name, int quantity, String carrier, Optional<TreasureType> drawnAs, long value,
	boolean resoldInFull, boolean selected)
{
	public LootLine
	{
		Objects.requireNonNull(name);
		Objects.requireNonNull(carrier);
		Objects.requireNonNull(drawnAs);
		if (quantity < 1 || value < 0)
		{
			throw new IllegalArgumentException("A loot line has one item or more and a value: " + name);
		}
	}

	/**
	 * @return a line for what a monster carried.
	 */
	public static LootLine carried(String name, int quantity, String carrier, long value, boolean resoldInFull,
		boolean selected)
	{
		return new LootLine(name, quantity, carrier, Optional.empty(), value, resoldInFull, selected);
	}

	/**
	 * @return what the line sells for.
	 */
	public long resale()
	{
		return resoldInFull ? value : value / 2;
	}

	public LootLine selecting(boolean isSelected)
	{
		return new LootLine(name, quantity, carrier, drawnAs, value, resoldInFull, isSelected);
	}
}
