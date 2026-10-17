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

import java.util.Optional;
import java.util.Set;

/**
 * Names the rules object a temporary bonus comes from, in a form a saved
 * character can hold: its kind, its category for an ability, and its key.
 *
 * @param kind     one of {@link #SPELL}, {@link #ABILITY}, {@link #TEMPLATE}, {@link #EQUIPMENT}
 * @param category the ability category's key for an ability; empty for any other kind
 * @param key      the object's key in the loaded sources. Never blank.
 */
public record BonusSourceKey(String kind, String category, String key)
{
	public static final String SPELL = "SPELL";
	public static final String ABILITY = "ABILITY";
	public static final String TEMPLATE = "TEMPLATE";
	public static final String EQUIPMENT = "EQUIPMENT";

	private static final Set<String> KINDS = Set.of(SPELL, ABILITY, TEMPLATE, EQUIPMENT);
	private static final String SEPARATOR = "=";

	public BonusSourceKey
	{
		if (!KINDS.contains(kind))
		{
			throw new IllegalArgumentException("A bonus comes from one of " + KINDS + ", not " + kind);
		}
		category = (category == null) ? "" : category.strip();
		if (ABILITY.equals(kind) == category.isEmpty())
		{
			throw new IllegalArgumentException("An ability, and only an ability, names its category");
		}
		if (key == null || key.isBlank())
		{
			throw new IllegalArgumentException("A bonus source needs a key");
		}
		key = key.strip();
	}

	/**
	 * The key as a saved character writes it, such as "SPELL=Prayer" or
	 * "ABILITY=FEAT=Power Attack".
	 *
	 * @return the key's text, which {@link #parse(String)} reads back
	 */
	public String text()
	{
		return category.isEmpty() ? kind + SEPARATOR + key : kind + SEPARATOR + category + SEPARATOR + key;
	}

	/**
	 * Reads a key written by {@link #text()}.
	 *
	 * @param text the key's text
	 * @return the key, or empty when the text names no known kind or lacks a part
	 */
	public static Optional<BonusSourceKey> parse(String text)
	{
		if (text == null)
		{
			return Optional.empty();
		}
		String[] kindAndRest = text.split(SEPARATOR, 2);
		if (kindAndRest.length < 2 || !KINDS.contains(kindAndRest[0]))
		{
			return Optional.empty();
		}
		String kind = kindAndRest[0];
		String category = "";
		String key = kindAndRest[1];
		if (ABILITY.equals(kind))
		{
			String[] categoryAndKey = key.split(SEPARATOR, 2);
			if (categoryAndKey.length < 2)
			{
				return Optional.empty();
			}
			category = categoryAndKey[0];
			key = categoryAndKey[1];
		}
		if (key.isBlank() || (ABILITY.equals(kind) && category.isBlank()))
		{
			return Optional.empty();
		}
		return Optional.of(new BonusSourceKey(kind, category, key));
	}
}
