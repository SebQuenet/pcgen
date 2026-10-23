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

/**
 * What to draw for one monster, kept with the loot so that the draw can be made again.
 *
 * @param carrier      the monster's name.
 * @param creatureType its creature type, such as "outsider", which picks the kinds of treasure.
 * @param type         its treasure type.
 * @param amount       the gold pieces to draw, what it carried already deducted.
 */
public record LootDraw(String carrier, String creatureType, TreasureType type, long amount)
{
	public LootDraw
	{
		Objects.requireNonNull(carrier);
		Objects.requireNonNull(creatureType);
		Objects.requireNonNull(type);
		if (amount < 0)
		{
			throw new IllegalArgumentException("A draw is never negative: " + carrier);
		}
	}
}
