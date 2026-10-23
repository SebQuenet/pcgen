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

import java.util.OptionalInt;

/**
 * What a Bluff check must reach to feint a creature in combat: 10 plus its base attack bonus
 * and Wisdom modifier, or 10 plus its Sense Motive bonus when it is trained and that is higher.
 * The feinter's penalty against a nonhumanoid or an animal mind is counted in the difficulty.
 */
public sealed interface FeintDefense
{
	/** The highest Intelligence a creature of animal mind has. */
	int ANIMAL_INTELLIGENCE = 2;

	/** A creature lacking an Intelligence score cannot be feinted. */
	record Impossible() implements FeintDefense
	{
	}

	/**
	 * @param base      10 plus the higher of its base attack bonus with its Wisdom modifier, or its trained Sense Motive
	 * @param hindrance what makes it harder to feint than a humanoid
	 */
	record Difficulty(int base, Hindrance hindrance) implements FeintDefense
	{
		/** @return what the Bluff check must reach. */
		public int difficulty()
		{
			return base + hindrance.penalty();
		}
	}

	/** What makes a creature harder to feint, with the penalty the feinter takes on Bluff. */
	enum Hindrance
	{
		NONE(0), NONHUMANOID(4), ANIMAL_MIND(8);

		private final int penalty;

		Hindrance(int penalty)
		{
			this.penalty = penalty;
		}

		public int penalty()
		{
			return penalty;
		}
	}

	/**
	 * @param baseAttackBonus    its base attack bonus
	 * @param wisdomModifier     its Wisdom modifier
	 * @param trainedSenseMotive its Sense Motive bonus, when it has ranks in it
	 * @param intelligence       its Intelligence score, empty when it has none
	 * @param humanoid           whether it is of the humanoid type
	 * @return how hard it is to feint
	 */
	static FeintDefense of(int baseAttackBonus, int wisdomModifier, OptionalInt trainedSenseMotive,
		OptionalInt intelligence, boolean humanoid)
	{
		if (intelligence.isEmpty())
		{
			return new Impossible();
		}
		int base = 10 + Math.max(baseAttackBonus + wisdomModifier, trainedSenseMotive.orElse(Integer.MIN_VALUE));
		Hindrance hindrance = (intelligence.getAsInt() <= ANIMAL_INTELLIGENCE) ? Hindrance.ANIMAL_MIND
			: humanoid ? Hindrance.NONE : Hindrance.NONHUMANOID;
		return new Difficulty(base, hindrance);
	}
}
