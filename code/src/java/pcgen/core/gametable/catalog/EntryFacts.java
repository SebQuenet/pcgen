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

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The values known of a bestiary entry. A field absent is not known yet: the entry is not built, or its build failed.
 *
 * @param values       the values known, by field.
 * @param buildFailure why the entry could not be built; empty when it was, or is not built yet.
 */
public record EntryFacts(Map<CatalogField, FieldValue> values, Optional<String> buildFailure)
{
	public EntryFacts
	{
		values = values.isEmpty() ? Map.of() : Map.copyOf(new EnumMap<>(values));
		Objects.requireNonNull(buildFailure);
	}

	/**
	 * @return an entry of which nothing is known.
	 */
	public static EntryFacts none()
	{
		return new EntryFacts(Map.of(), Optional.empty());
	}

	/**
	 * @param field a field.
	 * @return its value; empty when not known.
	 */
	public Optional<FieldValue> value(CatalogField field)
	{
		return Optional.ofNullable(values.get(field));
	}

	/**
	 * @param more values to add, replacing those of the same fields.
	 * @return these facts with the values added.
	 */
	public EntryFacts with(Map<CatalogField, FieldValue> more)
	{
		Map<CatalogField, FieldValue> merged = new EnumMap<>(CatalogField.class);
		merged.putAll(values);
		merged.putAll(more);
		return new EntryFacts(merged, buildFailure);
	}
}
