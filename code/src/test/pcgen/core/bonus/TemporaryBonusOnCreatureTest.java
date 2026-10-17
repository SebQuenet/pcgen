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
package pcgen.core.bonus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import pcgen.cdom.base.CDOMObject;
import pcgen.core.PlayerCharacter;
import pcgen.core.conjurations.BestiarySources;
import pcgen.core.conjurations.BonusSourceKey;
import pcgen.core.conjurations.CreatureAttack;
import pcgen.core.conjurations.CreatureStats;
import pcgen.core.conjurations.SummonTemplate;
import pcgen.core.conjurations.SummonedCreatureBuilder;
import pcgen.session.HeadlessUIDelegate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Temporary bonuses laid on a creature built from the Bestiary, counted by
 * PCGen's own rules.
 */
class TemporaryBonusOnCreatureTest
{
	private static final BonusSourceKey PRAYER = new BonusSourceKey(BonusSourceKey.SPELL, "", "Prayer");
	private static final BonusSourceKey DIVINE_FAVOR = new BonusSourceKey(BonusSourceKey.SPELL, "", "Divine Favor");
	private static final BonusSourceKey BULLS_STRENGTH =
		new BonusSourceKey(BonusSourceKey.SPELL, "", "Bull's Strength");
	private static final BonusSourceKey HASTE = new BonusSourceKey(BonusSourceKey.SPELL, "", "Haste");
	private static final BonusSourceKey INSPIRE_COURAGE =
		new BonusSourceKey(BonusSourceKey.ABILITY, "Internal", "Temp Bonus ~ Inspire Courage");
	private static final BonusSourceKey BLOOD_FRENZY =
		new BonusSourceKey(BonusSourceKey.ABILITY, "Special Ability", "Sahuagin ~ Blood Frenzy");

	private static SummonedCreatureBuilder builder;

	@BeforeAll
	static void loadTheBestiary()
	{
		builder = new SummonedCreatureBuilder(BestiarySources.campaigns(), new HeadlessUIDelegate());
	}

	private static PlayerCharacter houndArchon()
	{
		return builder.build(new SummonTemplate("Archon (Hound)", List.of(), 1), false);
	}

	private static CDOMObject source(BonusSourceKey key)
	{
		return TemporaryBonusSources.find(key).orElseThrow(() -> new AssertionError("Not loaded: " + key.text()));
	}

	private static CreatureAttack greatswordWith(List<BonusSourceKey> bonuses, OptionalInt value)
	{
		PlayerCharacter archon = houndArchon();
		for (BonusSourceKey bonus : bonuses)
		{
			TemporaryBonusApplier.apply(archon, source(bonus), value);
		}
		return builder.statsOf(archon).attacks().getFirst();
	}

	private static List<String> hitAndDamage(CreatureAttack attack)
	{
		return List.of(attack.toHit(), attack.damage());
	}

	@Test
	void prayerAddsOneToHitAndDamage()
	{
		assertEquals(List.of("+10/+5", "2d6+4"), hitAndDamage(greatswordWith(List.of(PRAYER), OptionalInt.empty())));
	}

	@Test
	void bullsStrengthAddsThreeDamageTwoHanded()
	{
		assertEquals(List.of("+11/+6", "2d6+6"),
			hitAndDamage(greatswordWith(List.of(BULLS_STRENGTH), OptionalInt.empty())));
	}

	@Test
	void hasteAddsAnAttackAndADodgePoint()
	{
		PlayerCharacter archon = houndArchon();
		TemporaryBonusApplier.apply(archon, source(HASTE), OptionalInt.empty());
		CreatureStats hasted = builder.statsOf(archon);

		assertEquals(List.of(20, "+10/+10/+5"), List.of(hasted.armorClass(), hasted.attacks().getFirst().toHit()));
	}

	@Test
	void twoLuckBonusesDoNotStack()
	{
		assertEquals("+10/+5", greatswordWith(List.of(PRAYER, DIVINE_FAVOR), OptionalInt.of(3)).toHit());
	}

	@Test
	void inspireCourageUsesTheChosenValue()
	{
		assertEquals(List.of("+11/+6", "2d6+5"),
			hitAndDamage(greatswordWith(List.of(INSPIRE_COURAGE), OptionalInt.of(2))));
	}

	@Test
	void removingABonusGivesBackTheNumbersWithoutIt()
	{
		PlayerCharacter archon = houndArchon();
		TemporaryBonusApplier.apply(archon, source(PRAYER), OptionalInt.empty());
		TemporaryBonusApplier.remove(archon, source(PRAYER));

		assertEquals("+9/+4", builder.statsOf(archon).attacks().getFirst().toHit());
	}

	@Test
	void inspireCourageAsksForAValueFromOneToFour()
	{
		assertEquals(Optional.of(new IntRange(1, 4, "Bonus Number Applied")),
			TemporaryBonusApplier.choiceRange(houndArchon(), source(INSPIRE_COURAGE)));
	}

	@Test
	void prayerAsksForNoValue()
	{
		assertEquals(Optional.empty(), TemporaryBonusApplier.choiceRange(houndArchon(), source(PRAYER)));
	}

	@Test
	void keyOfFindsTheSameSpellBack()
	{
		assertEquals(PRAYER, TemporaryBonusSources.keyOf(source(PRAYER)));
	}

	@Test
	void keyOfFindsTheSameAbilityBack()
	{
		assertEquals(INSPIRE_COURAGE, TemporaryBonusSources.keyOf(source(INSPIRE_COURAGE)));
	}

	@Test
	void castableOnOthersHoldsPrayer()
	{
		assertTrue(TemporaryBonusSources.castableOnOthers().stream().map(TemporaryBonusSources::keyOf)
			.anyMatch(PRAYER::equals));
	}

	@Test
	void castableOnOthersLeavesOutWhatACreatureGivesOnlyItself()
	{
		assertFalse(TemporaryBonusSources.castableOnOthers().stream().map(TemporaryBonusSources::keyOf)
			.anyMatch(BLOOD_FRENZY::equals));
	}

	@Test
	void aCreatureCanLayItsOwnAbilityOnItself()
	{
		PlayerCharacter sahuagin = builder.build(new SummonTemplate("Sahuagin", List.of(), 1), false);

		assertTrue(TemporaryBonusSources.availableTo(sahuagin).stream().map(TemporaryBonusSources::keyOf)
			.anyMatch(BLOOD_FRENZY::equals));
	}
}
