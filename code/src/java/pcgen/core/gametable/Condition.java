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

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

import pcgen.core.conjurations.BonusSourceKey;

/**
 * A condition, temporary bonus or negative property on a combatant, for a number of
 * rounds or until taken off. One drawn from the data changes a monster's numbers;
 * on anyone else, and when it has no rules object, it is only a label.
 *
 * @param name       what the game master reads. Never blank.
 * @param bonus      the rules object granting it, when it comes from the data
 * @param roundsLeft rounds before it wears off, at least one; empty when it lasts
 */
public record Condition(String name, Optional<BonusSourceKey> bonus, OptionalInt roundsLeft)
{
	public Condition
	{
		if (Objects.requireNonNull(name).isBlank())
		{
			throw new IllegalArgumentException("A condition has a name");
		}
		name = name.strip();
		Objects.requireNonNull(bonus);
		Objects.requireNonNull(roundsLeft);
		if (roundsLeft.isPresent() && roundsLeft.getAsInt() < 1)
		{
			throw new IllegalArgumentException("A condition lasts at least one round: " + roundsLeft);
		}
	}

	/**
	 * @param name       what the game master reads.
	 * @param roundsLeft rounds before it wears off; empty when it lasts.
	 * @return a condition that changes no number.
	 */
	public static Condition label(String name, OptionalInt roundsLeft)
	{
		return new Condition(name, Optional.empty(), roundsLeft);
	}

	/**
	 * @return this condition one round later, or nothing when it has worn off.
	 */
	public Optional<Condition> ticked()
	{
		if (roundsLeft.isEmpty())
		{
			return Optional.of(this);
		}
		int left = roundsLeft.getAsInt() - 1;
		return (left < 1) ? Optional.empty() : Optional.of(new Condition(name, bonus, OptionalInt.of(left)));
	}
}
