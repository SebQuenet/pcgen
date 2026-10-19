/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package pcgen.core.conjurations;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Permanent feat choices saved alongside the character's personal catalog. */
public final class SummonSelections
{
	private static final String VERSATILE_MONSTER = "# versatile-sm: ";
	private static final List<String> MONSTER_TEMPLATES = List.of("Aerial Creature", "Aqueous Creature",
		"Chthonic Creature", "Dark Creature", "Fiery Creature", "Primordial Creature");

	private SummonSelections()
	{
	}

	public static Set<String> versatileMonster(String source)
	{
		Set<String> selected = new LinkedHashSet<>();
		if (source != null)
		{
			for (String line : source.split("\\R"))
			{
				if (line.startsWith(VERSATILE_MONSTER))
				{
					String template = line.substring(VERSATILE_MONSTER.length()).strip();
					if (MONSTER_TEMPLATES.contains(template))
					{
						selected.add(template);
					}
				}
			}
		}
		return Set.copyOf(selected);
	}

	public static boolean isMonsterTemplate(String template)
	{
		return MONSTER_TEMPLATES.contains(template);
	}

	public static String versatileMonsterLine(String template)
	{
		if (!isMonsterTemplate(template))
		{
			throw new IllegalArgumentException("Unknown versatile summon template: " + template);
		}
		return VERSATILE_MONSTER + template;
	}
}
