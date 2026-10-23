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

import pcgen.core.conjurations.CreatureStats;
import pcgen.core.gametable.PlayerGlance;

/**
 * What the game master reads of a combatant.
 *
 * @param id              its id
 * @param name            its name
 * @param kind            PLAYER, PLAYER_SUMMON, MONSTER or MONSTER_SUMMON
 * @param stats           a monster's numbers under its conditions, null for others
 * @param challengeRating a monster's challenge rating, null for others
 * @param templates       the keys of the visible templates a monster has, empty for others
 * @param glance          a player's perception, sense motive and saves, read from its file, null when unknown
 * @param conditions      what is laid on it
 * @param spells          the spells it cast and keeps up
 */
public record CombatantSheet(int id, String name, String kind, CreatureStats stats, String challengeRating,
	List<String> templates, PlayerGlance glance, List<ConditionEntry> conditions,
	List<CastSpellEntry> spells)
{
}
