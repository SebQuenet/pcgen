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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import pcgen.AbstractCharacterTestCase;
import pcgen.core.PlayerCharacter;
import pcgen.core.conjurations.ConjurationSession;
import pcgen.core.conjurations.ConjuredCreature;
import pcgen.core.conjurations.SummonParseError;
import pcgen.facade.core.ConjurationSheetFacade;

import org.junit.jupiter.api.Test;

/**
 * Editing a catalog of conjurations, and playing the fight, through the facade
 * the Conjurations tab and its page talk to.
 */
public class ConjurationSheetFacadeImplTest extends AbstractCharacterTestCase
{

	private static final String ARCHON = "Hound Archon";

	private static final String A_CATALOG = """
		## Summon Monster IV
		summon: Hound Archon
		  hp: 51
		""";

	private static ConjurationSheetFacade facadeFor(PlayerCharacter character)
	{
		return new ConjurationSheetFacadeImpl(character);
	}

	private static List<ConjuredCreature> inPlay(PlayerCharacter character)
	{
		return character.getDisplay().getConjurationSession().conjured();
	}

	@Test
	public void reportsNoCatalogForACharacterWithoutOne()
	{
		assertEquals("", facadeFor(getCharacter()).getCatalogSource());
	}

	@Test
	public void storesTheCatalogOnTheCharacter()
	{
		PlayerCharacter character = getCharacter();

		facadeFor(character).setCatalogSource(A_CATALOG);

		assertEquals(A_CATALOG, character.getDisplay().getConjurationCatalog().orElseThrow());
	}

	@Test
	public void storesTextThatDoesNotReadRatherThanLosingIt()
	{
		PlayerCharacter character = getCharacter();

		facadeFor(character).setCatalogSource("## IV\nmonster: Hound Archon\n");

		assertEquals(Optional.of("## IV\nmonster: Hound Archon\n"), character.getDisplay().getConjurationCatalog());
	}

	@Test
	public void removesTheCatalogWhenItsTextIsErased()
	{
		PlayerCharacter character = getCharacter();
		ConjurationSheetFacade facade = facadeFor(character);
		facade.setCatalogSource(A_CATALOG);

		facade.setCatalogSource("");

		assertEquals(Optional.empty(), character.getDisplay().getConjurationCatalog());
	}

	@Test
	public void namesTheOffendingLineOfTextThatDoesNotRead()
	{
		List<SummonParseError> errors = facadeFor(getCharacter()).errorsIn("## IV\nmonster: Hound Archon\n");

		assertEquals(List.of(2), errors.stream().map(SummonParseError::line).toList());
	}

	@Test
	public void findsNothingWrongWithACatalogThatReads()
	{
		assertEquals(List.of(), facadeFor(getCharacter()).errorsIn(A_CATALOG));
	}

	@Test
	public void givesEachNewCreatureOfTheSameNameTheNextNumber()
	{
		ConjurationSheetFacade facade = facadeFor(getCharacter());

		assertEquals(List.of(1, 2, 3), List.of(facade.conjure(ARCHON), facade.conjure(ARCHON), facade.conjure(ARCHON)));
	}

	@Test
	public void bringsANewCreatureIntoTheFightForTheDurationSet()
	{
		PlayerCharacter character = getCharacter();
		ConjurationSheetFacade facade = facadeFor(character);
		facade.setDuration(12);

		facade.conjure(ARCHON);

		assertEquals(List.of(new ConjuredCreature(ARCHON, 1, 0, OptionalInt.of(12))), inPlay(character));
	}

	@Test
	public void recordsTheDamageACreatureTakes()
	{
		PlayerCharacter character = getCharacter();
		ConjurationSheetFacade facade = facadeFor(character);
		facade.conjure(ARCHON);

		facade.setDamage(ARCHON, 1, 14);

		assertEquals(14, inPlay(character).getFirst().damage());
	}

	@Test
	public void neverRecordsDamageBelowZero()
	{
		PlayerCharacter character = getCharacter();
		ConjurationSheetFacade facade = facadeFor(character);
		facade.conjure(ARCHON);

		facade.setDamage(ARCHON, 1, -5);

		assertEquals(0, inPlay(character).getFirst().damage());
	}

	@Test
	public void recordsTheRoundsACreatureHasLeft()
	{
		PlayerCharacter character = getCharacter();
		ConjurationSheetFacade facade = facadeFor(character);
		facade.conjure(ARCHON);

		facade.setRoundsLeft(ARCHON, 1, 4);

		assertEquals(OptionalInt.of(4), inPlay(character).getFirst().roundsLeft());
	}

	@Test
	public void takesARoundFromEveryCreatureAtTheNextRound()
	{
		PlayerCharacter character = getCharacter();
		ConjurationSheetFacade facade = facadeFor(character);
		facade.setDuration(12);
		facade.conjure(ARCHON);
		facade.conjure(ARCHON);

		facade.nextRound();

		assertEquals(List.of(OptionalInt.of(11), OptionalInt.of(11)),
			inPlay(character).stream().map(ConjuredCreature::roundsLeft).toList());
	}

	@Test
	public void dismissesThatCreatureOnly()
	{
		PlayerCharacter character = getCharacter();
		ConjurationSheetFacade facade = facadeFor(character);
		facade.conjure(ARCHON);
		facade.conjure(ARCHON);

		facade.dismiss(ARCHON, 1);

		assertEquals(List.of(2), inPlay(character).stream().map(ConjuredCreature::number).toList());
	}

	@Test
	public void dismissingEveryoneEndsTheFight()
	{
		PlayerCharacter character = getCharacter();
		ConjurationSheetFacade facade = facadeFor(character);
		facade.conjure(ARCHON);
		facade.conjure(ARCHON);

		facade.dismissAll();

		assertEquals(ConjurationSession.empty(), character.getDisplay().getConjurationSession());
	}

	@Test
	public void treatsANegativeDurationAsRoundsNotCounted()
	{
		PlayerCharacter character = getCharacter();
		ConjurationSheetFacade facade = facadeFor(character);

		facade.setDuration(-3);

		assertTrue(character.getDisplay().getConjurationSession().isEmpty());
	}
}
