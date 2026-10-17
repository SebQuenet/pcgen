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

import org.junit.jupiter.api.Test;

/**
 * Reading the text of a catalog of conjurations: groups, a race per summon and its templates.
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
	void readsARaceWithoutTemplate()
	{
		SummonCatalog catalog = parsed("## Summon Monster IV\nsummon: Archon (Hound)\n");

		assertEquals(new SummonCatalog(List.of(new SummonGroup("Summon Monster IV",
			List.of(new SummonTemplate("Archon (Hound)", List.of(), 2))))), catalog);
	}

	@Test
	void namesASummonWithoutTemplateAfterItsRace()
	{
		assertEquals("Archon (Hound)", firstSummon("## IV\nsummon: Archon (Hound)\n").name());
	}

	@Test
	void readsTemplatesSeparatedByCommas()
	{
		SummonTemplate eagle =
			firstSummon("## I\nsummon: Eagle\n  template: Celestial Creature, Augment Summoning\n");

		assertEquals(List.of("Celestial Creature", "Augment Summoning"), eagle.templateKeys());
	}

	@Test
	void namesASummonAfterItsRaceAndTemplates()
	{
		SummonTemplate eagle =
			firstSummon("## I\nsummon: Eagle\n  template: Celestial Creature, Augment Summoning\n");

		assertEquals("Eagle (Celestial Creature, Augment Summoning)", eagle.name());
	}

	@Test
	void sameRaceWithDifferentTemplatesAreTwoEntries()
	{
		SummonCatalog catalog = parsed("""
			## Summon Monster I
			summon: Eagle
			  template: Celestial Creature
			summon: Eagle
			  template: Fiendish Creature
			""");

		assertEquals(2, catalog.groups().getFirst().summons().size());
	}

	@Test
	void keepsGroupsAndSummonsInReadingOrderAcrossBlankLines()
	{
		SummonCatalog catalog = parsed("""
			## Summon Monster I

			summon: Dire Rat

			## Summon Monster II
			summon: Eagle
			  template: Celestial Creature
			""");

		assertEquals(List.of("Dire Rat", "Eagle (Celestial Creature)"), catalog.groups().stream()
			.flatMap(group -> group.summons().stream()).map(SummonTemplate::name).toList());
	}

	@Test
	void findsASummonByNameWhateverItsCase()
	{
		SummonCatalog catalog = parsed("## I\nsummon: Eagle\n  template: Celestial Creature\n");

		assertEquals("Eagle", catalog.find("eagle (celestial creature)").orElseThrow().raceKey());
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
	void refusesAnOldField()
	{
		List<SummonParseError> errors = errorsOf("## I\nsummon: Dire Rat\n  hp: 51\n");

		assertTrue(errors.stream().anyMatch(error -> error.line() == 3 && error.message().contains("'hp'")),
			"Expected an error naming 'hp' on line 3, got " + errors);
	}

	@Test
	void refusesAnIndentedLineWithNoSummonAboveIt()
	{
		assertErrorOnLine(2, "## I\n  template: Celestial Creature\nsummon: Dire Rat\n");
	}

	@Test
	void refusesTemplatesWrittenTwiceForTheSameSummon()
	{
		assertErrorOnLine(4, "## I\nsummon: Eagle\n  template: Celestial Creature\n  template: Fiendish Creature\n");
	}

	@Test
	void refusesASummonWithNoRace()
	{
		assertErrorOnLine(2, "## I\nsummon:\n");
	}

	@Test
	void refusesTheSameNameTwice()
	{
		assertErrorOnLine(4, "## I\nsummon: Eagle\n## II\nsummon: eagle\n");
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
