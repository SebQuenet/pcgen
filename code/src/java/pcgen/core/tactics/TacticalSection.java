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
 * A titled group of tactical entries, such as "Opening", "Rounds 1-2" or
 * "Emergency".
 *
 * @param title   the heading shown in the GUI and on the exported sheet. Never
 *                blank.
 * @param entries the lines of the section, in the order they should be read.
 *                Never empty, and never modifiable through the list passed in.
 */
public record TacticalSection(String title, List<TacticalEntry> entries)
{
	public TacticalSection
	{
		if (title == null || title.isBlank())
		{
			throw new IllegalArgumentException("A tactical section needs a non blank title");
		}
		if (entries == null || entries.isEmpty())
		{
			throw new IllegalArgumentException("Tactical section '" + title + "' needs at least one entry");
		}
		title = title.strip();
		entries = List.copyOf(entries);
	}
}
