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
 * One creature in play, as a caller reads it.
 *
 * @param creature   the name of its stat line in the catalog
 * @param number     which of the creatures of that name it is, from 1
 * @param damage     the damage it has taken
 * @param roundsLeft how many rounds it stays; null when its rounds are not counted
 * @param bonuses    the bonuses laid on it alone, each its source key with the value
 *                   chosen in parentheses, such as "SPELL=Prayer" or "ABILITY=Internal=Temp Bonus ~ Inspire Courage (2)"
 */
public record ConjuredCreatureEntry(String creature, int number, int damage, Integer roundsLeft, List<String> bonuses)
{
}
