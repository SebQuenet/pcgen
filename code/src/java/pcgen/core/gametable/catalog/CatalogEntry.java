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
import java.util.Objects;
import java.util.Optional;

import pcgen.core.gametable.MonsterRecipe;
import pcgen.core.gametable.MonsterSource;

/**
 * One line of the bestiary: a creature the game master can look at and send to an encounter or a fight.
 *
 * @param name   its name as listed.
 * @param recipe how it is built; a unique creature's carries its templates and hit dice.
 * @param kind   where it comes from.
 * @param facts  what is known of its values.
 */
public record CatalogEntry(String name, MonsterRecipe recipe, EntryKind kind, EntryFacts facts)
{
	public CatalogEntry
	{
		Objects.requireNonNull(name);
		Objects.requireNonNull(recipe);
		Objects.requireNonNull(kind);
		Objects.requireNonNull(facts);
	}

	/**
	 * @param name   its name as listed.
	 * @param source what it is built from, untouched.
	 * @param kind   where it comes from.
	 * @param facts  what is known of its values.
	 */
	public CatalogEntry(String name, MonsterSource source, EntryKind kind, EntryFacts facts)
	{
		this(name, new MonsterRecipe(source, List.of(), List.of(), 0), kind, facts);
	}

	/**
	 * @return the key of this entry in the bestiary's index; a unique creature has none.
	 */
	public Optional<IndexKey> indexKey()
	{
		if (kind == EntryKind.UNIQUE)
		{
			return Optional.empty();
		}
		return Optional.of(switch (recipe.source())
		{
			case MonsterSource.BestiaryRace race -> new IndexKey(IndexKey.RACE, race.raceKey());
			case MonsterSource.CharacterFile file -> new IndexKey(IndexKey.FILE, file.path());
			case MonsterSource.NpcKit kit -> new IndexKey(IndexKey.KIT, kit.kitKey());
		});
	}

	/**
	 * @param more indexed facts, which complete this entry's own: a value read on the sources loaded now wins.
	 * @return this entry with them.
	 */
	public CatalogEntry withFacts(EntryFacts more)
	{
		EntryFacts merged = new EntryFacts(more.values(), more.buildFailure().or(facts::buildFailure)).with(facts.values());
		return new CatalogEntry(name, recipe, kind, more.buildFailure().isPresent()
			? new EntryFacts(merged.values(), more.buildFailure()) : merged);
	}
}
