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
package pcgen.session.service.model;

/**
 * What a finished fight is worth; the fight is cleared.
 *
 * @param experience      what its monsters were worth together
 * @param challengeRating the challenge rating that total amounts to, null for none
 * @param players         how many players share it
 * @param perPlayer       each player's share
 * @param loot            the fight's loot, null when no monster of the game master's took part
 * @param table           the game table once the fight is cleared
 */
public record CombatEnded(long experience, String challengeRating, int players, long perPlayer, LootBlockEntry loot,
	GameTableState table)
{
}
