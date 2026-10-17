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
package pcgen.core.bonus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Pattern;

import pcgen.cdom.base.CDOMObject;
import pcgen.cdom.enumeration.ListKey;
import pcgen.cdom.enumeration.StringKey;
import pcgen.core.Globals;
import pcgen.core.PlayerCharacter;

/**
 * Lays the temporary bonuses of a rules object on a character, as PCGen's
 * temporary-bonus tab does, but without asking anything: a value the bonus
 * needs is passed in.
 */
public final class TemporaryBonusApplier
{
	private static final String CHOICE = "%CHOICE";
	private static final String MINIMUM = "MIN=";
	private static final String MAXIMUM = "MAX=";
	private static final String TITLE = "TITLE=";

	private TemporaryBonusApplier()
	{
	}

	/**
	 * @param target      the character receiving the bonuses.
	 * @param source      the rules object granting them.
	 * @param chosenValue the value replacing {@code %CHOICE} for a bonus that asks one.
	 */
	public static void apply(PlayerCharacter target, CDOMObject source, OptionalInt chosenValue)
	{
		List<BonusObj> granted = new ArrayList<>(source.getSafeListFor(ListKey.BONUS_ANYPC));
		granted.addAll(source.getSafeListFor(ListKey.BONUS_PC));
		for (BonusObj bonus : granted)
		{
			String text = bonus.toString();
			if (chosenValue.isPresent())
			{
				text = text.replaceAll(Pattern.quote(CHOICE), String.valueOf(chosenValue.getAsInt()));
			}
			BonusObj laid = Bonus.newBonus(Globals.getContext(), text);
			if (laid != null)
			{
				target.setApplied(laid, laid.qualifies(target, null));
				target.addTempBonus(laid, source, target);
			}
		}
		target.calcActiveBonuses();
	}

	/**
	 * @param target the character carrying the bonuses.
	 * @param source the rules object whose bonuses are lifted.
	 */
	public static void remove(PlayerCharacter target, CDOMObject source)
	{
		List<BonusObj> lifted = target.getTempBonusMap().entrySet().stream()
			.filter(laid -> laid.getValue().source == source).map(Map.Entry::getKey).toList();
		lifted.forEach(target::removeTempBonus);
		target.calcActiveBonuses();
	}

	/**
	 * @param character the character whose variables the range's formulas read.
	 * @param source    a rules object granting temporary bonuses.
	 * @return the values the player may choose, or nothing when its bonus asks none.
	 */
	public static Optional<IntRange> choiceRange(PlayerCharacter character, CDOMObject source)
	{
		String choice = source.get(StringKey.TEMPVALUE);
		if (choice == null || choice.isBlank())
		{
			return Optional.empty();
		}
		String minimum = "0";
		String maximum = "0";
		String title = "";
		for (String part : choice.split("\\|"))
		{
			if (part.startsWith(MINIMUM))
			{
				minimum = part.substring(MINIMUM.length());
			}
			else if (part.startsWith(MAXIMUM))
			{
				maximum = part.substring(MAXIMUM.length());
			}
			else if (part.startsWith(TITLE))
			{
				title = part.substring(TITLE.length());
			}
		}
		int lowest = character.getVariableValue(minimum, "").intValue();
		int highest = character.getVariableValue(maximum, "").intValue();
		return (highest < lowest) ? Optional.empty() : Optional.of(new IntRange(lowest, highest, title));
	}
}
