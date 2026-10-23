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
 * Where a monster comes from: a race of the loaded bestiary, or a character file the
 * game master built.
 */
public sealed interface MonsterSource
{
	/**
	 * @param raceKey the race's key in the loaded sources. Never blank.
	 */
	record BestiaryRace(String raceKey) implements MonsterSource
	{
		public BestiaryRace
		{
			if (Objects.requireNonNull(raceKey).isBlank())
			{
				throw new IllegalArgumentException("A bestiary monster names its race");
			}
		}
	}

	/**
	 * @param path the absolute path of a saved character. Never blank.
	 */
	record CharacterFile(String path) implements MonsterSource
	{
		public CharacterFile
		{
			if (Objects.requireNonNull(path).isBlank())
			{
				throw new IllegalArgumentException("A monster built as a character names its file");
			}
		}
	}
}
