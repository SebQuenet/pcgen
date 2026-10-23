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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * A spell a combatant cast, counted down at the start of each of its turns, as a duration
 * runs from its caster's initiative.
 *
 * @param name       what the game master reads. Never blank.
 * @param roundsLeft turns of its caster before it ends, at least one; empty when it lasts until dismissed
 * @param targetIds  the combatants bearing its effect, a condition of its name, until it ends
 */
public record CastSpell(String name, OptionalInt roundsLeft, List<Integer> targetIds)
{
	public CastSpell
	{
		if (Objects.requireNonNull(name).isBlank())
		{
			throw new IllegalArgumentException("A spell has a name");
		}
		name = name.strip();
		Objects.requireNonNull(roundsLeft);
		targetIds = List.copyOf(targetIds);
		if (roundsLeft.isPresent() && roundsLeft.getAsInt() < 1)
		{
			throw new IllegalArgumentException("A spell lasts at least one round: " + roundsLeft);
		}
	}

	/** A spell on no one, only kept track of. */
	public CastSpell(String name, OptionalInt roundsLeft)
	{
		this(name, roundsLeft, List.of());
	}

	/**
	 * @return this spell once its caster begins a turn, or nothing when it has ended.
	 */
	public Optional<CastSpell> ticked()
	{
		if (roundsLeft.isEmpty())
		{
			return Optional.of(this);
		}
		int left = roundsLeft.getAsInt() - 1;
		return (left < 1) ? Optional.empty() : Optional.of(new CastSpell(name, OptionalInt.of(left), targetIds));
	}
}
