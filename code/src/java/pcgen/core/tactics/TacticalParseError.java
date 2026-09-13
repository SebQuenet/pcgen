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

/**
 * One thing wrong with a tactical plan's source text.
 *
 * @param line    the one based line it is on, so an editor can point at it.
 * @param message what is wrong, in words a writer can act on. Never blank.
 */
public record TacticalParseError(int line, String message)
{
	public TacticalParseError
	{
		if (line < 1)
		{
			throw new IllegalArgumentException("A parse error sits on a one based line, not " + line);
		}
		message = TacticalText.required(message, "message");
	}
}
