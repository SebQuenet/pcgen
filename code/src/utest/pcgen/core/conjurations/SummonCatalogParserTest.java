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
import java.util.Set;

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
	void keepsADisplayNameWhileResolvingARaceAndLevel()
	{
		SummonTemplate summon = firstSummon("""
			## Convocation VI
			summon: Tigre sanguinaire céleste
			  race: Tiger (Dire)
			  template: Celestial Creature
			  level: VI
			  role: frappe, lutte
			  note: Bondit sur sa cible
			""");

		assertEquals("Tigre sanguinaire céleste", summon.name());
		assertEquals("Tiger (Dire)", summon.sourceRaceKey());
		assertEquals(List.of("Celestial Creature"), summon.templateKeys());
		assertEquals("VI", summon.metadata().get("level"));
		assertEquals(List.of("frappe", "lutte"), summon.roles());
	}

	@Test
	void preservesTheKitAndRulesSourceOfASelectedPublishedCreature()
	{
		SummonTemplate summon = firstSummon("""
			## Expanded Summon Monster VII
			summon: Young bronze dragon [Expanded VII]
			  race: Dragon (Bronze)
			  kit: Dragon (Bronze) Young
			  source: https://aonprd.com/FeatDisplay.aspx?ItemName=Expanded+Summon+Monster
			  level: VII
			""");

		assertEquals("Dragon (Bronze) Young", summon.metadata().get("kit"));
		assertEquals("https://aonprd.com/FeatDisplay.aspx?ItemName=Expanded+Summon+Monster",
			summon.metadata().get("source"));
	}

	@Test
	void addsTheTemplatesChosenWithTheFeatToThoseWrittenInTheCatalog()
	{
		Set<String> chosen = SummonSelections.versatileMonster("# versatile-sm: Aerial Creature",
			List.of("Fiery Creature", "Dark Creature"));

		assertEquals(Set.of("Aerial Creature", "Fiery Creature", "Dark Creature"), chosen);
	}

	@Test
	void ignoresAFeatChoiceThatIsNotAVersatileTemplate()
	{
		assertEquals(Set.of(), SummonSelections.versatileMonster("", List.of("Celestial Creature")));
	}

		@Test
	void readsPermanentVersatileMonsterChoicesAlongsidePersonalSummons()
	{
		String source = """
			# versatile-sm: Aerial Creature
			# versatile-sm: Fiery Creature
			## Favorites
			summon: Dog
			""";

		assertEquals(Set.of("Aerial Creature", "Fiery Creature"), SummonSelections.versatileMonster(source));
		assertEquals("Dog", parsed(source).groups().getFirst().summons().getFirst().name());
		assertTrue(parsed("# versatile-sm: Aerial Creature").groups().isEmpty());
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
	void readsAuthoredMetadataAlongsideRaceAndTemplates()
	{
		SummonTemplate summon = firstSummon("""
			## I
			summon: Dire Rat
			  template: Celestial Creature
			  role: tank, scout
			  hp: 51
			  note: Keep away from fire
			""");

		assertEquals("Dire Rat (Celestial Creature)", summon.name());
		assertEquals(List.of("tank", "scout"), summon.roles());
		assertEquals("51", summon.metadata().get("hp"));
		assertEquals("Keep away from fire", summon.metadata().get("note"));
	}

	@Test
	void readsThePreviousCatalogFormat()
	{
		SummonTemplate summon = firstSummon("""
			## VI
			summon: Lillend (azata)
			  role: lanceur, soutien, contrôle
			  speed: 9 m, vol 21 m
			  hp: 87
			  ac: 20
			  attack: épée longue
			  damage: 2d6+8
			  special: Sorts de barde
			  cmb: +15
			  cmd: 28
			""");

		assertEquals("Lillend (azata)", summon.name());
		assertEquals("87", summon.metadata().get("hp"));
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
