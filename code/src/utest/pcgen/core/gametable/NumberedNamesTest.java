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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Newcomers to a fight are told apart by number.
 */
class NumberedNamesTest
{
	private static Combat with(String... names)
	{
		Combat combat = Combat.empty();
		for (String name : names)
		{
			combat = combat.with(Combatant.player(combat.nextId(), name, Optional.empty()));
		}
		return combat;
	}

	@Test
	void aSingleNewcomerKeepsItsName()
	{
		assertEquals(List.of("Goblin chief"), GameTableSession.numberedNames(with("Seoni"), "Goblin chief", 1));
	}

	@Test
	void severalNewcomersAreNumbered()
	{
		assertEquals(List.of("Goblin 1", "Goblin 2", "Goblin 3"), GameTableSession.numberedNames(with(), "Goblin", 3));
	}

	@Test
	void numberingGoesOnFromThoseAlreadyFighting()
	{
		assertEquals(List.of("Goblin 3"), GameTableSession.numberedNames(with("Goblin 1", "Goblin 2"), "Goblin", 1));
	}

	@Test
	void aNameAlreadyTakenIsNumbered()
	{
		assertEquals(List.of("Goblin 2"), GameTableSession.numberedNames(with("Goblin"), "Goblin", 1));
	}
}
