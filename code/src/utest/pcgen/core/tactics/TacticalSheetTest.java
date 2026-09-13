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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Behaviour of the tactical sheet model.
 */
class TacticalSheetTest
{

	private static TacticalStep anEntry()
	{
		return new TacticalStep("Round 1", "Cast bless, then advance", "");
	}

	@Test
	void entryKeepsTheTextItWasGiven()
	{
		TacticalStep entry = new TacticalStep("HP below 12", "Drink a potion of cure light wounds", "Provokes");

		assertEquals("HP below 12", entry.trigger());
		assertEquals("Drink a potion of cure light wounds", entry.actions());
		assertEquals("Provokes", entry.note());
	}

	@Test
	void entryWithoutANoteStoresAnEmptyNote()
	{
		assertEquals("", new TacticalStep("Round 1", "Charge", null).note());
	}

	@Test
	void entryRejectsABlankTrigger()
	{
		assertThrows(IllegalArgumentException.class, () -> new TacticalStep("  ", "Charge", ""));
	}

	@Test
	void entryRejectsBlankActions()
	{
		assertThrows(IllegalArgumentException.class, () -> new TacticalStep("Round 1", "", ""));
	}

	@Test
	void sectionRejectsABlankTitle()
	{
		assertThrows(IllegalArgumentException.class, () -> new TacticalSection("", List.of(anEntry())));
	}

	@Test
	void sectionRejectsAnEmptyEntryList()
	{
		assertThrows(IllegalArgumentException.class, () -> new TacticalSection("Opening", List.of()));
	}

	@Test
	void sectionBlocksCannotBeModifiedThroughTheListPassedIn()
	{
		List<TacticalBlock> mutableBlocks = new java.util.ArrayList<>(List.of(anEntry()));
		TacticalSection section = new TacticalSection("Opening", mutableBlocks);

		mutableBlocks.clear();

		assertEquals(1, section.steps().size());
	}

	@Test
	void sheetRejectsAnEmptySectionList()
	{
		assertThrows(IllegalArgumentException.class, () -> new TacticalSheet(List.of()));
	}

	@Test
	void sheetKeepsSectionsInTheOrderGiven()
	{
		TacticalSection opening = new TacticalSection("Opening", List.of(anEntry()));
		TacticalSection emergency = new TacticalSection("Emergency", List.of(anEntry()));

		TacticalSheet sheet = new TacticalSheet(List.of(opening, emergency));

		assertEquals(List.of(opening, emergency), sheet.sections());
	}

	@Test
	void sheetsWithTheSameContentAreEqual()
	{
		TacticalSheet one = new TacticalSheet(List.of(new TacticalSection("Opening", List.of(anEntry()))));
		TacticalSheet other = new TacticalSheet(List.of(new TacticalSection("Opening", List.of(anEntry()))));

		assertEquals(one, other);
		assertTrue(one.hashCode() == other.hashCode());
	}
}
