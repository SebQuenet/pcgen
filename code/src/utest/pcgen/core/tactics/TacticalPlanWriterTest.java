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
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Behaviour of writing a tactical sheet back out as source text. The writer
 * exists to migrate characters saved under the old tags, so what it writes has
 * to read back identically.
 */
class TacticalPlanWriterTest
{

	private static void assertRoundTrips(TacticalSheet sheet)
	{
		String source = TacticalPlanWriter.write(sheet);

		switch (TacticalPlanParser.parse(source))
		{
			case TacticalParseSuccess success -> assertEquals(sheet, success.sheet(), "Written as:\n" + source);
			case TacticalParseFailure failure ->
				fail("Written source did not read back: " + failure.errors() + "\n" + source);
		}
	}

	private static TacticalSheet sheetOf(TacticalBlock... blocks)
	{
		return new TacticalSheet(List.of(new TacticalSection("Opening", List.of(blocks))));
	}

	@Test
	void roundTripsAStep()
	{
		assertRoundTrips(sheetOf(new TacticalStep("Round 1", "Cast bless, then advance", "")));
	}

	@Test
	void roundTripsAStepWithANote()
	{
		assertRoundTrips(sheetOf(new TacticalStep("HP below 12", "Withdraw", "Provokes")));
	}

	@Test
	void roundTripsANote()
	{
		assertRoundTrips(sheetOf(new TacticalNote("Baseline", "No buffs are up.\nInfusion is on the mace.")));
	}

	@Test
	void roundTripsAResource()
	{
		assertRoundTrips(sheetOf(new TacticalResource("Mythic power", new TacticalLiteral("11"), "immediate")));
	}

	@Test
	void roundTripsAResourceWithoutAnAction()
	{
		assertRoundTrips(sheetOf(new TacticalResource("Channel",
			new TacticalReference(ReferenceKind.VAR, "ChannelUses"), "")));
	}

	@Test
	void roundTripsAReferencedAttackWithVariants()
	{
		assertRoundTrips(sheetOf(new TacticalAttack(new TacticalReference(ReferenceKind.WEAPON, "Sceptre of Timeon"),
			Optional.empty(), Optional.empty(), Optional.empty(),
			List.of(new TacticalVariant("evil creature", "+2d6 holy")), "only in good hands")));
	}

	@Test
	void roundTripsAWrittenAttack()
	{
		assertRoundTrips(sheetOf(new TacticalAttack(new TacticalLiteral("Spear +1"), Optional.of("+13/+8"),
			Optional.of("1d8+4"), Optional.of("20/x3"), List.of(), "")));
	}

	@Test
	void roundTripsACreature()
	{
		assertRoundTrips(sheetOf(new TacticalCreature("Hound archon", "Summon monster IV", "7 rounds",
			List.of(new TacticalRow("Def", "AC 19"), new TacticalRow("Att", "greatsword +11/+6")))));
	}

	@Test
	void roundTripsACreatureWithoutSourceOrDuration()
	{
		assertRoundTrips(
			sheetOf(new TacticalCreature("Hound archon", "", "", List.of(new TacticalRow("Def", "AC 19")))));
	}

	@Test
	void roundTripsASpellRepertoire()
	{
		assertRoundTrips(sheetOf(new TacticalSpellList(SpellSource.PREPARED,
			List.of(new TacticalTag("Holy smite", List.of("damage", "good descriptor"))))));
	}

	@Test
	void roundTripsAStepNoteHoldingALineBreak()
	{
		assertRoundTrips(sheetOf(new TacticalStep("HP below 12", "Withdraw", "Two lines\nof note")));
	}

	@Test
	void roundTripsSeveralSections()
	{
		assertRoundTrips(new TacticalSheet(List.of(
			new TacticalSection("Opening", List.of(new TacticalStep("Round 1", "Cast bless", ""))),
			new TacticalSection("Emergency", List.of(new TacticalStep("HP below 12", "Withdraw", ""))))));
	}
}
