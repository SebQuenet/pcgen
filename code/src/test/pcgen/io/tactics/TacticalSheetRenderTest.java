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
package pcgen.io.tactics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import pcgen.AbstractCharacterTestCase;
import pcgen.core.PlayerCharacter;
import pcgen.io.ExportException;
import pcgen.io.ExportHandler;
import pcgen.system.LanguageBundle;

import org.junit.jupiter.api.Test;

/**
 * The tactical sheet template, run through the real export handler against a
 * real character.
 */
public class TacticalSheetRenderTest extends AbstractCharacterTestCase
{

	private static final File TEMPLATE = new File("outputsheets/tactics/tactical.htm.ftl");

	private String render() throws IOException, ExportException
	{
		StringWriter written = new StringWriter();
		try (BufferedWriter out = new BufferedWriter(written))
		{
			ExportHandler.createExportHandler(TEMPLATE).write(getCharacter(), out);
		}
		return written.toString();
	}

	@Test
	public void saysSoWhenTheCharacterHasNoPlan() throws IOException, ExportException
	{
		assertTrue(render().contains(LanguageBundle.getString("in_tactical_empty")));
	}

	@Test
	public void rendersEverySectionTitleAndStep() throws IOException, ExportException
	{
		getCharacter().setTacticalPlan("""
			## Before the fight
			step: Enemy at range
			  do: Close the distance
			  note: Provokes

			## Emergency
			step: HP below 12
			  do: Withdraw
			""");

		String html = render();

		assertTrue(html.contains("Before the fight"), html);
		assertTrue(html.contains("Emergency"), html);
		assertTrue(html.contains("Close the distance"), html);
		assertTrue(html.contains("Provokes"), html);
	}

	@Test
	public void escapesMarkupWrittenIntoThePlan() throws IOException, ExportException
	{
		getCharacter().setTacticalPlan("""
			## Before the fight
			step: Ambushed
			  do: <script>window.pcgen.spend('x', 9)</script>
			""");

		String html = render();

		assertFalse(html.contains("<script>window.pcgen"), "Authored markup reached the page unescaped");
		assertTrue(html.contains("&lt;script&gt;"), html);
	}

	@Test
	public void showsTheOffendingLineWhenThePlanDoesNotRead() throws IOException, ExportException
	{
		getCharacter().setTacticalPlan("""
			## Before the fight
			manoeuvre: Trip the ogre
			""");

		String html = render();

		assertTrue(html.contains(LanguageBundle.getString("in_tactical_does_not_read")), html);
		assertTrue(html.contains(LanguageBundle.getFormattedString("in_tactical_error", 2, "").strip()), html);
	}

	@Test
	public void warnsAboutAWeaponTheCharacterDoesNotCarry() throws IOException, ExportException
	{
		getCharacter().setTacticalPlan("""
			## Attacks
			attack: @weapon(Sceptre of Timeon)
			""");

		String html = render();

		String expected = LanguageBundle.getFormattedString("in_tactical_no_weapon", "'Sceptre of Timeon'");

		assertTrue(html.contains(expected) || html.contains(expected.replace("'", "&#39;")), html);
	}

	@Test
	public void offersAFilterForEveryTagACapabilityListDeclares() throws IOException, ExportException
	{
		getCharacter().setTacticalPlan("""
			## Repertoire
			capabilities: Everything
			  power: Touch of Good | buff, support | standard | 10/day | +3 sacred
			  power: Holy smite | damage
			""");

		String html = render();

		assertTrue(html.contains("data-tags=\"buff,damage,support\""), html);
		assertTrue(html.contains("Touch of Good"), html);
		assertTrue(html.contains("+3 sacred"), html);
	}

	@Test
	public void saysWhichSpellTagsMatchNothingTheCharacterHolds() throws IOException, ExportException
	{
		getCharacter().setTacticalPlan("""
			## Spells
			spells: @prepared
			  tag: Chatiment sacre | damage
			""");

		String html = render();

		assertTrue(html.contains(LanguageBundle.getString("in_tactical_unmatched_tags")), html);
		assertTrue(html.contains("Chatiment sacre"), html);
	}

	@Test
	public void rendersABuffAsASwitchCarryingItsDeltas() throws IOException, ExportException
	{
		getCharacter().setTacticalPlan("""
			## Buffs
			buff: Aura of holiness | 15 rounds
			  gives: ac +4 | will +4
			""");

		String html = render();

		assertTrue(html.contains("data-buff=\"Aura of holiness\""), html);
		assertTrue(html.contains("data-deltas=\"ac 4,will 4\""), html);
	}

	@Test
	public void leavesABuffInertWhenPCGenHoldsNoSuchTemporaryBonus() throws IOException, ExportException
	{
		getCharacter().setTacticalPlan("""
			## Buffs
			buff: Divine favour | 1 min
			  applies: @tempbonus(Divine Favor)
			""");

		String html = render();

		assertTrue(html.contains("disabled"), html);
		assertTrue(html.contains(LanguageBundle.getFormattedString("in_tactical_no_bonus", "'Divine Favor'"))
			|| html.contains(LanguageBundle.getFormattedString("in_tactical_no_bonus", "&#39;Divine Favor&#39;")),
			html);
	}

	@Test
	public void givesEachAttackCellItsRestingValueSoABuffCanAddToIt() throws IOException, ExportException
	{
		getCharacter().setTacticalPlan("""
			## Attacks
			attack: Spear +1 | +13/+8 | 1d8+4 | 20/x3
			""");

		String html = render();

		assertTrue(html.contains("data-base=\"+13/+8\""), html);
		assertTrue(html.contains("data-base=\"1d8+4\""), html);
	}

	@Test
	public void rendersAnItemCard() throws IOException, ExportException
	{
		getCharacter().setTacticalPlan("""
			## Gear
			item: Sceptre of Timeon
			  row: Aura | moderate evocation, CL 10
			""");

		String html = render();

		assertTrue(html.contains("Sceptre of Timeon"), html);
		assertTrue(html.contains("moderate evocation, CL 10"), html);
	}

	@Test
	public void countsOutThePipsOfAResource() throws IOException, ExportException
	{
		PlayerCharacter character = getCharacter();
		character.setTacticalPlan("""
			## Resources
			resource: Mythic power | 11 | immediate
			""");

		String html = render();

		assertTrue(html.contains("Mythic power"), html);
		assertTrue(html.contains("data-pip=\"11\""), html);
	}
}
