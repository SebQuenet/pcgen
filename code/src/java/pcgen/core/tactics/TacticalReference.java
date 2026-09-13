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
 * A pointer at something the character owns, resolved when the sheet is
 * rendered so that it follows a level up on its own.
 *
 * @param kind what kind of thing to look for. Never null.
 * @param key  the thing's name, as the character knows it. Never blank.
 */
public record TacticalReference(ReferenceKind kind, String key) implements TacticalSubject
{
	public TacticalReference
	{
		if (kind == null)
		{
			throw new IllegalArgumentException("A tactical reference needs a kind");
		}
		key = TacticalText.required(key, "key");
	}

	@Override
	public String display()
	{
		return key;
	}
}
