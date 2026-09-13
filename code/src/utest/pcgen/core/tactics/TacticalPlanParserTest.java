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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Behaviour of the tactical plan source syntax.
 */
class TacticalPlanParserTest
{

	private static TacticalSheet parsed(String source)
	{
		return switch (TacticalPlanParser.parse(source))
		{
			case TacticalParseSuccess success -> success.sheet();
			case TacticalParseFailure failure -> fail("Expected a parse, got errors: " + failure.errors());
		};
	}

	private static TacticalBlock firstBlock(String source)
	{
		return parsed(source).sections().getFirst().blocks().getFirst();
	}

	private static List<TacticalParseError> errorsOf(String source)
	{
		return switch (TacticalPlanParser.parse(source))
		{
			case TacticalParseSuccess success -> fail("Expected errors, got a sheet: " + success.sheet());
			case TacticalParseFailure failure -> failure.errors();
		};
	}

	private static void assertErrorOnLine(int expectedLine, String source)
	{
		List<TacticalParseError> errors = errorsOf(source);

		assertTrue(errors.stream().anyMatch(error -> error.line() == expectedLine),
			"Expected an error on line " + expectedLine + ", got " + errors);
	}

	@Test
	void parsesASectionHoldingOneStep()
	{
		String source = """
			## Round 1
			step: Enemy closes in
			  do: Swing the mace
			""";

		TacticalSheet sheet = parsed(source);

		assertEquals(List.of("Round 1"), sheet.sections().stream().map(TacticalSection::title).toList());
		assertEquals(new TacticalStep("Enemy closes in", "Swing the mace", ""),
			sheet.sections().getFirst().blocks().getFirst());
	}

	@Test
	void readsAStepNote()
	{
		String source = """
			## Traps
			step: Enemy targets touch AC
			  do: Touch AC is 14, so rays land
			  note: Total AC 25
			""";

		assertEquals(new TacticalStep("Enemy targets touch AC", "Touch AC is 14, so rays land", "Total AC 25"),
			firstBlock(source));
	}

	@Test
	void joinsANotesProseLines()
	{
		String source = """
			## Context
			note: Baseline
			  No buffs are up.
			  Weapon infusion is on the mace.
			""";

		assertEquals(new TacticalNote("Baseline", "No buffs are up.\nWeapon infusion is on the mace."),
			firstBlock(source));
	}

	@Test
	void readsAResourceWithAWrittenMaximum()
	{
		String source = """
			## Resources
			resource: Mythic power | 11 | immediate
			""";

		assertEquals(new TacticalResource("Mythic power", new TacticalLiteral("11"), "immediate"),
			firstBlock(source));
	}

	@Test
	void readsAResourceWhoseMaximumComesFromAVariable()
	{
		String source = """
			## Resources
			resource: Channel | @var(ChannelUses) | standard
			""";

		assertEquals(
			new TacticalResource("Channel", new TacticalReference(ReferenceKind.VAR, "ChannelUses"),
				"standard"),
			firstBlock(source));
	}

	@Test
	void readsAResourceWithoutAnAction()
	{
		String source = """
			## Resources
			resource: Mythic power | 11
			""";

		assertEquals("", ((TacticalResource) firstBlock(source)).action());
	}

	@Test
	void readsAnAttackThatBorrowsTheCharactersNumbers()
	{
		String source = """
			## Attacks
			attack: @weapon(Sceptre of Timeon)
			""";

		assertEquals(new TacticalAttack(new TacticalReference(ReferenceKind.WEAPON, "Sceptre of Timeon"),
			Optional.empty(), Optional.empty(), Optional.empty(), List.of(), ""), firstBlock(source));
	}

	@Test
	void readsAnAttackWrittenOutInFull()
	{
		String source = """
			## Attacks
			attack: Spear +1 | +13/+8 | 1d8+4 | 20/x3
			""";

		assertEquals(new TacticalAttack(new TacticalLiteral("Spear +1"), Optional.of("+13/+8"), Optional.of("1d8+4"),
			Optional.of("20/x3"), List.of(), ""), firstBlock(source));
	}

	@Test
	void readsAnAttacksTargetVariantsAndNote()
	{
		String source = """
			## Attacks
			attack: @weapon(Sceptre of Timeon)
			  target: evil creature | +2d6 holy
			  target: evil outsider | +2 enhancement, +2d6
			  note: only works in good hands
			""";

		TacticalAttack attack = (TacticalAttack) firstBlock(source);

		assertEquals(List.of(new TacticalVariant("evil creature", "+2d6 holy"),
			new TacticalVariant("evil outsider", "+2 enhancement, +2d6")), attack.variants());
		assertEquals("only works in good hands", attack.note());
	}

	@Test
	void readsACreatureStatBlock()
	{
		String source = """
			## Summons
			creature: Hound archon | Summon monster IV | 7 rounds
			  row: Def | AC 19 (touch 10, flat-footed 19)
			  row: DR/SR | DR 10/epic, SR 15
			""";

		assertEquals(new TacticalCreature("Hound archon", "Summon monster IV", "7 rounds",
			List.of(new TacticalRow("Def", "AC 19 (touch 10, flat-footed 19)"),
				new TacticalRow("DR/SR", "DR 10/epic, SR 15"))),
			firstBlock(source));
	}

	@Test
	void readsACreatureWithoutSourceOrDuration()
	{
		String source = """
			## Summons
			creature: Hound archon
			  row: Def | AC 19
			""";

		TacticalCreature creature = (TacticalCreature) firstBlock(source);

		assertEquals("", creature.source());
		assertEquals("", creature.duration());
	}

	@Test
	void readsATaggedSpellRepertoire()
	{
		String source = """
			## Spells
			spells: @prepared
			  tag: Holy smite | damage, good descriptor
			  tag: Blessing of fervour | buff
			""";

		assertEquals(new TacticalSpellList(SpellSource.PREPARED,
			List.of(new TacticalTag("Holy smite", List.of("damage", "good descriptor")),
				new TacticalTag("Blessing of fervour", List.of("buff")))),
			firstBlock(source));
	}

	@Test
	void readsAKnownSpellRepertoire()
	{
		String source = """
			## Spells
			spells: @known
			  tag: Magic missile | damage
			""";

		assertEquals(SpellSource.KNOWN, ((TacticalSpellList) firstBlock(source)).source());
	}

	@Test
	void keepsSeveralSectionsInReadingOrder()
	{
		String source = """
			## Before the fight
			resource: Mythic power | 11

			## Round 1
			step: Enemy closes in
			  do: Swing the mace

			## Emergency
			step: HP below 20
			  do: Withdraw
			""";

		assertEquals(List.of("Before the fight", "Round 1", "Emergency"),
			parsed(source).sections().stream().map(TacticalSection::title).toList());
	}

	@Test
	void refusesTextThatDoesNotOpenWithASection()
	{
		assertErrorOnLine(1, """
			step: Enemy closes in
			  do: Swing the mace
			""");
	}

	@Test
	void refusesAnUnknownBlockKey()
	{
		assertErrorOnLine(2, """
			## Round 1
			manoeuvre: Trip the ogre
			""");
	}

	@Test
	void refusesASubLineWithNoBlockAboveIt()
	{
		assertErrorOnLine(2, """
			## Round 1
			  do: Swing the mace
			""");
	}

	@Test
	void refusesASubLineThatDoesNotBelongToTheBlockAboveIt()
	{
		assertErrorOnLine(3, """
			## Summons
			creature: Hound archon
			  target: evil creature | +2d6
			""");
	}

	@Test
	void refusesAStepWithoutActions()
	{
		assertErrorOnLine(2, """
			## Round 1
			step: Enemy closes in
			""");
	}

	@Test
	void refusesAResourceWithoutAMaximum()
	{
		assertErrorOnLine(2, """
			## Resources
			resource: Mythic power
			""");
	}

	@Test
	void refusesACreatureWithoutARow()
	{
		assertErrorOnLine(2, """
			## Summons
			creature: Hound archon
			""");
	}

	@Test
	void refusesAWrittenAttackMissingItsNumbers()
	{
		assertErrorOnLine(2, """
			## Attacks
			attack: Spear +1 | +13/+8
			""");
	}

	@Test
	void refusesTwoResourcesSharingALabel()
	{
		assertErrorOnLine(3, """
			## Resources
			resource: Mythic power | 11
			resource: Mythic power | 5
			""");
	}

	@Test
	void refusesAMalformedReference()
	{
		assertErrorOnLine(2, """
			## Attacks
			attack: @weapon(Sceptre of Timeon
			""");
	}

	@Test
	void refusesAnUnknownSpellSource()
	{
		assertErrorOnLine(2, """
			## Spells
			spells: @wished-for
			""");
	}

	@Test
	void readsAnEscapedNewlineInAFieldAsALineBreak()
	{
		String source = """
			## Traps
			step: Ambush from behind
			  do: Withdraw
			  note: Two lines\\nof note
			""";

		assertEquals("Two lines\nof note", ((TacticalStep) firstBlock(source)).note());
	}

	@Test
	void reportsEveryErrorRatherThanStoppingAtTheFirst()
	{
		List<TacticalParseError> errors = errorsOf("""
			## Round 1
			manoeuvre: Trip the ogre
			gambit: Feint
			""");

		assertEquals(List.of(2, 3), errors.stream().map(TacticalParseError::line).toList());
	}
}
