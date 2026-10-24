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
package pcgen.gui2.gametable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import pcgen.AbstractCharacterTestCase;
import pcgen.core.Equipment;
import pcgen.core.Globals;
import pcgen.io.tactics.TacticalResolver;
import pcgen.system.LanguageBundle;
import pcgen.util.TestHelper;

import org.junit.jupiter.api.Test;

/**
 * The tactical sheet of a monster as the combat pane shows it beside its stat block.
 */
public class TacticsHtmlTest extends AbstractCharacterTestCase
{

	@Test
	public void showsNothingForAMonsterWithoutAPlan()
	{
		assertEquals("", TacticsHtml.of(getCharacter()));
	}

	@Test
	public void showsEverySectionAndWhatEachStepSaysToDo()
	{
		getCharacter().setTacticalPlan("""
			## Préparation
			step: Alarme donnée
			  do: Monter sur le parapet
			  note: Abri important

			## Round 1
			step: Initiative
			  do: Tir rapide sur le lanceur de sorts
			""");

		String html = TacticsHtml.of(getCharacter());

		assertTrue(html.contains("PRÉPARATION") && html.contains("ROUND 1") && html.contains("Alarme donnée")
			&& html.contains("Monter sur le parapet") && html.contains("Abri important")
			&& html.contains("Tir rapide sur le lanceur de sorts"), html);
	}

	@Test
	public void readsTheNumbersOfAWeaponOffTheMonster() throws Exception
	{
		TestHelper.makeEquipment("Test Mace\tTYPE:Weapon.Melee.Simple\tWT:5\tCOST:5\tDAMAGE:1d8\tCRITRANGE:1"
			+ "\tCRITMULT:x2\tPROFICIENCY:WEAPON|Test Mace\tSIZE:M");
		Equipment mace = Globals.getContext().getReferenceContext()
			.silentlyGetConstructedCDOMObject(Equipment.class, "Test Mace");
		getCharacter().addEquipment(mace);
		getCharacter().setCalcEquipmentList();
		getCharacter().setTacticalPlan("""
			## Attaques
			attack: @weapon(Test Mace)
			""");
		String toHit = new TacticalResolver(getCharacter()).weapon("Test Mace").orElseThrow().toHit();

		String html = TacticsHtml.of(getCharacter());

		assertTrue(html.contains(toHit) && html.contains("1d8"), html);
	}

	@Test
	public void namesTheLineOfAPlanThatDoesNotRead()
	{
		getCharacter().setTacticalPlan("""
			## Round 1
			manoeuvre: Croc-en-jambe
			""");

		String html = TacticsHtml.of(getCharacter());

		assertTrue(html.contains(LanguageBundle.getFormattedString("in_tactical_error", 2, "").strip()), html);
	}

	@Test
	public void escapesMarkupWrittenIntoThePlan()
	{
		getCharacter().setTacticalPlan("""
			## Round 1
			step: Embuscade
			  do: <b>Frapper</b>
			""");

		String html = TacticsHtml.of(getCharacter());

		assertFalse(html.contains("<b>Frapper</b>"), html);
	}
}
