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
package pcgen.core.tactics;

/**
 * How much a buff changes one thing.
 *
 * @param target what it changes. Never null.
 * @param amount by how much, which may be negative.
 */
public record TacticalDelta(DeltaTarget target, int amount)
{
	public TacticalDelta
	{
		if (target == null)
		{
			throw new IllegalArgumentException("A delta needs something to change");
		}
	}

	/**
	 * @return the amount as it should read on the sheet, signed
	 */
	public String signed()
	{
		return (amount < 0) ? Integer.toString(amount) : "+" + amount;
	}
}
