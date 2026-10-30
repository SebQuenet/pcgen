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
package pcgen.core.gametable.catalog;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * One value of a bestiary entry, of the sort its field holds.
 */
public sealed interface FieldValue
{
	/**
	 * @param value a number; a challenge rating may be a fraction, such as 1/3.
	 */
	record Number(double value) implements FieldValue
	{
	}

	/**
	 * @param value a text. Never null.
	 */
	record Text(String value) implements FieldValue
	{
		public Text
		{
			Objects.requireNonNull(value);
		}
	}

	/**
	 * @param values names, such as subtypes.
	 */
	record Items(Set<String> values) implements FieldValue
	{
		public Items
		{
			values = Set.copyOf(values);
		}
	}

	/**
	 * @param values numbers by name, such as skill totals.
	 */
	record Scores(Map<String, Integer> values) implements FieldValue
	{
		public Scores
		{
			values = Map.copyOf(values);
		}
	}

	/**
	 * @param value yes or no.
	 */
	record Flag(boolean value) implements FieldValue
	{
	}
}
