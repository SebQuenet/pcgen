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

import java.util.List;
import java.util.Objects;

/**
 * A criterion read back from text, and the fields it named that this version of PCGen no longer has.
 *
 * @param criterion     the criterion, without the tests on unknown fields.
 * @param ignoredFields the unknown fields' names, in the order met.
 */
public record CriterionReading(Criterion criterion, List<String> ignoredFields)
{
	public CriterionReading
	{
		Objects.requireNonNull(criterion);
		ignoredFields = List.copyOf(ignoredFields);
	}
}
