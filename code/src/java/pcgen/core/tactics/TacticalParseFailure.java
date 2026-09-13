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
package pcgen.core.tactics;

import java.util.List;

/**
 * A source text that did not read.
 *
 * @param errors every error found, in line order. Never empty, and never
 *               modifiable through the list passed in.
 */
public record TacticalParseFailure(List<TacticalParseError> errors) implements TacticalParseResult
{
	public TacticalParseFailure
	{
		if (errors == null || errors.isEmpty())
		{
			throw new IllegalArgumentException("A failed parse needs at least one error");
		}
		errors = List.copyOf(errors);
	}
}
