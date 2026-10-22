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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import pcgen.cdom.base.UserSelection;
import pcgen.cdom.content.CNAbilityFactory;
import pcgen.cdom.enumeration.Nature;
import pcgen.cdom.helper.CNAbilitySelection;
import pcgen.core.Ability;
import pcgen.core.AbilityCategory;
import pcgen.core.Globals;
import pcgen.core.PlayerCharacter;
import pcgen.core.Race;
import pcgen.core.SettingsHandler;
import pcgen.core.character.CharacterSpell;
import pcgen.core.spell.Spell;

import org.junit.jupiter.api.Test;

/**
 * Mighty Summons offers the agile and savage variants of the two highest
 * summoning levels, each summoned alone.
 */
class MythicSummonVariantsTest
{
	private static PlayerCharacter mightySummoner()
	{
		PlayerCharacter character = BestiarySources.newCharacter("Mighty summoner");
		Race race = character.getRace();
		Spell summonMonster = new Spell();
		summonMonster.setName("Summon Monster VI");
		CharacterSpell known = new CharacterSpell(race, summonMonster);
		known.addInfo(6, 1, Globals.getDefaultSpellBook());
		character.addCharacterSpell(race, known);
		AbilityCategory specialAbilities = SettingsHandler.getGameAsProperty().get()
			.getAbilityCategory("Special Ability");
		Ability mightySummons = Globals.getContext().getReferenceContext().getManufacturerId(specialAbilities)
			.getActiveObject("Mythic Path Ability ~ Mighty Summons");
		character.addAbility(new CNAbilitySelection(CNAbilityFactory.getCNAbility(specialAbilities, Nature.AUTOMATIC,
			mightySummons)), UserSelection.getInstance(), UserSelection.getInstance());
		return character;
	}

	private static List<String> namesIn(SummonCatalog catalog)
	{
		return catalog.groups().stream().flatMap(group -> group.summons().stream()).map(SummonTemplate::name).toList();
	}

	@Test
	void offersASavageVariantAtTheHighestLevel()
	{
		List<String> names = namesIn(PathfinderSummonOptions.withPublishedOptions(mightySummoner(), Optional.empty()));

		assertTrue(names.contains("Elemental (Air/Huge) · Savage [SM VI]"));
	}

	@Test
	void offersAnAgileVariantOneLevelBelow()
	{
		List<String> names = namesIn(PathfinderSummonOptions.withPublishedOptions(mightySummoner(), Optional.empty()));

		assertTrue(names.contains("Elemental (Air/Large) · Agile [SM V]"));
	}

	@Test
	void offersNoVariantTwoLevelsBelow()
	{
		List<String> names = namesIn(PathfinderSummonOptions.withPublishedOptions(mightySummoner(), Optional.empty()));

		assertTrue(names.stream().noneMatch(name -> name.endsWith("· Savage [SM IV]")));
	}

	@Test
	void aVariantComesAlone()
	{
		PlayerCharacter summoner = mightySummoner();
		SummonTemplate variant = PathfinderSummonOptions.withPublishedOptions(summoner, Optional.empty())
			.find("Elemental (Air/Large) · Agile [SM V]").orElseThrow();

		assertEquals(1, PathfinderSummonOptions.maximumCount(summoner, variant, 5));
	}

	@Test
	void mightySummonsAddsOneCreatureToASpellThatBringsSeveral()
	{
		PlayerCharacter summoner = mightySummoner();
		SummonTemplate elemental = PathfinderSummonOptions.withPublishedOptions(summoner, Optional.empty())
			.find("Elemental (Air/Large) [SM V]").orElseThrow();

		assertEquals(4, PathfinderSummonOptions.maximumCount(summoner, elemental, 5));
	}
}
