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
package pcgen.core.tactics;

import java.util.List;

/**
 * A piece of gear worth a card of its own: its aura, its price, its powers,
 * its curse.
 *
 * <p>
 * Its rows are free labels, like a creature's, because what an item is worth
 * saying differs from item to item.
 *
 * @param name what to call it. Never blank.
 * @param rows what to say about it, in reading order. Never empty, and never
 *             modifiable through the list passed in.
 */
public record TacticalItem(String name, List<TacticalRow> rows) implements TacticalBlock
{
	public TacticalItem
	{
		name = TacticalText.required(name, "name");
		if (rows == null || rows.isEmpty())
		{
			throw new IllegalArgumentException("Item '" + name + "' needs at least one row");
		}
		rows = List.copyOf(rows);
	}
}
