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

import java.util.Optional;

import pcgen.core.bonus.IntRange;

/**
 * A temporary bonus the player may lay on conjured creatures.
 *
 * @param key   the key naming its source, such as "SPELL=Prayer".
 * @param label the name of its source in the player's language.
 * @param range the values it lets the player choose, or nothing when it asks none.
 */
public record BonusChoice(String key, String label, Optional<IntRange> range)
{
}
