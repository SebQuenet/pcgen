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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Behaviour of the conjuration catalog source syntax.
 */
class SummonCatalogParserTest
{

	private static SummonCatalog parsed(String source)
	{
		return switch (SummonCatalogParser.parse(source))
		{
			case SummonParseSuccess success -> success.catalog();
			case SummonParseFailure failure -> fail("Expected a catalog, got errors: " + failure.errors());
		};
	}

	private static SummonTemplate firstSummon(String source)
	{
		return parsed(source).groups().getFirst().summons().getFirst();
	}

	private static List<SummonParseError> errorsOf(String source)
	{
		return switch (SummonCatalogParser.parse(source))
		{
			case SummonParseSuccess success -> fail("Expected errors, got a catalog: " + success.catalog());
			case SummonParseFailure failure -> failure.errors();
		};
	}

	private static void assertErrorOnLine(int expectedLine, String source)
	{
		List<SummonParseError> errors = errorsOf(source);

		assertTrue(errors.stream().anyMatch(error -> error.line() == expectedLine),
			"Expected an error on line " + expectedLine + ", got " + errors);
	}

	@Test
	void readsAGroupHoldingOneSummon()
	{
		SummonCatalog catalog = parsed("""
			## Summon Monster I
			summon: Dire Rat
			  hp: 8
			""");

		assertEquals(new SummonCatalog(List.of(new SummonGroup("Summon Monster I",
			List.of(new SummonTemplate("Dire Rat", List.of(), Map.of(SummonField.HIT_POINTS, "8")))))), catalog);
	}

	@Test
	void readsEveryFieldTheSpreadsheetHolds()
	{
		SummonTemplate archon = firstSummon("""
			## Summon Monster IV
			summon: Hound Archon
			  speed: 8
			  hp: 51
			  ac: 19 / 10 / 19
			  attack: +11/+6 sword
			  damage: 2d6+6
			  special: beast shape II
			  cmb: 10
			  cmd: 20
			  ref: 5
			  fort: 8
			  will: 5 / +2 evil
			  init: 4
			  spells: greater teleport
			  note: SR 15
			""");

		assertEquals(Map.ofEntries(Map.entry(SummonField.SPEED, "8"), Map.entry(SummonField.HIT_POINTS, "51"),
			Map.entry(SummonField.ARMOR_CLASS, "19 / 10 / 19"), Map.entry(SummonField.ATTACK, "+11/+6 sword"),
			Map.entry(SummonField.DAMAGE, "2d6+6"), Map.entry(SummonField.SPECIAL, "beast shape II"),
			Map.entry(SummonField.CMB, "10"), Map.entry(SummonField.CMD, "20"), Map.entry(SummonField.REFLEX, "5"),
			Map.entry(SummonField.FORTITUDE, "8"), Map.entry(SummonField.WILL, "5 / +2 evil"),
			Map.entry(SummonField.INITIATIVE, "4"), Map.entry(SummonField.SPELLS, "greater teleport"),
			Map.entry(SummonField.NOTE, "SR 15")), archon.fields());
	}

	@Test
	void readsRolesSeparatedByCommas()
	{
		SummonTemplate archon = firstSummon("""
			## Summon Monster IV
			summon: Hound Archon
			  role: Tank, Buff ,Debuff
			""");

		assertEquals(List.of("Tank", "Buff", "Debuff"), archon.roles());
	}

	@Test
	void turnsAnEscapedNewlineIntoALineBreak()
	{
		SummonTemplate archon = firstSummon("""
			## Summon Monster IV
			summon: Hound Archon
			  attack: +11/+6 sword\\n+5 bite
			""");

		assertEquals("+11/+6 sword\n+5 bite", archon.field(SummonField.ATTACK));
	}

	@Test
	void keepsAColonInsideAValue()
	{
		SummonTemplate vulpinal = firstSummon("""
			## Summon Monster V
			summon: Agathion, Vulpinal
			  spells: 3/day: charm monster
			""");

		assertEquals("3/day: charm monster", vulpinal.field(SummonField.SPELLS));
	}

	@Test
	void reportsAnEmptyStringForAFieldNotWritten()
	{
		assertEquals("", firstSummon("## I\nsummon: Dire Rat\n").field(SummonField.SPELLS));
	}

	@Test
	void keepsGroupsAndSummonsInReadingOrderAcrossBlankLines()
	{
		SummonCatalog catalog = parsed("""
			## Summon Monster I

			summon: Dire Rat

			summon: Poisonous Frog

			## Summon Monster II
			summon: Small Earth Elemental
			""");

		assertEquals(List.of(List.of("Dire Rat", "Poisonous Frog"), List.of("Small Earth Elemental")),
			catalog.groups().stream()
				.map(group -> group.summons().stream().map(SummonTemplate::name).toList()).toList());
	}

	@Test
	void takesTheFirstWholeNumberOfItsHitPointsAsTheMaximum()
	{
		SummonTemplate vulpinal = firstSummon("## V\nsummon: Vulpinal\n  hp: 73 (6 HD)\n");

		assertEquals(73, vulpinal.maximumHitPoints().orElseThrow());
	}

	@Test
	void hasNoMaximumWhenItsHitPointsHoldNoWholeNumber()
	{
		assertTrue(firstSummon("## V\nsummon: Vulpinal\n  hp: plenty\n").maximumHitPoints().isEmpty());
	}

	@Test
	void findsASummonByNameWhateverItsCase()
	{
		SummonCatalog catalog = parsed("## IV\nsummon: Hound Archon\n");

		assertEquals("Hound Archon", catalog.find("hound archon").orElseThrow().name());
	}

	@Test
	void listsEveryRoleOnceInTheOrderItFirstAppears()
	{
		SummonCatalog catalog = parsed("""
			## III
			summon: Crocodile
			  role: Attacker, Controller
			## IV
			summon: Air Elemental
			  role: controller
			summon: Hound Archon
			  role: Tank, attacker
			""");

		assertEquals(List.of("Attacker", "Controller", "Tank"), catalog.roles());
	}

	@Test
	void refusesTextThatDoesNotOpenWithAGroup()
	{
		assertErrorOnLine(1, "summon: Dire Rat\n");
	}

	@Test
	void refusesAnUnknownBlock()
	{
		assertErrorOnLine(2, "## I\nmonster: Dire Rat\n");
	}

	@Test
	void refusesAnUnknownField()
	{
		assertErrorOnLine(3, "## I\nsummon: Dire Rat\n  size: small\n");
	}

	@Test
	void refusesAnIndentedLineWithNoSummonAboveIt()
	{
		assertErrorOnLine(2, "## I\n  hp: 8\nsummon: Dire Rat\n");
	}

	@Test
	void refusesAFieldWrittenTwiceForTheSameSummon()
	{
		assertErrorOnLine(4, "## I\nsummon: Dire Rat\n  hp: 8\n  hp: 9\n");
	}

	@Test
	void refusesASummonWithNoName()
	{
		assertErrorOnLine(2, "## I\nsummon:\n  hp: 8\n");
	}

	@Test
	void refusesANameAlreadyTakenWhateverItsCase()
	{
		assertErrorOnLine(4, "## I\nsummon: Dire Rat\n## II\nsummon: dire rat\n");
	}

	@Test
	void refusesAGroupHoldingNoSummon()
	{
		assertErrorOnLine(1, "## I\n## II\nsummon: Dire Rat\n");
	}

	@Test
	void refusesTextHoldingNoGroup()
	{
		assertErrorOnLine(1, "\n");
	}

	@Test
	void reportsEveryErrorRatherThanTheFirstOnly()
	{
		List<SummonParseError> errors = errorsOf("## I\nsummon: Dire Rat\n  size: small\n  colour: grey\n");

		assertEquals(List.of(3, 4), errors.stream().map(SummonParseError::line).toList());
	}
}
