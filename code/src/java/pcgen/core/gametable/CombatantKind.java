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

/**
 * Who a combatant is to the game master: a player's, whose sheet the player keeps, or
 * the game master's own, whose hit points and numbers the table tracks.
 */
public enum CombatantKind
{
	PLAYER,
	PLAYER_SUMMON,
	MONSTER,
	MONSTER_SUMMON;

	/**
	 * @return whether the game master tracks its hit points and numbers.
	 */
	public boolean isGameMasters()
	{
		return this == MONSTER || this == MONSTER_SUMMON;
	}
}
