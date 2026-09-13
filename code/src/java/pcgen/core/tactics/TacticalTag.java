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
 * How a spell is filed on the sheet, so the repertoire can be filtered by what
 * a spell is for rather than only by its level.
 *
 * @param spellName the spell being tagged. Never blank.
 * @param tags      its tags, in reading order. Never empty, and never
 *                  modifiable through the list passed in.
 */
public record TacticalTag(String spellName, List<String> tags)
{
	public TacticalTag
	{
		spellName = TacticalText.required(spellName, "spell name");
		if (tags == null || tags.isEmpty())
		{
			throw new IllegalArgumentException("Spell '" + spellName + "' needs at least one tag");
		}
		tags = tags.stream().map(String::strip).filter(tag -> !tag.isEmpty()).toList();
		if (tags.isEmpty())
		{
			throw new IllegalArgumentException("Spell '" + spellName + "' needs at least one non blank tag");
		}
	}
}
