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

import java.util.Locale;

/**
 * The treasure a bestiary entry gives a creature, as a share of the treasure value of its
 * challenge rating. An NPC's gear is its treasure: nothing is drawn for it.
 */
public enum TreasureType
{
	NONE(0, 1),
	INCIDENTAL(1, 2),
	STANDARD(1, 1),
	DOUBLE(2, 1),
	TRIPLE(3, 1),
	NPC_GEAR(0, 1);

	private final long numerator;
	private final long denominator;

	TreasureType(long numerator, long denominator)
	{
		this.numerator = numerator;
		this.denominator = denominator;
	}

	/**
	 * @param bestiaryText the treasure line of a stat block, such as "double standard" or
	 *                     "NPC gear (leather armor, other treasure)".
	 * @return the type it names; standard when it names none.
	 */
	public static TreasureType parse(String bestiaryText)
	{
		String text = bestiaryText.toLowerCase(Locale.ROOT);
		int details = text.indexOf('(');
		String kind = (details < 0 ? text : text.substring(0, details)).strip();
		if (kind.startsWith("npc gear"))
		{
			return NPC_GEAR;
		}
		if (kind.startsWith("none"))
		{
			return NONE;
		}
		if (kind.startsWith("incidental") || kind.startsWith("half") || kind.startsWith("50%"))
		{
			return INCIDENTAL;
		}
		if (kind.startsWith("double"))
		{
			return DOUBLE;
		}
		if (kind.startsWith("triple"))
		{
			return TRIPLE;
		}
		return STANDARD;
	}

	/**
	 * @param creatureBudget the treasure value of the creature's challenge rating.
	 * @return the treasure this type gives it.
	 */
	public long apply(long creatureBudget)
	{
		return creatureBudget * numerator / denominator;
	}
}
