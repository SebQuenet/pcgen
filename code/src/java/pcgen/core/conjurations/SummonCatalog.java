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

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Everything a character can conjure, read off the catalog's source text.
 *
 * @param groups the catalog's groups, in reading order. Not modifiable through
 *               the list passed in, and no two of their
 *               creatures share a name, whatever its case: a creature in play
 *               finds its stat line by name.
 */
public record SummonCatalog(List<SummonGroup> groups)
{
	public SummonCatalog
	{
		if (groups == null)
		{
			throw new IllegalArgumentException("A catalog of conjurations needs a group list");
		}
		groups = List.copyOf(groups);
		Set<String> names = new HashSet<>();
		for (SummonGroup group : groups)
		{
			for (SummonTemplate summon : group.summons())
			{
				if (!names.add(summon.name().toLowerCase(Locale.ROOT)))
				{
					throw new IllegalArgumentException("Two conjurations are named '" + summon.name() + "'");
				}
			}
		}
	}

	/**
	 * @param name a creature's name, in any case
	 * @return its stat line, empty when the catalog holds no such creature
	 */
	public Optional<SummonTemplate> find(String name)
	{
		if (name == null)
		{
			return Optional.empty();
		}
		String wanted = name.strip();
		return groups.stream().flatMap(group -> group.summons().stream())
			.filter(summon -> summon.name().equalsIgnoreCase(wanted)).findFirst();
	}
}
