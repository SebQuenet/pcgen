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
import java.util.stream.Stream;

/**
 * A creature's special abilities as PCGen lists them, sorted the way a stat block sorts them.
 *
 * @param attacks   its special attacks
 * @param qualities its special qualities
 * @param defenses  its damage reduction and spell resistance
 */
public record SpecialAbilities(List<String> attacks, List<String> qualities, List<String> defenses)
{
	private static final String SEPARATOR = ", ";

	public SpecialAbilities
	{
		attacks = List.copyOf(attacks);
		qualities = List.copyOf(qualities);
		defenses = List.copyOf(defenses);
	}

	/** @return every ability on one line, as the conjuration sheets print them. */
	public String joined()
	{
		return String.join(SEPARATOR, Stream.of(attacks, qualities, defenses).flatMap(List::stream).toList());
	}

	/**
	 * @param listed abilities as an output token lists them, such as "Sneak Attack, Energy Resistance (Acid, Cold)".
	 * @return each ability, its choices kept with it.
	 */
	public static List<String> split(String listed)
	{
		List<String> abilities = new ArrayList<>();
		int depth = 0;
		int start = 0;
		for (int index = 0; index < listed.length(); index++)
		{
			char character = listed.charAt(index);
			if (character == '(')
			{
				depth++;
			}
			else if (character == ')')
			{
				depth = Math.max(0, depth - 1);
			}
			else if (character == ',' && depth == 0)
			{
				abilities.add(listed.substring(start, index));
				start = index + 1;
			}
		}
		abilities.add(listed.substring(start));
		return abilities.stream().map(String::strip).filter(ability -> !ability.isEmpty()).toList();
	}
}
