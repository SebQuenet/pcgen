/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package pcgen.core.conjurations;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import pcgen.AbstractCharacterTestCase;
import pcgen.core.Globals;
import pcgen.core.PlayerCharacter;
import pcgen.core.Race;
import pcgen.core.character.CharacterSpell;
import pcgen.core.spell.Spell;

import org.junit.jupiter.api.Test;

class PathfinderSummonOptionsTest extends AbstractCharacterTestCase
{
	@Test
	void onlyKnownSummonFamiliesContributeLevels()
	{
		PlayerCharacter character = getCharacter();
		assertArrayEquals(new int[] {0, 0}, PathfinderSummonOptions.knownSummonLevels(character));
		Race race = character.getRace();
		Spell summonMonster = new Spell();
		summonMonster.setName("Summon Monster IV");
		CharacterSpell known = new CharacterSpell(race, summonMonster);
		known.addInfo(4, 1, Globals.getDefaultSpellBook());
		character.addCharacterSpell(race, known);

		assertArrayEquals(new int[] {4, 0}, PathfinderSummonOptions.knownSummonLevels(character));
	}

	@Test
	void innateSummonAbilityWithAQualifierCountsForItsTier()
	{
		PlayerCharacter character = getCharacter();
		Race race = character.getRace();
		Spell summonAbility = new Spell();
		summonAbility.setName("Summon Monster VI (Invocateur Naturel)");
		CharacterSpell innate = new CharacterSpell(race, summonAbility);
		innate.addInfo(0, 3, "Innate");
		character.addCharacterSpell(race, innate);

		assertArrayEquals(new int[] {6, 0}, PathfinderSummonOptions.knownSummonLevels(character));
	}
}
