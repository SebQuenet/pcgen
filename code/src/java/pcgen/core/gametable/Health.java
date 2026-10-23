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

/**
 * The hit points of a monster the game master tracks.
 *
 * @param maximum its full hit points, as PCGen computes them
 * @param damage  the damage taken so far. Never negative.
 */
public record Health(int maximum, int damage)
{
	public Health
	{
		if (damage < 0)
		{
			throw new IllegalArgumentException("Damage is never negative: " + damage);
		}
	}

	/**
	 * @return the hit points left, below zero once the monster is dying.
	 */
	public int remaining()
	{
		return maximum - damage;
	}

	/**
	 * @param amount hit points lost.
	 * @return this health after the blow.
	 */
	public Health damaged(int amount)
	{
		return new Health(maximum, damage + Math.max(0, amount));
	}

	/**
	 * @param amount hit points healed.
	 * @return this health after healing, never above the maximum.
	 */
	public Health healed(int amount)
	{
		return new Health(maximum, Math.max(0, damage - Math.max(0, amount)));
	}

	/**
	 * @param newMaximum the full hit points after a touch to the monster.
	 * @return this health with the same damage taken.
	 */
	public Health withMaximum(int newMaximum)
	{
		return new Health(newMaximum, damage);
	}
}
