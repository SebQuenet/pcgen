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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class SummonFormsTest
{
	private static final SummonTemplate HOUND = new SummonTemplate("Archon (Hound)", List.of(), 1);
	private static final CreatureStats BASE = new CreatureStats(6, "40 ft.", 39, 19, 10, 19,
		List.of(new CreatureAttack("Bite", "+8", "1d8+3"),
			new CreatureAttack("Greatsword", "+9/+4", "2d6+3")), "+8", 18, "+5", "+6", "+5", "+4");

	@Test
	void canineFormsKeepTheArchonsHitDiceAndHitPointsButReplaceItsAttacks()
	{
		CreatureStats wolf = SummonForms.statsOf(HOUND, "wolf", BASE);

		assertEquals(6, wolf.hitDice());
		assertEquals(39, wolf.maximumHitPoints());
		assertEquals(12, wolf.armorClass());
		assertEquals("50 ft.", wolf.speed());
		assertEquals(List.of(new CreatureAttack("Bite", "+9", "1d6+4")), wolf.attacks());
		assertEquals("+9", wolf.combatManeuverBonus());
	}

	@Test
	void theLargeFormChangesManeuversAndTheSmallFormChangesReflex()
	{
		CreatureStats dog = SummonForms.statsOf(HOUND, "dog", BASE);
		CreatureStats direWolf = SummonForms.statsOf(HOUND, "dire-wolf", BASE);

		assertEquals("+6", dog.reflex());
		assertEquals("+7", dog.combatManeuverBonus());
		assertEquals("+11", direWolf.combatManeuverBonus());
		assertEquals("1d8+6", direWolf.attacks().getFirst().damage());
	}

	private static final SummonTemplate STAG = new SummonTemplate("Archon (Stag)", List.of(), 1);
	private static final CreatureStats STAG_ARCHON = new CreatureStats(6, "40 ft.", 33, 18, 12, 16,
		List.of(new CreatureAttack("Gore", "+7", "1d6+1"),
			new CreatureAttack("Longbow", "+9/+4", "1d8")), "+7", 19, "+7", "+5", "+7", "+2");

	@Test
	void theStagArchonOffersTheLargeStagForm()
	{
		assertEquals(List.of("stag"), SummonForms.forSummon(STAG).stream().map(SummonForms.Form::key).toList());
	}

	@Test
	void theLargeStagGoresAndKicksWithItsHooves()
	{
		assertEquals(List.of(new CreatureAttack("Gore", "+8", "1d8+3"), new CreatureAttack("Hooves (2)", "+3", "1d4+1")),
			SummonForms.statsOf(STAG, "stag", STAG_ARCHON).attacks());
	}

	@Test
	void theLargeStagRunsAtFiftyFeet()
	{
		assertEquals("50 ft.", SummonForms.statsOf(STAG, "stag", STAG_ARCHON).speed());
	}

	@Test
	void theLargeStagLosesArmorClassToItsSizeAndDexterity()
	{
		CreatureStats stag = SummonForms.statsOf(STAG, "stag", STAG_ARCHON);

		assertEquals(List.of(16, 10, 15), List.of(stag.armorClass(), stag.touchArmorClass(),
			stag.flatFootedArmorClass()));
	}

	@Test
	void theLargeStagGainsStrengthAndSizeOnManeuvers()
	{
		CreatureStats stag = SummonForms.statsOf(STAG, "stag", STAG_ARCHON);

		assertEquals(List.of("+10", "21"), List.of(stag.combatManeuverBonus(),
			Integer.toString(stag.combatManeuverDefense())));
	}

	@Test
	void theLargeStagLosesADexterityPointOnReflexAndInitiative()
	{
		CreatureStats stag = SummonForms.statsOf(STAG, "stag", STAG_ARCHON);

		assertEquals(List.of("+6", "+1"), List.of(stag.reflex(), stag.initiative()));
	}

	@Test
	void lanternGestaltIsCollectiveRatherThanAnIndividualShape()
	{
		SummonTemplate lantern = new SummonTemplate("Archon (Lantern)", List.of(), 1);
		assertTrue(SummonForms.forSummon(lantern).isEmpty());
		assertTrue(SummonForms.collectiveForm(lantern).isPresent());
		assertFalse(SummonForms.supports(lantern, "gestalt"));
	}

	@Test
	void bralaniCanFlyOnlyInWindForm()
	{
		SummonTemplate bralani = new SummonTemplate("Azata (Bralani)", List.of(), 1);
		CreatureStats base = new CreatureStats(7, "40 ft., fly 100 ft.", 66, 20, 14, 16,
			List.of(new CreatureAttack("Scimitar", "+13/+8", "1d6+8"),
				new CreatureAttack("Slam", "+12", "1d6+7")), "+12", 26, "+9", "+9", "+6", "+8");

		assertEquals("40 ft.", SummonForms.statsOf(bralani, "", base).speed());
		CreatureStats wind = SummonForms.statsOf(bralani, "wind", base);
		assertEquals("fly 100 ft. (perfect)", wind.speed());
		assertEquals(List.of(new CreatureAttack("Slam", "+12", "1d6+7")), wind.attacks());
	}
}
