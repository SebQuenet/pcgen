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

import pcgen.AbstractCharacterTestCase;
import pcgen.core.PlayerCharacter;
import pcgen.core.tactics.TacticalParseError;
import pcgen.facade.core.TacticalSheetFacade;

import org.junit.jupiter.api.Test;

/**
 * Editing a tactical sheet through the facade the Tactical tab talks to.
 */
public class TacticalSheetFacadeImplTest extends AbstractCharacterTestCase
{

	private static final String A_PLAN = """
		## Before the fight
		resource: Mythic power | 11 | immediate
		""";

	private TacticalSheetFacade facadeFor(PlayerCharacter character)
	{
		return new TacticalSheetFacadeImpl(character);
	}

	@Test
	public void reportsNoPlanForACharacterWithoutOne()
	{
		assertEquals("", facadeFor(getCharacter()).getPlanSource());
	}

	@Test
	public void storesThePlanOnTheCharacter()
	{
		PlayerCharacter character = getCharacter();
		TacticalSheetFacade facade = facadeFor(character);

		facade.setPlanSource(A_PLAN);

		assertEquals(A_PLAN, character.getDisplay().getTacticalPlan().orElseThrow());
		assertEquals(A_PLAN, facade.getPlanSource());
	}

	@Test
	public void storesTextThatDoesNotReadRatherThanLosingIt()
	{
		PlayerCharacter character = getCharacter();
		TacticalSheetFacade facade = facadeFor(character);

		facade.setPlanSource("## Before the fight\nmanoeuvre: Trip the ogre\n");

		assertTrue(character.getDisplay().getTacticalPlan().isPresent());
	}

	@Test
	public void namesTheOffendingLineOfTextThatDoesNotRead()
	{
		List<TacticalParseError> errors =
				facadeFor(getCharacter()).errorsIn("## Before the fight\nmanoeuvre: Trip the ogre\n");

		assertEquals(List.of(2), errors.stream().map(TacticalParseError::line).toList());
	}

	@Test
	public void findsNothingWrongWithAPlanThatReads()
	{
		assertEquals(List.of(), facadeFor(getCharacter()).errorsIn(A_PLAN));
	}

	@Test
	public void blankSourceRemovesThePlan()
	{
		PlayerCharacter character = getCharacter();
		TacticalSheetFacade facade = facadeFor(character);
		facade.setPlanSource(A_PLAN);

		facade.setPlanSource("");

		assertEquals(List.of(), character.getDisplay().getTacticalPlan().stream().toList());
	}

	@Test
	public void recordsDamageTaken()
	{
		PlayerCharacter character = getCharacter();

		facadeFor(character).setDamage(19);

		assertEquals(19, character.getDisplay().getTacticalSession().damageTaken());
	}

	@Test
	public void treatsNegativeDamageAsNone()
	{
		PlayerCharacter character = getCharacter();

		facadeFor(character).setDamage(-5);

		assertEquals(0, character.getDisplay().getTacticalSession().damageTaken());
	}

	@Test
	public void recordsWhatIsSpentOfAResource()
	{
		PlayerCharacter character = getCharacter();

		facadeFor(character).spend("Mythic power", 4);

		assertEquals(4, character.getDisplay().getTacticalSession().spent("Mythic power"));
	}

	@Test
	public void spendingNothingForgetsTheResource()
	{
		PlayerCharacter character = getCharacter();
		TacticalSheetFacade facade = facadeFor(character);
		facade.spend("Mythic power", 4);

		facade.spend("Mythic power", 0);

		assertEquals(0, character.getDisplay().getTacticalSession().spent("Mythic power"));
	}

	@Test
	public void keepsDamageWhenAResourceIsSpent()
	{
		PlayerCharacter character = getCharacter();
		TacticalSheetFacade facade = facadeFor(character);
		facade.setDamage(19);

		facade.spend("Mythic power", 4);

		assertEquals(19, character.getDisplay().getTacticalSession().damageTaken());
		assertEquals(4, character.getDisplay().getTacticalSession().spent("Mythic power"));
	}
}
