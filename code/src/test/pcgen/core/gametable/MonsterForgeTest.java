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
package pcgen.core.gametable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Stream;

import pcgen.core.PlayerCharacter;
import pcgen.core.character.SpellInfo;
import pcgen.core.conjurations.AppliedBonus;
import pcgen.core.conjurations.BestiarySources;
import pcgen.core.conjurations.BonusSourceKey;
import pcgen.core.conjurations.CreatureStats;
import pcgen.session.HeadlessUIDelegate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Monsters built from their recipe on the Pathfinder Bestiary, and what the game table reads of them.
 */
class MonsterForgeTest
{
	private static final MonsterRecipe WOLF = MonsterRecipe.of(new MonsterSource.BestiaryRace("Wolf"));

	private static MonsterForge forge;
	private static CombatantSheets sheets;

	@BeforeAll
	static void loadTheBestiary()
	{
		forge = new MonsterForge(BestiarySources.campaigns(), new HeadlessUIDelegate());
		sheets = new CombatantSheets(forge.builder());
	}

	private static PlayerCharacter forged(MonsterRecipe recipe)
	{
		ForgedMonster monster = forge.forge(recipe);
		assertInstanceOf(ForgedMonster.Forged.class, monster, monster.toString());
		return ((ForgedMonster.Forged) monster).character();
	}

	private static CreatureStats statsOf(MonsterRecipe recipe)
	{
		return sheets.stats(forged(recipe));
	}

	private static int number(String signed)
	{
		return Integer.parseInt(signed.split("/")[0].replace("+", "").strip());
	}

	@Test
	void anAdvancedWolfHasMoreHitPoints()
	{
		assertTrue(statsOf(WOLF.withTemplate("Advanced Creature")).maximumHitPoints() > statsOf(WOLF)
			.maximumHitPoints());
	}

	@Test
	void addedHitDiceAddToTheWolfs()
	{
		assertEquals(statsOf(WOLF).hitDice() + 2, statsOf(WOLF.withHitDice(2)).hitDice());
	}

	@Test
	void aTemplateTakenOffAgainLeavesThePlainWolf()
	{
		MonsterRecipe undone = WOLF.withTemplate("Advanced Creature").withoutTemplate("Advanced Creature");

		assertEquals(statsOf(WOLF).maximumHitPoints(), statsOf(undone).maximumHitPoints());
	}

	@Test
	void shakenLowersTheWolfsBiteByTwo()
	{
		PlayerCharacter wolf = forged(WOLF);
		int bite = number(sheets.stats(wolf).attacks().getFirst().toHit());
		Condition shaken = new Condition("Shaken", Optional.of(new BonusSourceKey(BonusSourceKey.TEMPLATE, "",
			"Shaken")), OptionalInt.of(2));

		sheets.lay(wolf, Set.of(), List.of(shaken));

		assertEquals(bite - 2, number(sheets.stats(wolf).attacks().getFirst().toHit()));
	}

	@Test
	void takingShakenOffRestoresTheBite()
	{
		PlayerCharacter wolf = forged(WOLF);
		int bite = number(sheets.stats(wolf).attacks().getFirst().toHit());
		Condition shaken = new Condition("Shaken", Optional.of(new BonusSourceKey(BonusSourceKey.TEMPLATE, "",
			"Shaken")), OptionalInt.of(2));
		Set<AppliedBonus> laid = sheets.lay(wolf, Set.of(), List.of(shaken));

		sheets.lay(wolf, laid, List.of());

		assertEquals(bite, number(sheets.stats(wolf).attacks().getFirst().toHit()));
	}

	@Test
	void anAgileWolfActsTwiceARound()
	{
		assertTrue(sheets.hasDualInitiative(forged(WOLF.withTemplate("Mythic Simple Template ~ Agile"))));
	}

	@Test
	void aPlainWolfActsOnce()
	{
		assertEquals(false, sheets.hasDualInitiative(forged(WOLF)));
	}

	@Test
	void aWolfIsBuiltWithItsTacticalPlan()
	{
		assertEquals(BestiaryTactics.of("Wolf"), forged(WOLF).getDisplay().getTacticalPlan());
	}

	@Test
	void aWolfHasAChallengeRating()
	{
		assertEquals("1", sheets.challengeRating(forged(WOLF)));
	}

	@Test
	void aMissingCharacterFileIsReported()
	{
		ForgedMonster monster = forge.forge(MonsterRecipe.of(new MonsterSource.CharacterFile("/nowhere/chief.pcg")));

		assertInstanceOf(ForgedMonster.Missing.class, monster);
	}

	@Test
	void aRaceTheSourcesLackIsReported()
	{
		ForgedMonster monster = forge.forge(MonsterRecipe.of(new MonsterSource.BestiaryRace("No Such Beast")));

		assertInstanceOf(ForgedMonster.Missing.class, monster);
	}

	@Test
	void aWolfCanBeShaken()
	{
		Map<String, BonusSourceKey> sources = CombatantSheets.conditionSources(Optional.of(forged(WOLF)));

		assertTrue(sources.containsValue(new BonusSourceKey(BonusSourceKey.TEMPLATE, "", "Shaken")));
	}

	@Test
	void aPlayerGlanceReadsPerception()
	{
		PlayerGlance glance = sheets.glance(BestiarySources.newCharacter("Seoni"));

		assertTrue(glance.perception().matches("[+-]\\d+"), glance.perception());
	}

	private static FeintDefense feintOf(String race)
	{
		return sheets.feint(forged(MonsterRecipe.of(new MonsterSource.BestiaryRace(race))));
	}

	@Test
	void anOgreIsFeintedAtTenPlusItsBaseAttackAndWisdom()
	{
		assertEquals(new FeintDefense.Difficulty(13, FeintDefense.Hindrance.NONE), feintOf("Ogre"));
	}

	@Test
	void aWolfsAnimalMindMakesItEightHarderToFeint()
	{
		assertEquals(new FeintDefense.Difficulty(12, FeintDefense.Hindrance.ANIMAL_MIND), feintOf("Wolf"));
	}

	@Test
	void aBabausTrainedSenseMotiveSetsItsFeintAsAnOutsider()
	{
		assertEquals(new FeintDefense.Difficulty(21, FeintDefense.Hindrance.NONHUMANOID), feintOf("Demon (Babau)"));
	}

	@Test
	void aMindlessZombieCannotBeFeinted()
	{
		assertInstanceOf(FeintDefense.Impossible.class, feintOf("Zombie (Human)"));
	}

	@Test
	void aBabausSneakAttackIsASpecialAttackApartFromItsQualities()
	{
		MonsterSheet babau = sheets.sheet(forged(MonsterRecipe.of(new MonsterSource.BestiaryRace("Demon (Babau)"))));

		assertEquals(List.of(1, 3, 2), List.of(babau.abilities().attacks().size(),
			babau.abilities().qualities().size(), babau.abilities().defenses().size()));
	}

	@Test
	void anOgreSheetListsItsFeats()
	{
		MonsterSheet ogre = sheets.sheet(forged(MonsterRecipe.of(new MonsterSource.BestiaryRace("Ogre"))));

		assertEquals(2, ogre.feats().size(), ogre.feats().toString());
	}

	@Test
	void aDrowNobleTakesTheDomainsHerKitGrants()
	{
		PlayerCharacter drow = forged(MonsterRecipe.of(new MonsterSource.BestiaryRace("Drow Noble")));

		assertEquals(2, drow.getDomainCount());
	}

	@Test
	void aDriderListsItsClimbOfTwentyTwoFirst()
	{
		MonsterSheet drider = sheets.sheet(forged(MonsterRecipe.of(new MonsterSource.BestiaryRace("Drider"))));

		assertEquals(22, drider.skills().getFirst().total());
	}

	@Test
	void aDriderCastsDarknessAtWill()
	{
		MonsterSheet drider = sheets.sheet(forged(MonsterRecipe.of(new MonsterSource.BestiaryRace("Drider"))));

		assertTrue(drider.spells().stream().flatMap(caster -> caster instanceof MonsterRepertoire.CasterSpells.Innate innate
			? innate.spells().stream() : Stream.empty())
			.anyMatch(spell -> spell.name().equals("Darkness") && spell.times() == SpellInfo.TIMES_AT_WILL));
	}

	@Test
	void aDrowNobleShowsTheSpellsHerClericPreparedRatherThanEveryOneSheKnows()
	{
		MonsterSheet drow = sheets.sheet(forged(MonsterRecipe.of(new MonsterSource.BestiaryRace("Drow Noble"))));

		assertEquals(List.of(10), drow.spells().stream().flatMap(caster -> caster instanceof MonsterRepertoire.CasterSpells.Levelled
			levelled ? Stream.of(levelled.spells().size()) : Stream.empty()).toList());
	}

	@Test
	void aDriderHasTheSixScoresOfItsStatBlock()
	{
		MonsterSheet drider = sheets.sheet(forged(MonsterRecipe.of(new MonsterSource.BestiaryRace("Drider"))));

		assertEquals(List.of(OptionalInt.of(15), OptionalInt.of(15), OptionalInt.of(18), OptionalInt.of(15), OptionalInt.of(16),
			OptionalInt.of(16)), drider.abilityScores().stream().map(AbilityScore::score).toList());
	}
}
