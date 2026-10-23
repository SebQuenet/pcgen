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

import pcgen.core.conjurations.AppliedBonus;
import pcgen.core.conjurations.BonusSourceKey;

/**
 * A condition, temporary bonus or negative property on a combatant, for a number of
 * rounds or until taken off. One drawn from the data changes a monster's numbers;
 * on anyone else, and when it has no rules object, it is only a label.
 *
 * @param name       what the game master reads. Never blank.
 * @param bonus      the rules object granting it, when it comes from the data
 * @param roundsLeft  rounds before it wears off, at least one; empty when it lasts
 * @param chosenValue the value chosen for a bonus that asks for one, such as the +2 of an Inspire Courage
 */
public record Condition(String name, Optional<BonusSourceKey> bonus, OptionalInt roundsLeft, OptionalInt chosenValue)
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
		Objects.requireNonNull(chosenValue);
		if (roundsLeft.isPresent() && roundsLeft.getAsInt() < 1)
		{
			throw new IllegalArgumentException("A condition lasts at least one round: " + roundsLeft);
		}
	}

	/** A condition whose bonus asks no value. */
	public Condition(String name, Optional<BonusSourceKey> bonus, OptionalInt roundsLeft)
	{
		this(name, bonus, roundsLeft, OptionalInt.empty());
	}

	/**
	 * @return the bonus it lays on a monster, with its chosen value; empty when it is only a label.
	 */
	public Optional<AppliedBonus> applied()
	{
		return bonus.map(source -> new AppliedBonus(source, chosenValue));
	}

	/**
	 * @return the same condition as a mere label, as it goes on a combatant whose numbers PCGen does not hold.
	 */
	public Condition asLabel()
	{
		return label(name, roundsLeft);
	}

	/**
	 * @return the same condition lasting until taken off, as a spell's effect lasts until the spell ends.
	 */
	public Condition lasting()
	{
		return new Condition(name, bonus, OptionalInt.empty(), chosenValue);
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
		return (left < 1) ? Optional.empty() : Optional.of(new Condition(name, bonus, OptionalInt.of(left), chosenValue));
	}
}
