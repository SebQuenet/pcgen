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
package pcgen.gui2.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import pcgen.AbstractCharacterTestCase;
import pcgen.core.PlayerCharacter;
import pcgen.core.tactics.TacticalEntry;
import pcgen.core.tactics.TacticalSection;
import pcgen.core.tactics.TacticalSheet;
import pcgen.facade.core.TacticalSheetFacade;

import org.junit.jupiter.api.Test;

/**
 * Editing a tactical sheet through the facade the Tactical tab talks to.
 */
public class TacticalSheetFacadeImplTest extends AbstractCharacterTestCase
{

	private static List<TacticalSection> sectionsOf(TacticalSheetFacade facade)
	{
		List<TacticalSection> listed = new java.util.ArrayList<>();
		facade.getSections().forEach(listed::add);
		return listed;
	}

	private static TacticalSection aSection(String title)
	{
		return new TacticalSection(title, List.of(new TacticalEntry("Round 1", "Charge", "")));
	}

	@Test
	public void testCharacterWithoutASheetHasNoSection()
	{
		TacticalSheetFacade facade = new TacticalSheetFacadeImpl(getCharacter());

		assertTrue(facade.getSections().isEmpty());
	}

	@Test
	public void testReadsTheSectionsAlreadyOnTheCharacter()
	{
		PlayerCharacter character = getCharacter();
		character.setTacticalSheet(new TacticalSheet(List.of(aSection("Opening"), aSection("Emergency"))));

		TacticalSheetFacade facade = new TacticalSheetFacadeImpl(character);

		assertEquals(List.of(aSection("Opening"), aSection("Emergency")), sectionsOf(facade));
	}

	@Test
	public void testAddedSectionReachesTheCharacterWithALineToFillIn()
	{
		PlayerCharacter character = getCharacter();
		TacticalSheetFacade facade = new TacticalSheetFacadeImpl(character);

		facade.addSection("Opening");

		List<TacticalSection> stored = character.getDisplay().getTacticalSheet().orElseThrow().sections();
		assertEquals(1, stored.size());
		assertEquals("Opening", stored.get(0).title());
		assertEquals(1, stored.get(0).entries().size());
	}

	@Test
	public void testReplacedSectionKeepsItsPosition()
	{
		PlayerCharacter character = getCharacter();
		character.setTacticalSheet(new TacticalSheet(List.of(aSection("Opening"), aSection("Emergency"))));
		TacticalSheetFacade facade = new TacticalSheetFacadeImpl(character);

		TacticalSection rewritten =
				new TacticalSection("Opening", List.of(new TacticalEntry("Round 1", "Cast bless", "")));
		facade.replaceSection(0, rewritten);

		List<TacticalSection> stored = character.getDisplay().getTacticalSheet().orElseThrow().sections();
		assertEquals(rewritten, stored.get(0));
		assertEquals(aSection("Emergency"), stored.get(1));
	}

	@Test
	public void testRemovingTheLastSectionLeavesTheCharacterWithoutASheet()
	{
		PlayerCharacter character = getCharacter();
		character.setTacticalSheet(new TacticalSheet(List.of(aSection("Opening"))));
		TacticalSheetFacade facade = new TacticalSheetFacadeImpl(character);

		facade.removeSection(0);

		assertTrue(character.getDisplay().getTacticalSheet().isEmpty());
		assertTrue(facade.getSections().isEmpty());
	}

	@Test
	public void testRemovingOneSectionKeepsTheOthers()
	{
		PlayerCharacter character = getCharacter();
		character.setTacticalSheet(new TacticalSheet(List.of(aSection("Opening"), aSection("Emergency"))));
		TacticalSheetFacade facade = new TacticalSheetFacadeImpl(character);

		facade.removeSection(0);

		assertEquals(List.of(aSection("Emergency")), sectionsOf(facade));
		assertEquals(List.of(aSection("Emergency")),
			character.getDisplay().getTacticalSheet().orElseThrow().sections());
	}

	@Test
	public void testEditingTheSheetMarksTheCharacterDirty()
	{
		PlayerCharacter character = getCharacter();
		TacticalSheetFacade facade = new TacticalSheetFacadeImpl(character);
		character.setDirty(false);

		facade.addSection("Opening");

		assertTrue(character.isDirty());
	}

	@Test
	public void testRemovingAPositionOutsideTheSheetChangesNothing()
	{
		PlayerCharacter character = getCharacter();
		character.setTacticalSheet(new TacticalSheet(List.of(aSection("Opening"))));
		TacticalSheetFacade facade = new TacticalSheetFacadeImpl(character);
		character.setDirty(false);

		facade.removeSection(4);

		assertEquals(List.of(aSection("Opening")), sectionsOf(facade));
		assertFalse(character.isDirty());
	}

	@Test
	public void testTwoSectionsWithTheSameContentAreEditedIndependently()
	{
		PlayerCharacter character = getCharacter();
		character.setTacticalSheet(new TacticalSheet(List.of(aSection("Emergency"), aSection("Emergency"))));
		TacticalSheetFacade facade = new TacticalSheetFacadeImpl(character);

		TacticalSection rewritten =
				new TacticalSection("Emergency", List.of(new TacticalEntry("HP below 12", "Withdraw", "")));
		facade.replaceSection(1, rewritten);

		assertEquals(List.of(aSection("Emergency"), rewritten), sectionsOf(facade));
	}
}
