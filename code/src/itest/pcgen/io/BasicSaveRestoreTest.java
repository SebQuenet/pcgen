/*
 * Copyright (c) 2013 Tom Parker <thpr@users.sourceforge.net>
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package pcgen.io;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import pcgen.cdom.util.CControl;
import pcgen.core.Deity;
import pcgen.core.GameMode;
import pcgen.core.NoteItem;
import pcgen.core.PCClass;
import pcgen.core.PCTemplate;
import pcgen.core.SettingsHandler;
import pcgen.core.Skill;
import pcgen.core.tactics.TacticalSessionState;
import pcgen.core.tactics.TacticalStep;
import pcgen.core.tactics.TacticalSection;
import pcgen.core.tactics.TacticalSheet;
import pcgen.io.testsupport.AbstractSaveRestoreTest;
import pcgen.output.channel.ChannelUtilities;
import pcgen.output.channel.compat.AlignmentCompat;

import plugin.lsttokens.pcclass.HdToken;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;


public class BasicSaveRestoreTest extends AbstractSaveRestoreTest
{

	@Test
	public void testBoilerplateOnly()
	{
		finishLoad();
		runRoundRobin(null);
	}

	@Test
	public void testDeity()
	{
		Deity deity = create(Deity.class, "MyDeity");
		finishLoad();
		ChannelUtilities.setControlledChannel(id, CControl.DEITYINPUT, deity);
		runRoundRobin(null);
	}

	//TODO No way to remove a deity once set :(
	//	@Test
	//	public void testDeityAddRemove()
	//	{
	//		Deity deity = create(Deity.class, "MyDeity");
	//		finishLoad();
	//		runWriteRead();
	//		pc.setDeity(deity);
	//		pc.removeDeity();
	//		testEquality();
	//	}

	@Test
	public void testAlignment()
	{
		finishLoad();
		AlignmentCompat.setCurrentAlignment(pc.getCharID(), le);
		runRoundRobin(null);
	}

	//TODO No way to remove a alignment once set :(
	//	@Test
	//	public void testAlignmentAddRemove()
	//	{
	//		finishLoad();
	//		runWriteRead();
	//		pc.setAlignment(le);
	//		pc.removeAlignment();
	//		testEquality();
	//	}

	@Test
	public void testTemplate()
	{
		PCTemplate template = create(PCTemplate.class, "MyTemplate");
		finishLoad();
		pc.addTemplate(template);
		runRoundRobin(null);
	}

	@Test
	public void testTemplateAddRemove()
	{
		PCTemplate template = create(PCTemplate.class, "MyTemplate");
		finishLoad();
		runWriteRead(false);
		pc.addTemplate(template);
		pc.removeTemplate(template);
		checkEquality();
	}

	@Test
	public void testSpellBook()
	{
		finishLoad();
		pc.addSpellBook("MyBook");
		runRoundRobin(null);
	}

	@Test
	public void testSpellBookAddRemove()
	{
		finishLoad();
		runWriteRead(false);
		pc.addSpellBook("MyBook");
		pc.delSpellBook("MyBook");
		checkEquality();
	}

	@Test
	public void testCharacterType()
	{
		final GameMode gameMode = SettingsHandler.getGameAsProperty().get();
		gameMode.setCharacterTypeList(Arrays.asList("Default",
				"MyType"));
		finishLoad();
		ChannelUtilities.setControlledChannel(pc.getCharID(),
			CControl.CHARACTERTYPE, "MyType");
		runRoundRobin(null);
	}

	@Test
	public void testNotes()
	{
		finishLoad();
		pc.addNotesItem(new NoteItem(1, -1, "NoteName", "NoteValue"));
		runRoundRobin(null);
	}

	@Test
	public void testNotesAddRemove()
	{
		finishLoad();
		NoteItem item = new NoteItem(1, -1, "NoteName", "NoteValue");
		pc.addNotesItem(item);
		pc.removeNote(item);
		runRoundRobin(null);
	}

	@Test
	public void testTacticalSheet()
	{
		finishLoad();
		pc.setTacticalSheet(new TacticalSheet(List.of(
			new TacticalSection("Opening",
				List.of(new TacticalStep("Round 1", "Cast bless, then advance", "Provokes"))),
			new TacticalSection("Emergency",
				List.of(new TacticalStep("HP below 12", "Drink a potion, withdraw", ""),
					new TacticalStep("Outnumbered", "Fall back to the corridor", ""))))));
		runRoundRobin(null);
	}

	@Test
	public void testTacticalSheetSurvivesSeparatorCharacters()
	{
		finishLoad();
		pc.setTacticalSheet(new TacticalSheet(List.of(new TacticalSection("Rounds 1|2",
			List.of(new TacticalStep("HP: below 12", "Cast [bless] & withdraw", "Two lines\nof note"))))));
		runRoundRobin(null);
	}

	@Test
	public void testTacticalSheetAddRemove()
	{
		finishLoad();
		pc.setTacticalSheet(new TacticalSheet(
			List.of(new TacticalSection("Opening", List.of(new TacticalStep("Round 1", "Charge", ""))))));
		pc.clearTacticalSheet();
		runRoundRobin(null);
	}

	@Test
	public void testTacticalPlanHoldingEveryKindOfBlock()
	{
		finishLoad();
		pc.setTacticalPlan("""
			## Before the fight
			note: Baseline
			  No buffs are up.
			  Infusion is on the mace.
			resource: Mythic power | 11 | immediate
			resource: Channel | @var(ChannelUses)
			attack: @weapon(Sceptre of Timeon)
			  target: evil outsider | +2 enhancement, +2d6
			  note: only works in good hands
			attack: Spear +1 | +13/+8 | 1d8+4 | 20/x3

			## Summons
			creature: Hound archon | Summon monster IV | 7 rounds
			  row: Def | AC 19 (touch 10, flat-footed 19)

			## Spells
			spells: @prepared
			  tag: Holy smite | damage, good descriptor

			## Emergency
			step: HP below 12
			  do: Withdraw
			  note: Provokes
			""");
		runRoundRobin(null);
	}

	@Test
	public void testTacticalSessionSurvivesASave()
	{
		finishLoad();
		pc.setTacticalPlan("""
			## Before the fight
			resource: Mythic power | 11
			""");
		pc.setTacticalSession(new TacticalSessionState(19, Map.of("Mythic power", 4, "Channel", 2)));
		runRoundRobin(null);
	}

	@Test
	public void testTacticalSessionIsForgottenWithThePlan()
	{
		finishLoad();
		pc.setTacticalPlan("""
			## Before the fight
			resource: Mythic power | 11
			""");
		pc.setTacticalSession(new TacticalSessionState(19, Map.of("Mythic power", 4)));
		pc.clearTacticalSheet();
		runRoundRobin(null);
	}

	@Test
	public void testTacticalSheetSavedUnderTheOldTagsBecomesSourceText()
	{
		finishLoad();
		String legacy = new PCGVer2Creator(pc, SettingsHandler.getGameAsProperty().get(), null).createPCGString()
			+ "TACTICALSECTION:Opening" + IOConstants.LINE_SEP
			+ "TACTICALENTRY:0|TACTICALTRIGGER:Round 1|TACTICALACTIONS:Cast bless|TACTICALNOTE:Provokes"
			+ IOConstants.LINE_SEP;

		PCGIOHandler handler = new PCGIOHandler();
		handler.read(reloadedPC, new ByteArrayInputStream(legacy.getBytes(StandardCharsets.UTF_8)), true);

		assertEquals(List.of(), handler.getErrors());
		assertEquals("""
			## Opening
			step: Round 1
			  do: Cast bless
			  note: Provokes
			""", reloadedPC.getDisplay().getTacticalPlan().orElseThrow());
	}

	@Test
	public void testSkill()
	{
		create(Skill.class, "MySkill");
		finishLoad();
		runRoundRobin(null);
	}

	//TODO need to test different types of skills:
	/*
	 * Ranked CLASS CROSS_CLASS EXCLUSIVE (had and not) QUALIFIED (PRE) NOT
	 * QUALIFIED (!PRE)
	 */

	@Test
	public void testStatRank()
	{
		finishLoad();
		pc.setStat(str, 14);
		pc.setStat(intel, 15);
		pc.setStat(wis, 16);
		pc.setStat(dex, 17);
		pc.setStat(con, 18);
		pc.setStat(cha, 19);
		runRoundRobin(null);
	}

	@Test
	public void testClass()
	{
		PCClass cl = create(PCClass.class, "MyClass");
		new HdToken().parseToken(context, cl, "6");
		finishLoad();
		pc.addClass(cl);
		pc.incrementClassLevel(1, cl);
		pc.setHP(pc.getActiveClassLevel(cl, 0), 4);
		runWriteRead(false);
		//TODO need this to create the spell support :/
		reloadedPC.getSpellSupport(cl);
		checkEquality();
	}

}
