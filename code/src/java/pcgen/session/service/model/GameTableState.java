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
 * The scenario the game table has open.
 *
 * @param file       the file it was opened from or saved to, null before it is saved
 * @param dirty      whether it changed since then
 * @param gameMode   the game mode of its sources
 * @param campaigns  the keys of its sources
 * @param party      the players' characters
 * @param encounters the prepared encounters
 * @param combat     the fight under way
 * @param notices    what the last call could not do, or what a new round wore off
 */
public record GameTableState(String file, boolean dirty, String gameMode, List<String> campaigns,
	List<PartyMemberEntry> party, List<EncounterEntry> encounters, CombatEntry combat, List<String> notices)
{
}
