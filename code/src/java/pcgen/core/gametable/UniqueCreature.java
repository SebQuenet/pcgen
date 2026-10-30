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

/**
 * A creature the game master made unique from a bestiary entry, kept in the scenario: its own name, and its recipe
 * with the templates and hit dice laid on it.
 *
 * @param name   its name. Never blank.
 * @param recipe how it is built.
 */
public record UniqueCreature(String name, MonsterRecipe recipe)
{
	public UniqueCreature
	{
		if (Objects.requireNonNull(name).isBlank())
		{
			throw new IllegalArgumentException("A unique creature has a name");
		}
		Objects.requireNonNull(recipe);
	}
}
