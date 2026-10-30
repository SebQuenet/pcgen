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
package pcgen.core.gametable.catalog;

import java.util.List;

/**
 * The bestiary's entries: races, NPC files, NPC kits and unique creatures.
 *
 * @param entries the entries, in the order listed.
 */
public record Catalog(List<CatalogEntry> entries)
{
	/**
	 * @param entry an entry kept by a filter.
	 * @param match yes, or unknown while a value the filter needs is not computed.
	 */
	public record Filtered(CatalogEntry entry, Match match)
	{
	}

	public Catalog
	{
		entries = List.copyOf(entries);
	}

	public static Catalog of(List<CatalogEntry> entries)
	{
		return new Catalog(entries);
	}

	/**
	 * @param criterion what the game master asks.
	 * @return the entries that meet it or may meet it, in order.
	 */
	public List<Filtered> filter(Criterion criterion)
	{
		return entries.stream().map(entry -> new Filtered(entry, criterion.evaluate(entry.facts())))
			.filter(filtered -> filtered.match() != Match.NO).toList();
	}

	/**
	 * @param index computed values.
	 * @return this catalog with each entry's indexed values laid over its own.
	 */
	public Catalog withIndex(BestiaryIndex index)
	{
		return new Catalog(entries.stream().map(entry -> entry.indexKey().flatMap(index::facts).map(entry::withFacts)
			.orElse(entry)).toList());
	}
}
