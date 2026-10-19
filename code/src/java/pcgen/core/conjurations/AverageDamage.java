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
import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A simple damage estimate from PCGen's already calculated weapon damage strings. */
public final class AverageDamage
{
	private static final Pattern DICE = Pattern.compile("^(\\d+)d(\\d+)([+-]\\d+)?$");
	private static final Pattern WHOLE = Pattern.compile("^[+-]?\\d+$");

	private AverageDamage()
	{
	}

	/**
	 * Expected damage against a target AC. Attack bonus segments such as
	 * {@code +9/+4} are separate attacks. Natural 1 and 20 bound each chance
	 * between 5 and 95 percent. Critical hits and damage reduction are excluded.
	 */
	public static OptionalDouble expectedAgainst(List<CreatureAttack> attacks, int targetArmorClass)
	{
		if (attacks.isEmpty())
		{
			return OptionalDouble.empty();
		}
		double total = 0;
		for (CreatureAttack attack : attacks)
		{
			OptionalDouble mean = of(attack.damage());
			if (mean.isEmpty())
			{
				return OptionalDouble.empty();
			}
			if (attack.toHit() == null || attack.toHit().isBlank())
			{
				return OptionalDouble.empty();
			}
			for (String bonusText : attack.toHit().split("/"))
			{
				String bonus = bonusText.strip();
				if (!WHOLE.matcher(bonus).matches())
				{
					return OptionalDouble.empty();
				}
				int targetRoll = targetArmorClass - Integer.parseInt(bonus);
				double hitChance = Math.max(0.05, Math.min(0.95, (21 - targetRoll) / 20.0));
				total += mean.getAsDouble() * hitChance;
			}
		}
		return OptionalDouble.of(total);
	}

	public static OptionalDouble of(String damage)
	{
		if (damage == null)
		{
			return OptionalDouble.empty();
		}
		String compact = damage.strip().replace(" ", "");
		Matcher dice = DICE.matcher(compact);
		if (dice.matches())
		{
			int count = Integer.parseInt(dice.group(1));
			int faces = Integer.parseInt(dice.group(2));
			int bonus = (dice.group(3) == null) ? 0 : Integer.parseInt(dice.group(3));
			return (count > 0 && faces > 0) ? OptionalDouble.of(count * (faces + 1) / 2.0 + bonus)
				: OptionalDouble.empty();
		}
		return WHOLE.matcher(compact).matches() ? OptionalDouble.of(Integer.parseInt(compact))
			: OptionalDouble.empty();
	}
}
