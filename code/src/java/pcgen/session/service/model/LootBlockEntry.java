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
 * The loot of one finished fight.
 *
 * @param id              its number, to select, draw again or remove it
 * @param title           the encounters fought
 * @param challengeRating the challenge rating of the whole fight
 * @param experience      what the fight's monsters were worth together
 * @param experiencePerPlayer each party member's share of that experience
 * @param budget          the treasure value of that challenge rating, in gold pieces
 * @param lines           what the monsters carried, then what was drawn, each line by its index
 * @param selectedValue   what the kept lines are worth together
 * @param selectedResale  what the kept lines sell for together
 * @param sharePerPlayer  each party member's share of that resale
 */
public record LootBlockEntry(int id, String title, String challengeRating, long experience, long experiencePerPlayer,
	long budget, List<LootLineEntry> lines,
	long selectedValue, long selectedResale, long sharePerPlayer)
{
}
