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
package pcgen.core.gametable;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * One of a creature's six ability scores.
 *
 * @param abbreviation the ability's short name, such as Str.
 * @param score        the creature's score; empty when it has none, as an undead has no Constitution.
 */
public record AbilityScore(String abbreviation, OptionalInt score)
{
	public AbilityScore
	{
		Objects.requireNonNull(abbreviation);
		Objects.requireNonNull(score);
	}
}
