/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package pcgen.io.conjurations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import pcgen.core.conjurations.CreatureAttack;
import pcgen.core.conjurations.CreatureStats;
import pcgen.core.conjurations.SummonCatalog;
import pcgen.core.conjurations.SummonForms;
import pcgen.core.conjurations.SummonGroup;
import pcgen.core.conjurations.SummonTemplate;
import pcgen.system.LanguageBundle;

import org.junit.jupiter.api.Test;

/**
 * The catalog laid out as rows of the downloadable workbook.
 */
class ConjurationSpreadsheetTest
{
	private static final CreatureStats LEGION_ARCHON = new CreatureStats(9, "40 ft., fly 90 ft.", 92, 23, 11, 22,
		List.of(new CreatureAttack("Longsword", "+15/+10", "2d6+7"), new CreatureAttack("Javelin", "+12", "1d6+6")),
		"+13", 24, "+3", "+12", "+8", "+0", "Aura of Menace, DR 10/evil");

	private static SummonTemplate summon(String name, Map<String, String> metadata)
	{
		return new SummonTemplate(name, List.of(), 0, List.of(), metadata);
	}

	private static List<List<Object>> rowsOf(SummonCatalog catalog)
	{
		return layoutOf(catalog).rows();
	}

	private static ConjurationSpreadsheet.Layout layoutOf(SummonCatalog catalog)
	{
		return ConjurationSpreadsheet.layoutOf(catalog, (summon, level) -> level + 1,
			summon -> Optional.of(LEGION_ARCHON));
	}

	@Test
	void putsTheMaximumCountRightAfterTheCreaturesName()
	{
		List<Object> header = rowsOf(new SummonCatalog(List.of())).get(0);

		assertEquals(LanguageBundle.getString("in_conjurations_sheet_maximum_count"), header.get(3));
	}

	@Test
	void writesACreaturesLevelNameCountAndComputedNumbers()
	{
		SummonCatalog catalog = new SummonCatalog(List.of(new SummonGroup("Summon Monster VI",
			List.of(summon("Archon (Legion)", Map.of("level", "VI"))))));

		List<Object> row = rowsOf(catalog).get(1);

		assertEquals(List.of("VI", "Archon (Legion)", 7, "40 ft., fly 90 ft.", 92, "23 / 11 / 22",
			"Longsword +15/+10\nJavelin +12", "2d6+7\n1d6+6", "Aura of Menace, DR 10/evil", "+13", 24, "+3", "+12",
			"+8", "+0", ""), List.of(row.get(0), row.get(2), row.get(3), row.get(5), row.get(6), row.get(7),
				row.get(8), row.get(9), row.get(10), row.get(11), row.get(12), row.get(13), row.get(14), row.get(15),
				row.get(16), row.get(17)));
	}

	@Test
	void sortsTheCreaturesByLevel()
	{
		SummonCatalog catalog = new SummonCatalog(List.of(
			new SummonGroup("Summon Monster VI", List.of(summon("Archon (Legion)", Map.of()))),
			new SummonGroup("Summon Monster I", List.of(summon("Dog", Map.of())))));

		List<List<Object>> rows = rowsOf(catalog);

		assertEquals(List.of("I", "VI"), List.of(rows.get(1).get(0), rows.get(2).get(0)));
	}

	@Test
	void keepsTheValuesOfAHandwrittenCreature()
	{
		SummonCatalog catalog = new SummonCatalog(List.of(new SummonGroup("Summon Monster IV",
			List.of(summon("Hound archon", Map.of("hp", "51", "ac", "19 / 10 / 19", "spells", "aid"))))));

		List<Object> row = rowsOf(catalog).get(1);

		assertEquals(List.of("51", "19 / 10 / 19", "aid"), List.of(row.get(6), row.get(7), row.get(17)));
	}

	@Test
	void namesTheMythicPowerAMythicVariantCosts()
	{
		SummonCatalog catalog = new SummonCatalog(List.of(new SummonGroup("Summon Monster VI",
			List.of(summon("Archon (Legion) · Savage", Map.of("race", "Archon (Legion)",
				"mythicTemplate", "Mythic Simple Template ~ Savage"))))));

		Object special = rowsOf(catalog).get(1).get(10);

		assertEquals(LanguageBundle.getString("in_conjurations_sheet_mythic_cost") + "\nAura of Menace, DR 10/evil",
			special);
	}

	@Test
	void sortsTheCreaturesOfALevelAlphabetically()
	{
		SummonCatalog catalog = new SummonCatalog(List.of(new SummonGroup("Summon Monster I",
			List.of(summon("Wolf", Map.of()), summon("Eagle", Map.of())))));

		List<List<Object>> rows = rowsOf(catalog);

		assertEquals(List.of("Eagle", "Wolf"), List.of(rows.get(1).get(2), rows.get(2).get(2)));
	}

	@Test
	void foldsTheVariantsOfACreatureUnderItsBase()
	{
		SummonCatalog catalog = new SummonCatalog(List.of(new SummonGroup("Summon Monster VI", List.of(
			new SummonTemplate("Bear (Dire)", List.of("Celestial Creature", "Mythic Simple Template ~ Savage"), 0),
			summon("Archon (Legion)", Map.of()),
			new SummonTemplate("Bear (Dire)", List.of("Celestial Creature"), 0)))));

		ConjurationSpreadsheet.Layout layout = layoutOf(catalog);

		assertEquals(List.of(List.of("Archon (Legion)", "Bear (Dire) (Celestial Creature)",
			"\u21B3 Bear (Dire) (Celestial Creature, Mythic Simple Template ~ Savage)"), Set.of(3)),
			List.of(layout.rows().stream().skip(1).map(row -> row.get(2)).toList(), layout.folded()));
	}

	@Test
	void putsTheRpgbotRatingRightAfterTheMaximumCount()
	{
		List<Object> header = rowsOf(new SummonCatalog(List.of())).get(0);

		assertEquals(LanguageBundle.getString("in_conjurations_sheet_ranking"), header.get(4));
	}

	@Test
	void marksAVariantNameAsBelongingToTheLineAbove()
	{
		SummonCatalog catalog = new SummonCatalog(List.of(new SummonGroup("Summon Monster VI", List.of(
			new SummonTemplate("Bear (Dire)", List.of("Celestial Creature"), 0),
			new SummonTemplate("Bear (Dire)", List.of("Celestial Creature", "Mythic Simple Template ~ Savage"), 0)))));

		Object variantName = rowsOf(catalog).get(2).get(2);

		assertEquals("\u21B3 Bear (Dire) (Celestial Creature, Mythic Simple Template ~ Savage)", variantName);
	}

	@Test
	void addsOneFoldedRowPerAlternateFormUnderTheCreature()
	{
		SummonCatalog catalog = new SummonCatalog(List.of(new SummonGroup("Summon Monster IV",
			List.of(summon("Archon (Hound)", Map.of())))));
		List<String> forms = SummonForms.forSummon(summon("Archon (Hound)", Map.of())).stream()
			.map(form -> "\u21B3 Archon (Hound) \u2014 " + form.label()).toList();

		ConjurationSpreadsheet.Layout layout = layoutOf(catalog);

		assertEquals(List.of(forms, Set.of(2, 3, 4)), List.of(layout.rows().stream().skip(2).map(row -> row.get(2))
			.toList(), layout.folded()));
	}

	@Test
	void computesTheNumbersOfAnAlternateForm()
	{
		SummonCatalog catalog = new SummonCatalog(List.of(new SummonGroup("Summon Monster IV",
			List.of(summon("Archon (Hound)", Map.of())))));
		SummonForms.Form wolf = SummonForms.forSummon(summon("Archon (Hound)", Map.of())).stream()
			.filter(form -> "wolf".equals(form.key())).findFirst().orElseThrow();
		CreatureStats expected = SummonForms.statsOf(summon("Archon (Hound)", Map.of()), "wolf", LEGION_ARCHON);

		List<Object> wolfRow = layoutOf(catalog).rows().stream()
			.filter(row -> String.valueOf(row.get(2)).endsWith(wolf.label())).findFirst().orElseThrow();

		assertEquals(expected.speed(), wolfRow.get(5));
	}
}
