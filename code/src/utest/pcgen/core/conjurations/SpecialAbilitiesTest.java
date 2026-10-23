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

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * A creature's special abilities, read from the lists PCGen's output tokens write.
 */
class SpecialAbilitiesTest
{
	@Test
	void aListedAbilityStandsAlone()
	{
		assertEquals(List.of("Sneak Attack", "Darkvision"), SpecialAbilities.split("Sneak Attack, Darkvision"));
	}

	@Test
	void aCommaBetweenAnAbilitysChoicesDoesNotSplitIt()
	{
		assertEquals(List.of("Energy Resistance (Acid, Cold)", "Darkvision"),
			SpecialAbilities.split("Energy Resistance (Acid, Cold), Darkvision"));
	}

	@Test
	void anEmptyListHoldsNoAbility()
	{
		assertEquals(List.of(), SpecialAbilities.split(" "));
	}

	@Test
	void theJoinedAbilitiesListAttacksThenQualitiesThenDefenses()
	{
		SpecialAbilities abilities = new SpecialAbilities(List.of("Sneak Attack"), List.of("Darkvision"),
			List.of("DR 10/Good", "SR 17"));

		assertEquals("Sneak Attack, Darkvision, DR 10/Good, SR 17", abilities.joined());
	}
}
