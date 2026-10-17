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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * How a source of temporary bonuses is named, in a saved character and on the page.
 */
class BonusSourceKeyTest
{

	@Test
	void writesASpellAsItsKindAndKey()
	{
		assertEquals("SPELL=Prayer", new BonusSourceKey(BonusSourceKey.SPELL, "", "Prayer").text());
	}

	@Test
	void writesAnAbilityWithItsCategory()
	{
		assertEquals("ABILITY=FEAT=Power Attack",
			new BonusSourceKey(BonusSourceKey.ABILITY, "FEAT", "Power Attack").text());
	}

	@Test
	void readsBackEveryKindItWrites()
	{
		for (BonusSourceKey key : new BonusSourceKey[]{new BonusSourceKey(BonusSourceKey.SPELL, "", "Prayer"),
			new BonusSourceKey(BonusSourceKey.ABILITY, "Special Ability", "Rage (Ex)"),
			new BonusSourceKey(BonusSourceKey.TEMPLATE, "", "Celestial Creature"),
			new BonusSourceKey(BonusSourceKey.EQUIPMENT, "", "Potion of Bull's Strength")})
		{
			assertEquals(Optional.of(key), BonusSourceKey.parse(key.text()));
		}
	}

	@Test
	void keepsAnEqualsSignInsideAKey()
	{
		BonusSourceKey key = new BonusSourceKey(BonusSourceKey.SPELL, "", "Odd=Name");

		assertEquals(Optional.of(key), BonusSourceKey.parse(key.text()));
	}

	@Test
	void parseRefusesAnUnknownKind()
	{
		assertEquals(Optional.empty(), BonusSourceKey.parse("FEAT=Power Attack"));
	}

	@Test
	void parseRefusesAnAbilityWithoutCategory()
	{
		assertEquals(Optional.empty(), BonusSourceKey.parse("ABILITY=Power Attack"));
	}

	@Test
	void refusesAnUnknownKind()
	{
		assertThrows(IllegalArgumentException.class, () -> new BonusSourceKey("FEAT", "", "Power Attack"));
	}

	@Test
	void refusesAnAbilityWithoutCategory()
	{
		assertThrows(IllegalArgumentException.class, () -> new BonusSourceKey(BonusSourceKey.ABILITY, "", "Rage"));
	}

	@Test
	void refusesACategoryOnAnythingButAnAbility()
	{
		assertThrows(IllegalArgumentException.class, () -> new BonusSourceKey(BonusSourceKey.SPELL, "FEAT", "Prayer"));
	}
}
