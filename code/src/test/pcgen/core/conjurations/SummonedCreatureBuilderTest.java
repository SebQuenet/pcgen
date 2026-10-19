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

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import pcgen.core.Globals;
import pcgen.core.PlayerCharacter;
import pcgen.session.HeadlessUIDelegate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * A conjured creature built from its race in the Pathfinder Bestiary, with the
 * numbers the Bestiary prints for it.
 */
class SummonedCreatureBuilderTest
{
	private static final SummonTemplate HOUND_ARCHON = new SummonTemplate("Archon (Hound)", List.of(), 1);

	private static SummonedCreatureBuilder builder;

	@BeforeAll
	static void loadTheBestiary()
	{
		builder = new SummonedCreatureBuilder(BestiarySources.campaigns(), new HeadlessUIDelegate());
	}

	private static CreatureStats statsOf(SummonTemplate summon, boolean augmented)
	{
		return builder.statsOf(builder.build(summon, augmented));
	}

	@Test
	void aHoundArchonHasItsBestiaryArmorClass()
	{
		CreatureStats archon = statsOf(HOUND_ARCHON, false);

		assertEquals(List.of(19, 10, 19),
			List.of(archon.armorClass(), archon.touchArmorClass(), archon.flatFootedArmorClass()));
	}

	@Test
	void aHoundArchonSwingsItsGreatsword()
	{
		CreatureAttack greatsword = statsOf(HOUND_ARCHON, false).attacks().getFirst();

		assertEquals(List.of("+9/+4", "2d6+3"), List.of(greatsword.toHit(), greatsword.damage()));
	}

	@Test
	void aHoundArchonHasItsBestiaryHitPoints()
	{
		assertEquals(39, statsOf(HOUND_ARCHON, false).maximumHitPoints());
	}

	@Test
	void augmentSummoningRaisesItsAttack()
	{
		CreatureAttack greatsword = statsOf(HOUND_ARCHON, true).attacks().getFirst();

		assertEquals(List.of("+11/+6", "2d6+6"), List.of(greatsword.toHit(), greatsword.damage()));
	}

	@Test
	void augmentSummoningRaisesItsHitPoints()
	{
		assertEquals(51, statsOf(HOUND_ARCHON, true).maximumHitPoints());
	}

	@Test
	void catalogMetricsUseTheBuiltCreaturesHitDiceAndAugmentedAttacks()
	{
		CreatureStats ordinary = statsOf(HOUND_ARCHON, false);
		CreatureStats augmented = statsOf(HOUND_ARCHON, true);

		assertEquals(6, ordinary.hitDice());
		assertTrue(AverageDamage.expectedAgainst(augmented.attacks(), 20).orElseThrow()
			> AverageDamage.expectedAgainst(ordinary.attacks(), 20).orElseThrow());
	}

	@Test
	void aCelestialEagleCarriesItsTemplate()
	{
		PlayerCharacter eagle = builder.build(new SummonTemplate("Eagle", List.of("Celestial Creature"), 1), false);

		assertTrue(eagle.getDisplay().getTemplateSet().stream()
			.anyMatch(template -> "Celestial Creature".equals(template.getKeyName())));
	}

	@Test
	void buildsATranslatedCatalogEntryFromItsSourceRace()
	{
		SummonTemplate summon = SummonCatalogParser.catalogOf("""
			## IV
			summon: Archonte molosse
			  race: Archon (Hound)
			""").orElseThrow().groups().getFirst().summons().getFirst();

		assertTrue(CatalogResolution.unresolvedIn(summon).isEmpty());
		assertEquals(6, statsOf(summon, true).hitDice());
	}

	@Test
	void buildingLeavesNoCharacterBehind()
	{
		int before = Globals.getPCList().size();

		builder.build(HOUND_ARCHON, true);

		assertEquals(before, Globals.getPCList().size());
	}

	@Test
	void anUnknownRaceIsReportedOnItsLine()
	{
		SummonCatalog catalog = SummonCatalogParser.catalogOf("## IV\nsummon: Archon (Hound)\nsummon: No Such Race\n")
			.orElseThrow();

		assertEquals(List.of(3), CatalogResolution.unresolvedIn(catalog).stream().map(SummonParseError::line).toList());
	}

	@Test
	void anUnknownTemplateIsReportedOnItsLine()
	{
		SummonCatalog catalog =
			SummonCatalogParser.catalogOf("## IV\nsummon: Eagle\n  template: No Such Template\n").orElseThrow();

		assertEquals(List.of(2), CatalogResolution.unresolvedIn(catalog).stream().map(SummonParseError::line).toList());
	}

	@Test
	void timesTheBuildOfThreeCreatures()
	{
		long start = System.nanoTime();
		for (int built = 0; built < 3; built++)
		{
			builder.statsOf(builder.build(HOUND_ARCHON, true));
		}
		long milliseconds = (System.nanoTime() - start) / 1_000_000;

		System.out.println("Built and read 3 creatures in " + milliseconds + " ms");
		assertTrue(milliseconds >= 0);
	}

	@Test
	void buildsCatalogCreatureAwayFromTheUiThread() throws Exception
	{
		try (var worker = Executors.newSingleThreadExecutor())
		{
			CreatureStats archon = worker.submit(() -> statsOf(HOUND_ARCHON, true)).get(30, TimeUnit.SECONDS);
			assertEquals(51, archon.maximumHitPoints());
			assertEquals(6, archon.hitDice());
		}
	}
}
