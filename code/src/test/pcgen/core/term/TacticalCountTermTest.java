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
package pcgen.core.term;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import pcgen.AbstractCharacterTestCase;
import pcgen.core.PlayerCharacter;
import pcgen.core.tactics.TacticalEntry;
import pcgen.core.tactics.TacticalSection;
import pcgen.core.tactics.TacticalSheet;

import org.junit.jupiter.api.Test;

/**
 * The COUNT[TACTICS] and COUNT[TACTICENTRIES] variables output sheets loop on.
 */
public class TacticalCountTermTest extends AbstractCharacterTestCase
{

	private static TacticalSheet aSheetOfTwoSectionsAndThreeEntries()
	{
		return new TacticalSheet(List.of(
			new TacticalSection("Opening", List.of(new TacticalEntry("Round 1", "Cast bless", ""))),
			new TacticalSection("Emergency", List.of(new TacticalEntry("HP below 12", "Withdraw", ""),
				new TacticalEntry("Outnumbered", "Fall back", "")))));
	}

	@Test
	public void testCountsSections()
	{
		PlayerCharacter character = getCharacter();
		character.setTacticalSheet(aSheetOfTwoSectionsAndThreeEntries());

		assertEquals(2.0f, character.getVariableValue("COUNT[TACTICS]", ""));
	}

	@Test
	public void testCountsEntriesAcrossEverySection()
	{
		PlayerCharacter character = getCharacter();
		character.setTacticalSheet(aSheetOfTwoSectionsAndThreeEntries());

		assertEquals(3.0f, character.getVariableValue("COUNT[TACTICENTRIES]", ""));
	}

	@Test
	public void testCharacterWithoutASheetCountsZero()
	{
		PlayerCharacter character = getCharacter();

		assertEquals(0.0f, character.getVariableValue("COUNT[TACTICS]", ""));
		assertEquals(0.0f, character.getVariableValue("COUNT[TACTICENTRIES]", ""));
	}
}
