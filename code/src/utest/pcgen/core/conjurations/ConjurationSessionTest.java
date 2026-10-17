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
package pcgen.core.conjurations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

/**
 * Behaviour of the creatures a character has conjured into a fight.
 */
class ConjurationSessionTest
{

	private static final String ARCHON = "Hound Archon";
	private static final String LANTERN = "Lantern Archon";

	private static ConjurationSession conjured(ConjurationSession session, String creature)
	{
		return session.with(session.arrival(creature));
	}

	private static ConjurationSession threeArchons()
	{
		return conjured(conjured(conjured(ConjurationSession.empty(), ARCHON), ARCHON), ARCHON);
	}

	private static ConjuredCreature unhurt(String creature, int number)
	{
		return new ConjuredCreature(creature, number, 0, OptionalInt.empty());
	}

	@Test
	void anEmptySessionHasNobodyInPlay()
	{
		assertEquals(List.of(), ConjurationSession.empty().conjured());
	}

	@Test
	void numbersEachNewArrivalOfTheSameCreatureOneHigher()
	{
		assertEquals(List.of(1, 2, 3), threeArchons().conjured().stream().map(ConjuredCreature::number).toList());
	}

	@Test
	void reusesTheLowestNumberADismissalFreed()
	{
		ConjurationSession session = threeArchons().dismiss(ARCHON, 2);

		assertEquals(2, session.arrival(ARCHON).number());
	}

	@Test
	void numbersEachCreatureOnItsOwn()
	{
		ConjurationSession session = conjured(ConjurationSession.empty(), ARCHON);

		assertEquals(1, session.arrival(LANTERN).number());
	}

	@Test
	void numbersACreatureWhateverTheCaseItsNameIsWrittenIn()
	{
		ConjurationSession session = conjured(ConjurationSession.empty(), ARCHON);

		assertEquals(2, session.arrival("hound archon").number());
	}

	@Test
	void anArrivalIsUnhurt()
	{
		assertEquals(0, ConjurationSession.empty().arrival(ARCHON).damage());
	}

	@Test
	void anArrivalStaysForTheSessionsDuration()
	{
		ConjurationSession session = ConjurationSession.empty().withDuration(12);

		assertEquals(OptionalInt.of(12), session.arrival(ARCHON).roundsLeft());
	}

	@Test
	void anArrivalCountsNoRoundsWhenTheDurationIsZero()
	{
		assertEquals(OptionalInt.empty(), ConjurationSession.empty().arrival(ARCHON).roundsLeft());
	}

	@Test
	void recordsTheDamageOfThatCreatureOnly()
	{
		ConjurationSession session = threeArchons().withDamage(ARCHON, 2, 14);

		assertEquals(List.of(0, 14, 0), session.conjured().stream().map(ConjuredCreature::damage).toList());
	}

	@Test
	void changesNothingForACreatureNotInPlay()
	{
		ConjurationSession session = threeArchons();

		assertEquals(session, session.withDamage(LANTERN, 1, 5));
	}

	@Test
	void setsTheRoundsLeftOfThatCreatureOnly()
	{
		ConjurationSession session = threeArchons().withRoundsLeft(ARCHON, 1, 4);

		assertEquals(List.of(OptionalInt.of(4), OptionalInt.empty(), OptionalInt.empty()),
			session.conjured().stream().map(ConjuredCreature::roundsLeft).toList());
	}

	@Test
	void takesARoundFromEveryCountedCreatureAtTheNextRound()
	{
		ConjurationSession session = new ConjurationSession(0, List.of(
			new ConjuredCreature(ARCHON, 1, 0, OptionalInt.of(12)),
			new ConjuredCreature(ARCHON, 2, 0, OptionalInt.of(0)),
			unhurt(LANTERN, 1)));

		assertEquals(List.of(OptionalInt.of(11), OptionalInt.of(0), OptionalInt.empty()),
			session.nextRound().conjured().stream().map(ConjuredCreature::roundsLeft).toList());
	}

	@Test
	void dismissesThatCreatureAndKeepsTheOthers()
	{
		ConjurationSession session = threeArchons().dismiss(ARCHON, 2);

		assertEquals(List.of(1, 3), session.conjured().stream().map(ConjuredCreature::number).toList());
	}

	@Test
	void dismissingEveryoneKeepsTheDuration()
	{
		ConjurationSession session = threeArchons().withDuration(12).dismissAll();

		assertEquals(new ConjurationSession(12, List.of()), session);
	}

	@Test
	void hasNothingToSaveWithNobodyInPlayAndNoDuration()
	{
		assertTrue(ConjurationSession.empty().isEmpty());
	}

	@Test
	void hasSomethingToSaveOnceADurationIsSet()
	{
		assertFalse(ConjurationSession.empty().withDuration(12).isEmpty());
	}

	@Test
	void hasSomethingToSaveWhileACreatureIsInPlay()
	{
		assertFalse(conjured(ConjurationSession.empty(), ARCHON).isEmpty());
	}

	@Test
	void refusesTwoCreaturesWithTheSameNameAndNumber()
	{
		assertThrows(IllegalArgumentException.class,
			() -> new ConjurationSession(0, List.of(unhurt(ARCHON, 1), unhurt("hound archon", 1))));
	}

	@Test
	void refusesANumberBelowOne()
	{
		assertThrows(IllegalArgumentException.class, () -> unhurt(ARCHON, 0));
	}

	@Test
	void refusesNegativeDamage()
	{
		assertThrows(IllegalArgumentException.class,
			() -> new ConjuredCreature(ARCHON, 1, -1, OptionalInt.empty()));
	}

	@Test
	void refusesNegativeRoundsLeft()
	{
		assertThrows(IllegalArgumentException.class,
			() -> new ConjuredCreature(ARCHON, 1, 0, OptionalInt.of(-1)));
	}

	@Test
	void refusesACreatureWithNoName()
	{
		assertThrows(IllegalArgumentException.class, () -> unhurt(" ", 1));
	}

	@Test
	void refusesANegativeDuration()
	{
		assertThrows(IllegalArgumentException.class, () -> new ConjurationSession(-1, List.of()));
	}

	@Test
	void cannotBeChangedThroughTheListPassedIn()
	{
		List<ConjuredCreature> mutable = new ArrayList<>(List.of(unhurt(ARCHON, 1)));
		ConjurationSession session = new ConjurationSession(0, mutable);

		mutable.clear();

		assertEquals(1, session.conjured().size());
	}

	private static final AppliedBonus PRAYER =
		new AppliedBonus(new BonusSourceKey(BonusSourceKey.SPELL, "", "Prayer"), OptionalInt.empty());
	private static final AppliedBonus BULLS_STRENGTH =
		new AppliedBonus(new BonusSourceKey(BonusSourceKey.SPELL, "", "Bull's Strength"), OptionalInt.empty());

	@Test
	void addingAGroupBonusTwiceKeepsOne()
	{
		ConjurationSession session = ConjurationSession.empty().withGroupBonus(PRAYER).withGroupBonus(PRAYER);

		assertEquals(List.of(PRAYER), session.groupBonuses());
	}

	@Test
	void addingAGroupBonusAgainReplacesItsValue()
	{
		AppliedBonus courage = new AppliedBonus(new BonusSourceKey(BonusSourceKey.ABILITY, "Special Ability",
			"Inspire Courage"), OptionalInt.of(1));
		AppliedBonus strongerCourage = new AppliedBonus(courage.source(), OptionalInt.of(2));

		ConjurationSession session =
			ConjurationSession.empty().withGroupBonus(courage).withGroupBonus(strongerCourage);

		assertEquals(List.of(strongerCourage), session.groupBonuses());
	}

	@Test
	void removingAGroupBonusKeepsTheSameBonusOnACreature()
	{
		ConjurationSession session = threeArchons().withGroupBonus(PRAYER).withBonus(ARCHON, 2, PRAYER)
			.withoutGroupBonus(PRAYER.source());

		assertEquals(List.of(PRAYER), session.conjured().get(1).bonuses());
	}

	@Test
	void aCreatureBonusStaysOnThatCreatureOnly()
	{
		ConjurationSession session = threeArchons().withBonus(ARCHON, 2, BULLS_STRENGTH);

		assertEquals(List.of(List.of(), List.of(BULLS_STRENGTH), List.of()),
			session.conjured().stream().map(ConjuredCreature::bonuses).toList());
	}

	@Test
	void removesACreatureBonus()
	{
		ConjurationSession session =
			threeArchons().withBonus(ARCHON, 2, BULLS_STRENGTH).withoutBonus(ARCHON, 2, BULLS_STRENGTH.source());

		assertEquals(List.of(), session.conjured().get(1).bonuses());
	}

	@Test
	void dismissingACreatureDropsItsBonuses()
	{
		ConjurationSession session = threeArchons().withBonus(ARCHON, 2, BULLS_STRENGTH).dismiss(ARCHON, 2);
		ConjurationSession again = conjured(session, ARCHON);

		assertEquals(List.of(), again.conjured().getLast().bonuses());
	}

	@Test
	void dismissAllKeepsTheGroupBonuses()
	{
		ConjurationSession session = threeArchons().withGroupBonus(PRAYER).dismissAll();

		assertEquals(List.of(PRAYER), session.groupBonuses());
	}

	@Test
	void theFightKeepsItsGroupBonusesThroughItsChanges()
	{
		ConjurationSession session = threeArchons().withGroupBonus(PRAYER).withDamage(ARCHON, 1, 5).nextRound()
			.withDuration(4).withRoundsLeft(ARCHON, 1, 2);

		assertEquals(List.of(PRAYER), session.groupBonuses());
	}

	@Test
	void aSessionWithOnlyAGroupBonusIsNotEmpty()
	{
		assertFalse(ConjurationSession.empty().withGroupBonus(PRAYER).isEmpty());
	}
}
