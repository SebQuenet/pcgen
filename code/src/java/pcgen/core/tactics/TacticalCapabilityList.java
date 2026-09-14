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

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Everything a character can do, in one filterable list rather than one card
 * per capability.
 *
 * @param title        the heading of the list. Never blank.
 * @param capabilities what it holds, in reading order. Never empty, and never
 *                     modifiable through the list passed in.
 */
public record TacticalCapabilityList(String title, List<TacticalCapability> capabilities) implements TacticalBlock
{
	public TacticalCapabilityList
	{
		title = TacticalText.required(title, "title");
		if (capabilities == null || capabilities.isEmpty())
		{
			throw new IllegalArgumentException("Capability list '" + title + "' needs at least one capability");
		}
		capabilities = List.copyOf(capabilities);
	}

	/**
	 * The roles the list uses, which is what its filter bar offers.
	 *
	 * @return every tag any capability carries, without repeats, in alphabetical
	 *         order so the bar does not reshuffle when the plan is edited.
	 */
	public List<String> tags()
	{
		return capabilities.stream().flatMap(capability -> capability.tags().stream()).distinct()
			.sorted(Comparator.comparing(tag -> tag.toLowerCase(Locale.ROOT))).toList();
	}
}
