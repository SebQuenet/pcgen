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

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * A temporary bonus laid on conjured creatures.
 *
 * @param source      the rules object the bonus comes from. Never null.
 * @param chosenValue the value chosen for a bonus that asks for one, such as
 *                    the +2 of an Inspire Courage; empty for any other bonus.
 */
public record AppliedBonus(BonusSourceKey source, OptionalInt chosenValue)
{
	public AppliedBonus
	{
		if (source == null)
		{
			throw new IllegalArgumentException("An applied bonus needs its source");
		}
		chosenValue = (chosenValue == null) ? OptionalInt.empty() : chosenValue;
	}

	/**
	 * @param bonuses the bonuses already laid.
	 * @param added   the bonus to lay; it replaces a bonus from the same source.
	 * @return the bonuses with {@code added}, in the order they were first laid.
	 */
	static List<AppliedBonus> adding(List<AppliedBonus> bonuses, AppliedBonus added)
	{
		List<AppliedBonus> joined = new ArrayList<>(bonuses);
		int index = joined.stream().map(AppliedBonus::source).toList().indexOf(added.source());
		if (index < 0)
		{
			joined.add(added);
		}
		else
		{
			joined.set(index, added);
		}
		return joined;
	}

	/**
	 * @param bonuses the bonuses already laid.
	 * @param removed the source whose bonus is lifted.
	 * @return the bonuses without the one from {@code removed}.
	 */
	static List<AppliedBonus> removing(List<AppliedBonus> bonuses, BonusSourceKey removed)
	{
		return bonuses.stream().filter(bonus -> !bonus.source().equals(removed)).toList();
	}
}
