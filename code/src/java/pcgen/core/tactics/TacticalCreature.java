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
 * A creature the character brings along: a summon, a companion, a cohort.
 * Written out in full, because the character's own data holds nothing about it.
 *
 * @param name     the creature's name. Never blank.
 * @param source   what brings it, such as the spell that summons it. Empty
 *                 when there is nothing to say.
 * @param duration how long it stays. Empty when there is nothing to say.
 * @param rows     its stat block, in reading order. Never empty, and never
 *                 modifiable through the list passed in.
 */
public record TacticalCreature(String name, String source, String duration, List<TacticalRow> rows)
		implements TacticalBlock
{
	public TacticalCreature
	{
		name = TacticalText.required(name, "name");
		source = TacticalText.optional(source);
		duration = TacticalText.optional(duration);
		if (rows == null || rows.isEmpty())
		{
			throw new IllegalArgumentException("Creature '" + name + "' needs at least one row");
		}
		rows = List.copyOf(rows);
	}
}
