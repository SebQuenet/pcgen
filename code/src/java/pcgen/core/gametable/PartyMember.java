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
import java.util.Optional;

/**
 * A player's character in the party.
 *
 * @param name          the character's name. Never blank.
 * @param characterFile its saved character, when the game master has a copy to glance at
 */
public record PartyMember(String name, Optional<String> characterFile)
{
	public PartyMember
	{
		if (Objects.requireNonNull(name).isBlank())
		{
			throw new IllegalArgumentException("A party member has a name");
		}
		name = name.strip();
		Objects.requireNonNull(characterFile);
	}
}
