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

import java.util.List;
import java.util.OptionalInt;

/**
 * One creature conjured into the fight. Three hound archons conjured together
 * are three of these, numbered 1 to 3, each with its own wounds.
 *
 * @param creature   the name of its stat line in the catalog. Never blank.
 * @param number     which of the creatures of that name it is, from 1.
 * @param damage     the damage it has taken, never negative. It may exceed its
 *                   hit points.
 * @param roundsLeft how many rounds it stays, never negative; empty when its
 *                   rounds are not counted.
 * @param bonuses    the temporary bonuses laid on this creature alone, on top of
 *                   the fight's group bonuses.
 * @param form       its current alternate form, or empty for its ordinary form.
 */
public record ConjuredCreature(String creature, int number, int damage, OptionalInt roundsLeft,
	List<AppliedBonus> bonuses, String form)
{
	/**
	 * A creature with no bonus of its own.
	 */
	public ConjuredCreature(String creature, int number, int damage, OptionalInt roundsLeft)
	{
		this(creature, number, damage, roundsLeft, List.of(), "");
	}

	public ConjuredCreature(String creature, int number, int damage, OptionalInt roundsLeft,
		List<AppliedBonus> bonuses)
	{
		this(creature, number, damage, roundsLeft, bonuses, "");
	}

	public ConjuredCreature
	{
		bonuses = (bonuses == null) ? List.of() : List.copyOf(bonuses);
		form = (form == null) ? "" : form.strip();
		if (creature == null || creature.isBlank())
		{
			throw new IllegalArgumentException("A conjured creature needs the name of its stat line");
		}
		creature = creature.strip();
		if (number < 1)
		{
			throw new IllegalArgumentException("A conjured creature is numbered from 1, not " + number);
		}
		if (damage < 0)
		{
			throw new IllegalArgumentException("Damage taken cannot be negative, and " + damage + " is");
		}
		roundsLeft = (roundsLeft == null) ? OptionalInt.empty() : roundsLeft;
		if (roundsLeft.isPresent() && roundsLeft.getAsInt() < 0)
		{
			throw new IllegalArgumentException("Rounds left cannot be negative, and " + roundsLeft.getAsInt() + " is");
		}
	}

	/**
	 * @param name   a creature's name, in any case
	 * @param wanted a number
	 * @return true when this is the creature of that name with that number
	 */
	public boolean is(String name, int wanted)
	{
		return number == wanted && creature.equalsIgnoreCase(name.strip());
	}
}
