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
import java.util.Optional;

import pcgen.cdom.base.UserSelection;
import pcgen.cdom.content.CNAbilityFactory;
import pcgen.cdom.enumeration.Nature;
import pcgen.cdom.helper.CNAbilitySelection;
import pcgen.cdom.util.CControl;
import pcgen.core.Ability;
import pcgen.core.AbilityCategory;
import pcgen.core.Deity;
import pcgen.core.Globals;
import pcgen.core.PlayerCharacter;
import pcgen.core.Race;
import pcgen.core.SettingsHandler;
import pcgen.core.character.CharacterSpell;
import pcgen.core.spell.Spell;
import pcgen.output.channel.ChannelUtilities;

import org.junit.jupiter.api.Test;

/**
 * The Invocateur de Saint Thero's Héraut Divin limits summon monster to the
 * saint's servants, plus the creatures of Summon Good Monster.
 */
class DevotedHeraldSummonsTest
{
	private static PlayerCharacter devotedHerald(String highestSummonMonster, int spellLevel)
	{
		return devotedHerald(highestSummonMonster, spellLevel, Optional.empty());
	}

	private static PlayerCharacter devotedHerald(String highestSummonMonster, int spellLevel, Optional<String> deity)
	{
		PlayerCharacter character = BestiarySources.newCharacter("Devoted herald");
		// The deity goes first: choosing it afterwards drops the summon spell added below
		deity.ifPresent(key -> ChannelUtilities.setControlledChannel(character.getCharID(), CControl.DEITYINPUT,
			Globals.getContext().getReferenceContext().silentlyGetConstructedCDOMObject(Deity.class, key)));
		Race race = character.getRace();
		Spell summonMonster = new Spell();
		summonMonster.setName(highestSummonMonster);
		CharacterSpell known = new CharacterSpell(race, summonMonster);
		known.addInfo(spellLevel, 1, Globals.getDefaultSpellBook());
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
		return PathfinderSummonOptions.withPublishedOptions(devotedHerald("Summon Monster IX", 9), Optional.empty())
			.groups().stream().flatMap(group -> group.summons().stream()).map(SummonTemplate::name).toList();
	}

	private static Optional<SummonTemplate> summonOfFourthLevelHerald(String name)
	{
		return PathfinderSummonOptions.withPublishedOptions(devotedHerald("Summon Monster IV", 4), Optional.empty())
			.groups().stream().flatMap(group -> group.summons().stream())
			.filter(summon -> summon.name().equals(name)).findFirst();
	}

	private static List<String> heraldSummonsWithAuthored(String catalog)
	{
		return PathfinderSummonOptions.withPublishedOptions(devotedHerald("Summon Monster IX", 9),
				SummonCatalogParser.catalogOf(catalog)).groups().stream()
			.flatMap(group -> group.summons().stream()).map(SummonTemplate::name).toList();
	}

	@Test
	void dropsAHandWrittenCreatureOutsideSaintTherosAndSummonGoodMonstersLists()
	{
		assertFalse(heraldSummonsWithAuthored("## Convocation de monstres VI\nsummon: Tigre sanguinaire céleste\n"
			+ "  template: Celestial Creature\n  race: Tiger (Dire)\n").contains("Tigre sanguinaire céleste"));
	}

	@Test
	void keepsAHandWrittenServantOfSaintThero()
	{
		assertTrue(heraldSummonsWithAuthored("## Convocation de monstres IV\nsummon: Lion céleste\n"
			+ "  template: Celestial Creature\n  race: Lion\n").contains("Lion céleste"));
	}

	private static List<String> summonsOfSaintTherosHerald(Optional<SummonCatalog> authored)
	{
		PlayerCharacter character = devotedHerald("Summon Monster IX", 9, Optional.of("Saint Thero"));
		return PathfinderSummonOptions.withPublishedOptions(character, authored).groups().stream()
			.flatMap(group -> group.summons().stream()).map(SummonTemplate::name).toList();
	}

	@Test
	void dropsAChaoticCreatureOfSummonGoodMonsterForALawfulGoodDeity()
	{
		assertFalse(summonsOfSaintTherosHerald(Optional.empty()).contains("Lillend azata [Good VI]"));
	}

	@Test
	void keepsALawfulGoodCreatureOfSummonGoodMonsterForALawfulGoodDeity()
	{
		assertTrue(summonsOfSaintTherosHerald(Optional.empty()).contains("Lantern archon [Good III]"));
	}

	@Test
	void keepsACelestialAnimalOneStepFromALawfulGoodDeity()
	{
		assertTrue(summonsOfSaintTherosHerald(Optional.empty()).contains("Celestial dog · Celestial Creature [Good I]"));
	}

	@Test
	void dropsAHandWrittenChaoticCreatureForALawfulGoodDeity()
	{
		assertFalse(summonsOfSaintTherosHerald(SummonCatalogParser.catalogOf(
			"## Convocation de monstres VI\nsummon: Azata lillend\n  race: Azata (Lillend)\n"))
			.contains("Azata lillend"));
	}

	@Test
	void keepsAChaoticServantThatSaintTheroChoseHimself()
	{
		assertTrue(summonsOfSaintTherosHerald(Optional.empty()).contains("Lyrakien azata [Saint Thero III]"));
	}

	@Test
	void keepsAHandWrittenChaoticServantOfSaintThero()
	{
		assertTrue(summonsOfSaintTherosHerald(SummonCatalogParser.catalogOf(
			"## Convocation de monstres III\nsummon: Azata lyrakien\n  race: Azata (Lyrakien)\n"))
			.contains("Azata lyrakien"));
	}

	@Test
	void showsTheNinthLevelServantsBeforeTheHeraldCanSummonThem()
	{
		assertTrue(summonOfFourthLevelHerald("Phoenix [Saint Thero IX]").isPresent());
	}

	@Test
	void marksTheServantsAboveTheHeraldsReachAsNotYetSummonable()
	{
		assertEquals(Optional.of("true"), summonOfFourthLevelHerald("Phoenix [Saint Thero IX]")
			.map(summon -> summon.metadata().get("beyondReach")));
	}

	@Test
	void leavesTheServantsWithinTheHeraldsReachSummonable()
	{
		assertEquals(Optional.empty(), summonOfFourthLevelHerald("Stag archon [Saint Thero IV]")
			.flatMap(summon -> Optional.ofNullable(summon.metadata().get("beyondReach"))));
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
