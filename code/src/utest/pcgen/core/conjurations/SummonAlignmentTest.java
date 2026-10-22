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
 * Which creature alignments lie within one step of a deity's alignment.
 */
class SummonAlignmentTest
{
	private static final List<String> ALL = List.of("LG", "NG", "CG", "LN", "TN", "CN", "LE", "NE", "CE");

	@Test
	void aLawfulGoodDeityAcceptsOnlyLawfulGoodNeutralGoodAndLawfulNeutral()
	{
		List<String> accepted = ALL.stream().filter(creature -> SummonAlignment.isWithinOneStep("LG", creature)).toList();

		assertEquals(List.of("LG", "NG", "LN"), accepted);
	}

	@Test
	void aTrueNeutralDeityAcceptsEveryAlignmentOneStepAway()
	{
		List<String> accepted = ALL.stream().filter(creature -> SummonAlignment.isWithinOneStep("TN", creature)).toList();

		assertEquals(List.of("NG", "LN", "TN", "CN", "NE"), accepted);
	}

	@Test
	void anUnknownAlignmentIsNeverWithinOneStep()
	{
		assertEquals(false, SummonAlignment.isWithinOneStep("LG", "NONE"));
	}

	@Test
	void aCelestialTemplateMakesANeutralAnimalNeutralGood()
	{
		assertEquals("NG", SummonAlignment.withTemplate("TN", "Celestial Creature"));
	}

	@Test
	void aResoluteTemplateKeepsTheCreaturesGoodEvilAxis()
	{
		assertEquals("LG", SummonAlignment.withTemplate("CG", "Resolute Creature"));
	}

	@Test
	void aCelestialTemplateLeavesAChaoticCreatureChaotic()
	{
		assertEquals("CG", SummonAlignment.withTemplate("CG", "Celestial Creature"));
	}

	@Test
	void aTemplateWithoutAnAlignmentChangesNothing()
	{
		assertEquals("TN", SummonAlignment.withTemplate("TN", "Aerial Creature"));
	}
}
