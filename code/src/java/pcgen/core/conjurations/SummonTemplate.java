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
 * A creature the character can conjure: a race of PCGen's data and the
 * templates laid on it. PCGen computes every number from them.
 *
 * @param raceKey      the key of the creature's race in the loaded sources. Never blank.
 * @param templateKeys the keys of the templates laid on that race, in the order written.
 * @param line         the line of the catalog that opens this summon, from 1, so that a
 *                     race the loaded sources lack can be reported where it is written.
 */
public record SummonTemplate(String raceKey, List<String> templateKeys, int line, List<String> roles,
	Map<String, String> metadata)
{
	private static final Pattern WHOLE_NUMBER = Pattern.compile("\\d{1,6}");

	public SummonTemplate(String raceKey, List<String> templateKeys, int line)
	{
		this(raceKey, templateKeys, line, List.of(), Map.of());
	}

	public SummonTemplate
	{
		if (raceKey == null || raceKey.isBlank())
		{
			throw new IllegalArgumentException("A conjuration needs a race");
		}
		raceKey = raceKey.strip();
		templateKeys = (templateKeys == null) ? List.of() : List.copyOf(templateKeys);
		roles = (roles == null) ? List.of() : List.copyOf(roles);
		metadata = (metadata == null) ? Map.of() : Map.copyOf(metadata);
	}

	/** Whether this entry carries a handwritten stat line from an earlier catalog. */
	public boolean hasAuthoredStats()
	{
		return metadata.containsKey("hp");
	}

	public OptionalInt authoredHitPoints()
	{
		Matcher number = WHOLE_NUMBER.matcher(metadata.getOrDefault("hp", ""));
		return number.find() ? OptionalInt.of(Integer.parseInt(number.group())) : OptionalInt.empty();
	}

	/**
	 * The name a creature in play uses to find this summon again: its race,
	 * followed by its templates in parentheses when it has any.
	 *
	 * @return the name, such as "Eagle (Celestial Creature)".
	 */
	public String name()
	{
		return templateKeys.isEmpty() ? raceKey : raceKey + " (" + String.join(", ", templateKeys) + ')';
	}
}
