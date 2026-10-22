/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package pcgen.core.conjurations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * What a creature's templates change, compared with the same creature without them.
 */
class TemplateDifferencesTest
{
	private static final CreatureStats WOLF = new CreatureStats(2, "50 ft.", 13, 14, 12, 12,
		List.of(new CreatureAttack("Bite", "+2", "1d6+1")), "+2", 14, "+5", "+5", "+1", "+2", "Scent, Low-Light Vision");

	private static CreatureStats wolfWith(int armorClass, String initiative, String specialAbilities)
	{
		return new CreatureStats(2, "50 ft.", 13, armorClass, 12, 12, List.of(new CreatureAttack("Bite", "+2", "1d6+1")),
			"+2", 14, "+5", "+5", "+1", initiative, specialAbilities);
	}

	@Test
	void namesOnlyTheNumbersTheTemplatesChange()
	{
		List<TemplateDifference> differences = TemplateDifferences.between(WOLF,
			wolfWith(16, "+2", "Scent, Low-Light Vision"));

		assertEquals(List.of(new TemplateDifference("ac", "14 / 12 / 12", "16 / 12 / 12")), differences);
	}

	@Test
	void listsTheAbilitiesTheTemplatesAdd()
	{
		List<TemplateDifference> differences = TemplateDifferences.between(WOLF,
			wolfWith(14, "+2", "Dual Initiative, Scent, Low-Light Vision, Evasion"));

		assertEquals(List.of(new TemplateDifference("abilities", "", "Dual Initiative, Evasion")), differences);
	}

	@Test
	void findsNothingWhenTheTemplatesChangeNothing()
	{
		assertEquals(List.of(), TemplateDifferences.between(WOLF, WOLF));
	}

	@Test
	void keepsTheOrderOfTheCardsStatistics()
	{
		List<String> stats = TemplateDifferences.between(WOLF, wolfWith(16, "+22", "Scent, Low-Light Vision"))
			.stream().map(TemplateDifference::stat).toList();

		assertEquals(List.of("ac", "init"), stats);
	}
}
