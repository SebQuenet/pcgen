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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class AverageDamageTest
{
	@Test
	void countsBothIterativeAttacksAgainstTheChosenArmorClass()
	{
		assertEquals(7.5, AverageDamage.expectedAgainst(
			List.of(new CreatureAttack("Greatsword", "+9/+4", "2d6+3")), 20).orElseThrow(), 0.001);
	}

	@Test
	void naturalOneAndTwentyBoundTheChanceToHit()
	{
		assertEquals(0.5, AverageDamage.expectedAgainst(
			List.of(new CreatureAttack("Bite", "+100", "2d6+3")), 200).orElseThrow(), 0.001);
		assertEquals(9.5, AverageDamage.expectedAgainst(
			List.of(new CreatureAttack("Bite", "+100", "2d6+3")), 1).orElseThrow(), 0.001);
	}

	@Test
	void declinesToInventANumberForUnparsedDamage()
	{
		assertTrue(AverageDamage.expectedAgainst(List.of(new CreatureAttack("Breath", "+9", "special")), 20)
			.isEmpty());
	}
}
