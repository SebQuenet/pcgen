/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package pcgen.core.conjurations;

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
 * The Invocateur de Saint Thero's Héraut Divin limits summon monster to the
 * saint's servants, plus the creatures of Summon Good Monster.
 */
class DevotedHeraldSummonsTest
{
	private static PlayerCharacter devotedHerald()
	{
		PlayerCharacter character = BestiarySources.newCharacter("Devoted herald");
		Race race = character.getRace();
		Spell summonMonster = new Spell();
		summonMonster.setName("Summon Monster IX");
		CharacterSpell known = new CharacterSpell(race, summonMonster);
		known.addInfo(9, 1, Globals.getDefaultSpellBook());
		character.addCharacterSpell(race, known);
		AbilityCategory specialAbilities = SettingsHandler.getGameAsProperty().get()
			.getAbilityCategory("Special Ability");
		Ability divineHerald = Globals.getContext().getReferenceContext().getManufacturerId(specialAbilities)
			.getActiveObject("Invocateur de Saint Thero ~ Héraut Divin");
		character.addAbility(new CNAbilitySelection(CNAbilityFactory.getCNAbility(specialAbilities, Nature.AUTOMATIC,
			divineHerald)), UserSelection.getInstance(), UserSelection.getInstance());
		return character;
	}

	private static List<String> heraldSummons()
	{
		return PathfinderSummonOptions.withPublishedOptions(devotedHerald(), Optional.empty()).groups().stream()
			.flatMap(group -> group.summons().stream()).map(SummonTemplate::name).toList();
	}

	@Test
	void offersTheRepentantErinyes()
	{
		assertTrue(heraldSummons().contains("Repentant erinyes [Saint Thero VI]"));
	}

	@Test
	void offersSaintTheroAnimalsWithTheResoluteTemplate()
	{
		assertTrue(heraldSummons().contains("Lion · Resolute Creature [Saint Thero IV]"));
	}

	@Test
	void dropsTheStandardSummonMonsterList()
	{
		assertTrue(heraldSummons().stream().noneMatch(name -> name.contains("[SM ")));
	}

	@Test
	void offersTheHarbingerArchon()
	{
		assertTrue(heraldSummons().contains("Harbinger archon [Saint Thero II]"));
	}

	@Test
	void keepsEveryCreatureOfSummonGoodMonster()
	{
		assertTrue(heraldSummons().contains("Storm giant [Good IX]"));
	}

	@Test
	void offersTheCelestialAnimalsOfSummonGoodMonsterWithTheResoluteTemplate()
	{
		assertTrue(heraldSummons().contains("Celestial dog · Resolute Creature [Good I]"));
	}

	@Test
	void offersTheGrisantianLionOfCelmae()
	{
		assertTrue(heraldSummons().contains("Grisantian lion · Celestial Creature [Saint Thero IX]"));
	}

	@Test
	void offersTheUdaeusCelestialSquireOfCelmae()
	{
		assertTrue(heraldSummons().contains("Udaeus celestial squire [Saint Thero VIII]"));
	}

	@Test
	void offersTheFooDogDespiteItsMissingBestiaryKit()
	{
		assertTrue(heraldSummons().contains("Foo dog [Good III]"));
	}

	@Test
	void offersTheLiminalSpriteOfCelmae()
	{
		assertTrue(heraldSummons().contains("Liminal sprite · Celestial Creature [Saint Thero III]"));
	}

	@Test
	void offersTheGiantOwl()
	{
		assertTrue(heraldSummons().contains("Giant owl · Resolute Creature [Saint Thero V]"));
	}

	@Test
	void offersTheMoonDogOfCelmae()
	{
		assertTrue(heraldSummons().contains("Moon dog [Saint Thero VII]"));
	}
}
