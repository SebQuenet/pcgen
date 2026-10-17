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
package pcgen.facade.core;

import java.util.List;

import pcgen.core.conjurations.CreatureStats;

/**
 * A creature in play, as the conjuration page shows it.
 *
 * @param name      the name of its catalog entry.
 * @param number    which of the creatures of that name it is, from 1.
 * @param stats     its numbers with every bonus it carries.
 * @param baseStats its numbers before any bonus, to show what a bonus changed.
 * @param bonuses   the bonuses laid on it alone.
 */
public record ConjuredCreatureView(String name, int number, CreatureStats stats, CreatureStats baseStats,
	List<BonusLabel> bonuses)
{
	public ConjuredCreatureView
	{
		bonuses = (bonuses == null) ? List.of() : List.copyOf(bonuses);
	}
}
