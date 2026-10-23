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
 * Identical monsters of a prepared encounter, which share one initiative in the fight.
 *
 * @param name   the name of one, numbered in the fight when there are several. Never blank.
 * @param recipe how to build each one
 * @param count  how many there are, at least one
 */
public record EncounterMonster(String name, MonsterRecipe recipe, int count)
{
	public EncounterMonster
	{
		if (Objects.requireNonNull(name).isBlank())
		{
			throw new IllegalArgumentException("A monster has a name");
		}
		name = name.strip();
		Objects.requireNonNull(recipe);
		if (count < 1)
		{
			throw new IllegalArgumentException("An encounter holds at least one of a monster: " + count);
		}
	}

	public EncounterMonster withRecipe(MonsterRecipe newRecipe)
	{
		return new EncounterMonster(name, newRecipe, count);
	}

	public EncounterMonster withCount(int newCount)
	{
		return new EncounterMonster(name, recipe, newCount);
	}
}
