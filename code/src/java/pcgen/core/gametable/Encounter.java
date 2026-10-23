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
package pcgen.core.gametable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An encounter the game master prepares before the session.
 *
 * @param name     what the scenario calls it, such as "Goblin ambush". Never blank.
 * @param monsters its monsters, by kind
 * @param notes    free notes
 */
public record Encounter(String name, List<EncounterMonster> monsters, String notes)
{
	public Encounter
	{
		if (Objects.requireNonNull(name).isBlank())
		{
			throw new IllegalArgumentException("An encounter has a name");
		}
		name = name.strip();
		monsters = List.copyOf(monsters);
		notes = Objects.requireNonNull(notes);
	}

	/**
	 * @param name what the scenario calls it.
	 * @return an encounter with no monster yet.
	 */
	public static Encounter named(String name)
	{
		return new Encounter(name, List.of(), "");
	}

	public Encounter withName(String newName)
	{
		return new Encounter(newName, monsters, notes);
	}

	public Encounter withNotes(String newNotes)
	{
		return new Encounter(name, monsters, newNotes);
	}

	public Encounter withMonsters(List<EncounterMonster> newMonsters)
	{
		return new Encounter(name, newMonsters, notes);
	}

	public Encounter withMonster(EncounterMonster added)
	{
		List<EncounterMonster> all = new ArrayList<>(monsters);
		all.add(added);
		return withMonsters(all);
	}
}
