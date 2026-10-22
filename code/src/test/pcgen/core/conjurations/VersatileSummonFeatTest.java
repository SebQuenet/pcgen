/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package pcgen.core.conjurations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import pcgen.cdom.base.UserSelection;
import pcgen.cdom.content.CNAbility;
import pcgen.cdom.content.CNAbilityFactory;
import pcgen.cdom.enumeration.Nature;
import pcgen.cdom.helper.CNAbilitySelection;
import pcgen.core.Ability;
import pcgen.core.AbilityCategory;
import pcgen.core.Globals;
import pcgen.core.PlayerCharacter;
import pcgen.core.Race;
import pcgen.core.character.CharacterSpell;
import pcgen.core.spell.Spell;

import org.junit.jupiter.api.Test;

/**
 * The two templates chosen with Versatile Summon Monster open its options in the catalog.
 */
class VersatileSummonFeatTest
{
	private static PlayerCharacter versatileSummoner(String... templates)
	{
		PlayerCharacter character = BestiarySources.newCharacter("Versatile summoner");
		Race race = character.getRace();
		Spell summonMonster = new Spell();
		summonMonster.setName("Summon Monster I");
		CharacterSpell known = new CharacterSpell(race, summonMonster);
		known.addInfo(1, 1, Globals.getDefaultSpellBook());
		character.addCharacterSpell(race, known);
		Ability feat = Globals.getContext().getReferenceContext().getManufacturerId(AbilityCategory.FEAT)
			.getActiveObject("Versatile Summon Monster");
		CNAbility versatile = CNAbilityFactory.getCNAbility(AbilityCategory.FEAT, Nature.NORMAL, feat);
		for (String template : templates)
		{
			character.addAbility(new CNAbilitySelection(versatile, template), UserSelection.getInstance(),
				UserSelection.getInstance());
		}
		return character;
	}

	private static List<String> namesFor(PlayerCharacter character)
	{
		return PathfinderSummonOptions.withPublishedOptions(character, Optional.empty()).groups().stream()
			.flatMap(group -> group.summons().stream()).map(SummonTemplate::name).toList();
	}

	@Test
	void offersTheTemplatesChosenWithTheFeat()
	{
		assertTrue(namesFor(versatileSummoner("Aerial Creature", "Fiery Creature"))
			.contains("Dog · Aerial Creature [Versatile SM I]"));
	}

	@Test
	void offersNoTemplateLeftUnchosen()
	{
		assertFalse(namesFor(versatileSummoner("Aerial Creature", "Fiery Creature"))
			.contains("Dog · Dark Creature [Versatile SM I]"));
	}
}
