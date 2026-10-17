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
package pcgen.core.conjurations;

import java.util.List;

/**
 * A titled run of conjurations, usually everything one summoning spell level
 * can bring.
 *
 * @param title   the group's heading. Never blank.
 * @param summons the creatures it holds, in reading order. Never empty, and
 *                never modifiable through the list passed in.
 */
public record SummonGroup(String title, List<SummonTemplate> summons)
{
	public SummonGroup
	{
		if (title == null || title.isBlank())
		{
			throw new IllegalArgumentException("A group of conjurations needs a title");
		}
		title = title.strip();
		if (summons == null || summons.isEmpty())
		{
			throw new IllegalArgumentException("Group '" + title + "' needs at least one conjuration");
		}
		summons = List.copyOf(summons);
	}
}
