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
 * The character's spell repertoire, read off the character and filed by the
 * tags the writer attached.
 *
 * @param source which list to show. Never null.
 * @param tags   the tags to file spells under. A spell the writer did not tag
 *               still shows, untagged. Never modifiable through the list passed
 *               in.
 */
public record TacticalSpellList(SpellSource source, List<TacticalTag> tags) implements TacticalBlock
{
	public TacticalSpellList
	{
		if (source == null)
		{
			throw new IllegalArgumentException("A spell list needs a source");
		}
		tags = (tags == null) ? List.of() : List.copyOf(tags);
	}
}
