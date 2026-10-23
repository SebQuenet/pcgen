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

import java.util.List;

/**
 * A line of the order of initiative: a combatant's turn, or a combatant delaying.
 *
 * @param id                 the combatant's id, which every operation on it takes
 * @param name               its name
 * @param kind               PLAYER, PLAYER_SUMMON, MONSTER or MONSTER_SUMMON
 * @param initiative         the score of this turn, null until rolled or announced
 * @param initiativeModifier its initiative modifier
 * @param secondTurn         whether this is the second turn of a dual initiative, twenty lower
 * @param acting             whether this turn is the one being played
 * @param delaying           whether the combatant has stepped out of the order
 * @param hitPoints          its hit points left, null for those whose player tracks them
 * @param maximumHitPoints   its full hit points, null for those whose player tracks them
 * @param conditions         what is laid on it
 * @param roundsLeft         the rounds before a conjured creature leaves, null when it stays
 * @param summonerId         the id of its conjurer, null when not conjured
 * @param recipe             how it is built, null for those whose player tracks them
 */
public record CombatantEntry(int id, String name, String kind, Integer initiative, int initiativeModifier,
	boolean secondTurn, boolean acting, boolean delaying, Integer hitPoints, Integer maximumHitPoints,
	List<ConditionEntry> conditions, Integer roundsLeft, Integer summonerId, RecipeEntry recipe)
{
}
