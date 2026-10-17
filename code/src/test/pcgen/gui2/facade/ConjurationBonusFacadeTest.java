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
package pcgen.gui2.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalInt;

import pcgen.core.PlayerCharacter;
import pcgen.core.conjurations.BestiarySources;
import pcgen.facade.core.ConjurationSheetFacade;
import pcgen.facade.core.ConjuredCreatureView;

import org.junit.jupiter.api.Test;

/**
 * Bonuses laid on conjured creatures through the conjuration page, on real
 * creatures of the Bestiary.
 */
class ConjurationBonusFacadeTest
{
	private static final String ARCHON = "Archon (Hound)";
	private static final String PRAYER = "SPELL=Prayer";
	private static final String BULLS_STRENGTH = "SPELL=Bull's Strength";

	private static PlayerCharacter conjurer()
	{
		PlayerCharacter character = BestiarySources.newCharacter("Conjurer");
		character.setConjurationCatalog("## Summon Monster IV\nsummon: Archon (Hound)\n");
		return character;
	}

	private static String toHitOf(ConjuredCreatureView creature)
	{
		return creature.stats().attacks().getFirst().toHit();
	}

	@Test
	void aCreatureConjuredLaterReceivesTheGroupBonuses()
	{
		ConjurationSheetFacade sheet = new ConjurationSheetFacadeImpl(conjurer());
		sheet.addGroupBonus(PRAYER, OptionalInt.empty());

		int number = sheet.conjure(ARCHON);

		assertEquals("+10/+5", toHitOf(sheet.viewOf(ARCHON, number).orElseThrow()));
	}

	@Test
	void addingAGroupBonusRefreshesEveryCreature()
	{
		ConjurationSheetFacade sheet = new ConjurationSheetFacadeImpl(conjurer());
		sheet.conjure(ARCHON);
		sheet.conjure(ARCHON);

		List<ConjuredCreatureView> refreshed = sheet.addGroupBonus(PRAYER, OptionalInt.empty());

		assertEquals(List.of("+10/+5", "+10/+5"), refreshed.stream().map(ConjurationBonusFacadeTest::toHitOf).toList());
	}

	@Test
	void aCreatureBonusRefreshesOnlyThatCreature()
	{
		ConjurationSheetFacade sheet = new ConjurationSheetFacadeImpl(conjurer());
		sheet.conjure(ARCHON);
		sheet.conjure(ARCHON);

		List<ConjuredCreatureView> refreshed = sheet.addBonus(ARCHON, 2, BULLS_STRENGTH, OptionalInt.empty());

		assertEquals(List.of(2), refreshed.stream().map(ConjuredCreatureView::number).toList());
	}

	@Test
	void aCreatureBonusKeepsItsUnbuffedNumbersForComparison()
	{
		ConjurationSheetFacade sheet = new ConjurationSheetFacadeImpl(conjurer());
		sheet.conjure(ARCHON);

		ConjuredCreatureView strong = sheet.addBonus(ARCHON, 1, BULLS_STRENGTH, OptionalInt.empty()).getFirst();

		assertEquals(List.of("+9/+4", "+11/+6"),
			List.of(strong.baseStats().attacks().getFirst().toHit(), toHitOf(strong)));
	}

	@Test
	void removingAGroupBonusGivesBackTheNumbers()
	{
		ConjurationSheetFacade sheet = new ConjurationSheetFacadeImpl(conjurer());
		sheet.conjure(ARCHON);
		sheet.addGroupBonus(PRAYER, OptionalInt.empty());

		ConjuredCreatureView archon = sheet.removeGroupBonus(PRAYER).getFirst();

		assertEquals("+9/+4", toHitOf(archon));
	}

	@Test
	void groupChoicesHoldPrayer()
	{
		ConjurationSheetFacade sheet = new ConjurationSheetFacadeImpl(conjurer());

		assertTrue(sheet.groupBonusChoices().stream().anyMatch(choice -> PRAYER.equals(choice.key())));
	}

	@Test
	void anUnknownKeyChangesNothing()
	{
		PlayerCharacter character = conjurer();
		ConjurationSheetFacade sheet = new ConjurationSheetFacadeImpl(character);
		sheet.conjure(ARCHON);

		List<ConjuredCreatureView> refreshed = sheet.addGroupBonus("SPELL=No Such Spell", OptionalInt.empty());

		assertEquals(List.of(List.of(), List.of()),
			List.of(refreshed, character.getDisplay().getConjurationSession().groupBonuses()));
	}

	@Test
	void layingABonusMarksTheCharacterModified()
	{
		PlayerCharacter character = conjurer();
		ConjurationSheetFacade sheet = new ConjurationSheetFacadeImpl(character);
		character.setDirty(false);

		sheet.addGroupBonus(PRAYER, OptionalInt.empty());

		assertTrue(character.isDirty());
	}
}
