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

import java.util.Map;

import org.junit.jupiter.api.Test;

class TreasureTypesTest
{
	private static final TreasureTypes TYPES = new TreasureTypes(Map.of("vrock", "double", "demon (vrock)", "triple",
		"glabrezu", "standard", "giant cockroach", "incidental", "sylvanien", "none"));

	@Test
	void theKeyItselfComesFirst()
	{
		assertEquals(TreasureType.TRIPLE, TYPES.of("Demon (Vrock)", "Démon (vrock)", "Outsider"));
	}

	@Test
	void aParenthesisedKeyFindsItsCreature()
	{
		assertEquals(TreasureType.STANDARD, TYPES.of("Demon (Glabrezu)", "Démon (glabrezu)", "Outsider"));
	}

	@Test
	void aParenthesisedKeyFindsItsCreatureNamedTheOtherWay()
	{
		assertEquals(TreasureType.INCIDENTAL, TYPES.of("Cockroach (Giant)", "Blatte géante", "Vermin"));
	}

	@Test
	void theLocalizedNameIsTriedLast()
	{
		assertEquals(TreasureType.NONE, TYPES.of("Treant", "Sylvanien", "Plant"));
	}

	@Test
	void anUnknownAnimalHasNone()
	{
		assertEquals(TreasureType.NONE, TYPES.of("Bear (Grizzly)", "Ours (grizzly)", "Animal"));
	}

	@Test
	void anUnknownVerminHasNone()
	{
		assertEquals(TreasureType.NONE, TYPES.of("Ant (Drone)", "Fourmi (bourdon)", "Vermin"));
	}

	@Test
	void anUnknownOutsiderHasStandard()
	{
		assertEquals(TreasureType.STANDARD, TYPES.of("Azata (Bralani)", "Azata (bralani)", "Outsider"));
	}

	@Test
	void theBundledFileKnowsTheTreant()
	{
		assertEquals(TreasureType.STANDARD, TreasureTypes.bundled().of("Treant", "Sylvanien", "Plant"));
	}
}
