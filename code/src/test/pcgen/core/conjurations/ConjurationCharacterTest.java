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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import pcgen.AbstractCharacterTestCase;
import pcgen.core.PlayerCharacter;

import org.junit.jupiter.api.Test;

/**
 * What a character keeps of its conjurations: the catalog as written, and the
 * creatures it has conjured into the fight.
 */
public class ConjurationCharacterTest extends AbstractCharacterTestCase
{

	private static final String A_CATALOG = """
		## Summon Monster IV
		summon: Hound Archon
		  hp: 51
		""";

	private static final ConjurationSession A_FIGHT = new ConjurationSession(12,
		List.of(new ConjuredCreature("Hound Archon", 1, 14, OptionalInt.of(7))));

	@Test
	public void hasNoCatalogUntilOneIsWritten()
	{
		assertEquals(Optional.empty(), getCharacter().getDisplay().getConjurationCatalog());
	}

	@Test
	public void storesTheCatalogExactlyAsItWasWritten()
	{
		PlayerCharacter character = getCharacter();

		character.setConjurationCatalog(A_CATALOG);

		assertEquals(A_CATALOG, character.getDisplay().getConjurationCatalog().orElseThrow());
	}

	@Test
	public void refusesABlankCatalog()
	{
		assertThrows(IllegalArgumentException.class, () -> getCharacter().setConjurationCatalog("  "));
	}

	@Test
	public void writingTheCatalogMarksTheCharacterModified()
	{
		PlayerCharacter character = getCharacter();
		character.setDirty(false);

		character.setConjurationCatalog(A_CATALOG);

		assertTrue(character.isDirty());
	}

	@Test
	public void remembersTheFight()
	{
		PlayerCharacter character = getCharacter();

		character.setConjurationSession(A_FIGHT);

		assertEquals(A_FIGHT, character.getDisplay().getConjurationSession());
	}

	@Test
	public void recordingTheFightMarksTheCharacterModified()
	{
		PlayerCharacter character = getCharacter();
		character.setDirty(false);

		character.setConjurationSession(A_FIGHT);

		assertTrue(character.isDirty());
	}

	@Test
	public void clearingForgetsTheCatalogAndTheFight()
	{
		PlayerCharacter character = getCharacter();
		character.setConjurationCatalog(A_CATALOG);
		character.setConjurationSession(A_FIGHT);

		character.clearConjurations();

		assertEquals(List.of(Optional.empty(), ConjurationSession.empty()), List.of(
			character.getDisplay().getConjurationCatalog(), character.getDisplay().getConjurationSession()));
	}
}
