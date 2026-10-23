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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

import pcgen.core.conjurations.BonusSourceKey;

/**
 * Behaviour of a fight: who acts when, what each round wears off, who is hurt.
 */
class CombatTest
{
	private static final MonsterRecipe GOBLIN = MonsterRecipe.of(new MonsterSource.BestiaryRace("Goblin"));

	private static Combatant player(int id, String name)
	{
		return Combatant.player(id, name, Optional.empty());
	}

	private static Combatant goblin(int id, String group)
	{
		return Combatant.monster(id, "Goblin " + id, group, GOBLIN, 6, 2, false);
	}

	private static List<String> names(Combat combat)
	{
		return combat.turns().stream()
			.map(turn -> combat.combatant(turn.combatantId()).orElseThrow().name() + (turn.second() ? "'" : ""))
			.toList();
	}

	private static String acting(Combat combat)
	{
		Turn turn = combat.current().orElseThrow();
		return combat.combatant(turn.combatantId()).orElseThrow().name() + (turn.second() ? "'" : "");
	}

	/** Valeros 22, Seoni 15, two goblins sharing 11, started. */
	private static Combat skirmish()
	{
		Combat combat = Combat.empty().with(player(1, "Valeros")).with(player(2, "Seoni"))
			.with(goblin(3, "goblins")).with(goblin(4, "goblins"));
		return combat.withInitiative(1, 22).withInitiative(2, 15).withInitiative(3, 11).started();
	}

	@Test
	void combatantsActByScoreThenModifier()
	{
		Combat combat = Combat.empty().with(player(1, "Low")).with(goblin(2, "a"))
			.withInitiative(1, 12).withInitiative(2, 12);

		assertEquals(List.of("Goblin 2", "Low"), names(combat));
	}

	@Test
	void aTroopSharesOneInitiative()
	{
		Combat combat = Combat.empty().with(goblin(1, "goblins")).with(goblin(2, "goblins")).withInitiative(1, 9);

		assertEquals(OptionalInt.of(9), combat.combatant(2).orElseThrow().initiative().score());
	}

	@Test
	void missingMonsterInitiativesAreRolledOncePerTroop()
	{
		Combat combat = Combat.empty().with(goblin(1, "goblins")).with(goblin(2, "goblins"))
			.with(player(3, "Seoni"));
		int[] rolls = {14, 3};
		int[] next = {0};

		Combat rolled = combat.rollingMissing(() -> rolls[next[0]++]);

		assertEquals(List.of(OptionalInt.of(16), OptionalInt.of(16), OptionalInt.empty()),
			rolled.order().stream().map(fighter -> fighter.initiative().score()).toList());
	}

	@Test
	void nextGoesRoundTheOrderThenStartsANewRound()
	{
		Combat combat = skirmish().next().combat().next().combat().next().combat().next().combat();

		assertEquals(List.of(2, "Valeros"), List.of(combat.round(), acting(combat)));
	}

	@Test
	void aNewRoundWearsConditionsDownAndReportsTheExpired()
	{
		Combat combat = skirmish().withCondition(1, Condition.label("Shaken", OptionalInt.of(1)))
			.withCondition(2, Condition.label("Prone", OptionalInt.empty()));
		for (int turn = 0; turn < 3; turn++)
		{
			combat = combat.next().combat();
		}

		CombatAdvance advance = combat.next();

		assertEquals(List.of(new Expiry.ConditionEnded("Valeros", "Shaken")), advance.expired());
	}

	/** Seoni, acting, casts a spell lasting the given rounds. */
	private static Combat seoniCasting(OptionalInt rounds)
	{
		return skirmish().next().combat().withSpell(2, new CastSpell("Bless", rounds));
	}

	@Test
	void aSpellOutlastsTheRoundItWasCastIn()
	{
		Combat combat = seoniCasting(OptionalInt.of(1));
		for (int turn = 0; turn < 3; turn++)
		{
			combat = combat.next().combat();
		}

		assertEquals(List.of(new CastSpell("Bless", OptionalInt.of(1))), combat.combatant(2).orElseThrow().spells());
	}

	@Test
	void aSpellEndsAsItsCasterBeginsItsTurnOnceItsRoundsAreSpent()
	{
		Combat combat = seoniCasting(OptionalInt.of(1));
		for (int turn = 0; turn < 3; turn++)
		{
			combat = combat.next().combat();
		}

		CombatAdvance advance = combat.next();

		assertEquals(List.of(new Expiry.SpellEnded("Seoni", "Bless")), advance.expired());
	}

	@Test
	void theFirstToActWearsItsSpellsDownAsTheFightStarts()
	{
		Combat combat = Combat.empty().with(player(1, "Valeros")).withInitiative(1, 22)
			.withSpell(1, new CastSpell("Bless", OptionalInt.of(3)));

		assertEquals(List.of(new CastSpell("Bless", OptionalInt.of(2))), combat.started().combatant(1).orElseThrow().spells());
	}

	/** Seoni, acting, blesses Valeros and herself for one round. */
	private static Combat seoniBlessing()
	{
		return skirmish().next().combat().cast(2, Condition.label("Bless", OptionalInt.of(1)), List.of(1, 2));
	}

	private static List<String> conditionNames(Combat combat, int id)
	{
		return combat.combatant(id).orElseThrow().conditions().stream().map(Condition::name).toList();
	}

	@Test
	void aSpellLaysItsEffectOnItsTargetsUntilItEnds()
	{
		Combat combat = seoniBlessing();

		assertEquals(List.of(Condition.label("Bless", OptionalInt.empty())), combat.combatant(1).orElseThrow().conditions());
	}

	@Test
	void anEndedSpellLiftsItsEffectFromItsTargets()
	{
		Combat combat = seoniBlessing();
		for (int turn = 0; turn < 4; turn++)
		{
			combat = combat.next().combat();
		}

		assertEquals(List.of(List.of(), List.of()), List.of(conditionNames(combat, 1), conditionNames(combat, 2)));
	}

	@Test
	void aDroppedSpellLiftsItsEffect()
	{
		Combat combat = seoniBlessing().withoutSpell(2, "Bless");

		assertEquals(List.of(), conditionNames(combat, 1));
	}

	@Test
	void anotherCastersSpellOfTheSameNameKeepsTheEffect()
	{
		Combat combat = seoniBlessing().cast(3, Condition.label("Bless", OptionalInt.of(5)), List.of(1))
			.withoutSpell(2, "Bless");

		assertEquals(List.of("Bless"), conditionNames(combat, 1));
	}

	@Test
	void aSpellsEffectCountsDownAloneOnceItsCasterLeaves()
	{
		Combat combat = seoniBlessing().removed(2);

		assertEquals(List.of(Condition.label("Bless", OptionalInt.of(1))), combat.combatant(1).orElseThrow().conditions());
	}

	@Test
	void aSpellOnAPlayerIsOnlyALabelThere()
	{
		Combat combat = skirmish().next().combat().cast(2, new Condition("Shaken", Optional.of(new BonusSourceKey(
			BonusSourceKey.TEMPLATE, "", "Shaken")), OptionalInt.of(2)), List.of(1, 3));

		assertEquals(List.of(false, true), List.of(combat.combatant(1).orElseThrow().conditions().getFirst().bonus().isPresent(),
			combat.combatant(3).orElseThrow().conditions().getFirst().bonus().isPresent()));
	}

	@Test
	void aSpellWithoutDurationLastsUntilDropped()
	{
		Combat combat = seoniCasting(OptionalInt.empty());
		for (int turn = 0; turn < 8; turn++)
		{
			combat = combat.next().combat();
		}

		assertEquals(List.of(), combat.withoutSpell(2, "bless").combatant(2).orElseThrow().spells());
	}

	@Test
	void conditionsLastTheirRoundsOnlyOncePerRoundForADualInitiative()
	{
		Combatant agile = Combatant.monster(5, "Agile Wolf", "wolf", GOBLIN, 13, 2, true);
		Combat combat = skirmish().with(agile).withInitiative(5, 30)
			.withCondition(5, Condition.label("Haste", OptionalInt.of(2)));
		for (int turn = 0; turn < 6; turn++)
		{
			combat = combat.next().combat();
		}

		assertEquals(OptionalInt.of(1), combat.combatant(5).orElseThrow().conditions().get(0).roundsLeft());
	}

	@Test
	void aDualInitiativeActsAgainTwentyLower()
	{
		Combatant agile = Combatant.monster(5, "Agile Wolf", "wolf", GOBLIN, 13, 2, true);

		Combat combat = skirmish().with(agile).withInitiative(5, 33);

		assertEquals(List.of("Agile Wolf", "Valeros", "Seoni", "Agile Wolf'", "Goblin 3", "Goblin 4"), names(combat));
	}

	@Test
	void aSecondTurnBelowEveryoneComesLast()
	{
		Combatant agile = Combatant.monster(5, "Agile Wolf", "wolf", GOBLIN, 13, 2, true);

		Combat combat = skirmish().with(agile).withInitiative(5, 12);

		assertEquals("Agile Wolf'", names(combat).get(names(combat).size() - 1));
	}

	@Test
	void aConjuredCreatureActsRightAfterItsConjurer()
	{
		Combatant archon = Combatant.player(9, "Hound Archon 1", Optional.empty()).withRoundsLeft(OptionalInt.of(3));

		Combat combat = skirmish().summoned(2, List.of(archon));

		assertEquals(List.of("Valeros", "Seoni", "Hound Archon 1", "Goblin 3", "Goblin 4"), names(combat));
	}

	@Test
	void aPlayerGivenAnInitiativeAfterConjuringTakesItsPlace()
	{
		Combatant azata = Combatant.player(9, "Lillend Azata 1", Optional.empty());
		Combat combat = skirmish().with(player(5, "Shalendra")).summoned(5, List.of(azata));

		Combat placed = combat.withInitiative(5, 18);

		assertEquals(List.of("Valeros", "Shalendra", "Lillend Azata 1", "Seoni", "Goblin 3", "Goblin 4"),
			names(placed));
	}

	@Test
	void aConjuredCreatureFollowsItsConjurersNewInitiative()
	{
		Combatant azata = Combatant.player(9, "Lillend Azata 1", Optional.empty());
		Combat combat = skirmish().summoned(2, List.of(azata));

		Combat placed = combat.withInitiative(2, 25);

		assertEquals(List.of("Seoni", "Lillend Azata 1", "Valeros", "Goblin 3", "Goblin 4"), names(placed));
	}

	@Test
	void aConjuredCreatureLeavesWhenItsDurationEnds()
	{
		Combatant archon = Combatant.player(9, "Hound Archon 1", Optional.empty()).withRoundsLeft(OptionalInt.of(1));
		Combat combat = skirmish().summoned(2, List.of(archon));
		for (int turn = 0; turn < 4; turn++)
		{
			combat = combat.next().combat();
		}

		CombatAdvance advance = combat.next();

		assertEquals(List.of(new Expiry.CreatureLeft("Hound Archon 1")), advance.expired());
	}

	@Test
	void aConjuredCreatureIsGoneOnceItsDurationEnds()
	{
		Combatant archon = Combatant.player(9, "Hound Archon 1", Optional.empty()).withRoundsLeft(OptionalInt.of(1));
		Combat combat = skirmish().summoned(2, List.of(archon));
		for (int turn = 0; turn < 5; turn++)
		{
			combat = combat.next().combat();
		}

		assertTrue(combat.combatant(9).isEmpty());
	}

	@Test
	void delayingLeavesTheOrderAndPassesTheTurn()
	{
		Combat combat = skirmish().delaying(1);

		assertEquals(List.of("Seoni", "Seoni"), List.of(acting(combat), names(combat).get(0)));
	}

	@Test
	void resumingActsNowWithTheScoreOfWhoeverJustActed()
	{
		Combat combat = skirmish().delaying(1).next().combat();

		Combat resumed = combat.resuming(1);

		assertEquals(List.of("Valeros", "15"),
			List.of(acting(resumed), String.valueOf(resumed.combatant(1).orElseThrow().initiative().score().getAsInt())));
	}

	@Test
	void afterResumingTheInterruptedTurnFollows()
	{
		Combat combat = skirmish().delaying(1).next().combat().resuming(1);

		assertEquals("Goblin 3", acting(combat.next().combat()));
	}

	@Test
	void removingWhoeverActsPassesTheTurnOn()
	{
		Combat combat = skirmish().next().combat().removed(2);

		assertEquals("Goblin 3", acting(combat));
	}

	@Test
	void removingTheLastToActEndsTheRoundOnNext()
	{
		Combat combat = skirmish();
		for (int turn = 0; turn < 3; turn++)
		{
			combat = combat.next().combat();
		}

		Combat after = combat.removed(4).next().combat();

		assertEquals(List.of(2, "Valeros"), List.of(after.round(), acting(after)));
	}

	@Test
	void movingKeepsTheNewPlaceWhenInitiativesAreSortedAgain()
	{
		Combat combat = skirmish().moved(2, 0).withInitiative(3, 11);

		assertEquals("Seoni", names(combat).get(0));
	}

	@Test
	void damageNeverTurnsIntoHealing()
	{
		Combat combat = skirmish().damaged(3, 4).healed(3, 10);

		assertEquals(6, combat.combatant(3).orElseThrow().health().orElseThrow().remaining());
	}

	@Test
	void damageCanDropAMonsterBelowZero()
	{
		Combat combat = skirmish().damaged(3, 9);

		assertEquals(-3, combat.combatant(3).orElseThrow().health().orElseThrow().remaining());
	}

	@Test
	void aConditionOfTheSameNameIsReplaced()
	{
		Combat combat = skirmish().withCondition(1, Condition.label("Shaken", OptionalInt.of(1)))
			.withCondition(1, Condition.label("Shaken", OptionalInt.of(4)));

		assertEquals(List.of(Condition.label("Shaken", OptionalInt.of(4))), combat.combatant(1).orElseThrow().conditions());
	}

	@Test
	void aConditionCanBeTakenOff()
	{
		Combat combat = skirmish().withCondition(1, Condition.label("Shaken", OptionalInt.of(1))).withoutCondition(1,
			"Shaken");

		assertEquals(List.of(), combat.combatant(1).orElseThrow().conditions());
	}

	@Test
	void aPlayerIsNotTrackedForHitPoints()
	{
		assertEquals(CombatantKind.PLAYER, skirmish().combatant(1).orElseThrow().kind());
	}

	@Test
	void aNewCombatantTakesTheNextFreeId()
	{
		assertEquals(5, skirmish().nextId());
	}
}
