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
import java.util.Map;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A creature the character can conjure, as its catalog writes it. The values
 * are the author's: nothing here is computed from PCGen's own data.
 *
 * @param name   the creature's name, which a creature in play uses to find its
 *               stat line again. Never blank.
 * @param roles  what the creature is for, such as "Tank"; the catalog filters
 *               on them. Never modifiable through the list passed in.
 * @param fields the rest of its stat line, as written. A field the author left
 *               out is simply missing.
 */
public record SummonTemplate(String name, List<String> roles, Map<SummonField, String> fields)
{
	private static final Pattern WHOLE_NUMBER = Pattern.compile("\\d{1,6}");

	public SummonTemplate
	{
		if (name == null || name.isBlank())
		{
			throw new IllegalArgumentException("A conjuration needs a name");
		}
		name = name.strip();
		roles = (roles == null) ? List.of() : List.copyOf(roles);
		fields = (fields == null) ? Map.of() : Map.copyOf(fields);
	}

	/**
	 * @param field one column of the stat line
	 * @return what the author wrote there, empty when nothing was written
	 */
	public String field(SummonField field)
	{
		return fields.getOrDefault(field, "");
	}

	/**
	 * The hit points a creature in play starts from. They are free text, so the
	 * first whole number in them is taken as the maximum.
	 *
	 * @return the maximum, empty when the hit points hold no whole number
	 */
	public OptionalInt maximumHitPoints()
	{
		Matcher number = WHOLE_NUMBER.matcher(field(SummonField.HIT_POINTS));
		return number.find() ? OptionalInt.of(Integer.parseInt(number.group())) : OptionalInt.empty();
	}
}
