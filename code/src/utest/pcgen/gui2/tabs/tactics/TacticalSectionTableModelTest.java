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
package pcgen.gui2.tabs.tactics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import pcgen.core.tactics.TacticalEntry;
import pcgen.core.tactics.TacticalSection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The table the Tactical tab shows for one section, and what it publishes when
 * the user edits a cell.
 */
class TacticalSectionTableModelTest
{

	private List<TacticalSection> published;
	private TacticalSectionTableModel tableModel;

	@BeforeEach
	void setUp()
	{
		published = new ArrayList<>();
		tableModel = new TacticalSectionTableModel(published::add);
		tableModel.showSection(new TacticalSection("Opening",
			List.of(new TacticalEntry("Round 1", "Cast bless", "Provokes"),
				new TacticalEntry("Round 2", "Charge", ""))));
	}

	@Test
	void showsOneRowPerEntryAndThreeColumns()
	{
		assertEquals(2, tableModel.getRowCount());
		assertEquals(3, tableModel.getColumnCount());
	}

	@Test
	void readsEachFieldInItsOwnColumn()
	{
		assertEquals("Round 1", tableModel.getValueAt(0, 0));
		assertEquals("Cast bless", tableModel.getValueAt(0, 1));
		assertEquals("Provokes", tableModel.getValueAt(0, 2));
		assertEquals("Charge", tableModel.getValueAt(1, 1));
	}

	@Test
	void showsNothingWhenNoSectionIsSelected()
	{
		tableModel.showNoSection();

		assertEquals(0, tableModel.getRowCount());
	}

	@Test
	void editingACellPublishesTheWholeSection()
	{
		tableModel.setValueAt("Cast bless, then advance", 0, 1);

		assertEquals(1, published.size());
		assertEquals(new TacticalSection("Opening",
			List.of(new TacticalEntry("Round 1", "Cast bless, then advance", "Provokes"),
				new TacticalEntry("Round 2", "Charge", ""))),
			published.get(0));
	}

	@Test
	void editingACellToBlankPublishesNothingAndKeepsTheOldText()
	{
		tableModel.setValueAt("   ", 0, 1);

		assertTrue(published.isEmpty());
		assertEquals("Cast bless", tableModel.getValueAt(0, 1));
	}

	@Test
	void clearingANotePublishesTheSectionSinceANoteMayBeEmpty()
	{
		tableModel.setValueAt("", 0, 2);

		assertEquals(1, published.size());
		assertEquals("", published.get(0).entries().get(0).note());
	}

	@Test
	void addedLinePublishesASectionWithOneMoreEntry()
	{
		tableModel.addEntry("When", "Do");

		assertEquals(3, published.get(0).entries().size());
		assertEquals("When", published.get(0).entries().get(2).trigger());
	}

	@Test
	void removedLinePublishesASectionWithoutIt()
	{
		tableModel.removeEntry(0);

		assertEquals(List.of(new TacticalEntry("Round 2", "Charge", "")), published.get(0).entries());
	}

	@Test
	void removingTheLastLinePublishesNothingSinceASectionIsNeverEmpty()
	{
		tableModel.removeEntry(0);
		published.clear();

		tableModel.removeEntry(0);

		assertTrue(published.isEmpty());
	}

	@Test
	void everyCellIsEditable()
	{
		assertTrue(tableModel.isCellEditable(0, 0));
		assertTrue(tableModel.isCellEditable(1, 2));
	}
}
