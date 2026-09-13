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
 * What a character should do, round by round and situation by situation.
 *
 * <p>
 * A tactical sheet is written by an AI agent through the MCP server or by hand
 * in the GUI, saved with the character and rendered on the exported character
 * sheet.
 *
 * @param sections the sections of the sheet, in reading order. Never empty, and
 *                 never modifiable through the list passed in. A character
 *                 without a tactical sheet holds no {@code TacticalSheet} at
 *                 all rather than an empty one.
 */
public record TacticalSheet(List<TacticalSection> sections)
{
	public TacticalSheet
	{
		if (sections == null || sections.isEmpty())
		{
			throw new IllegalArgumentException("A tactical sheet needs at least one section");
		}
		sections = List.copyOf(sections);
	}
}
