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
package pcgen.core.tactics;

/**
 * Something that runs out: mythic power, channels, uses per day, an item's
 * daily charge. Rendered as a row of pips the player ticks off.
 *
 * @param label   what to call it on the sheet. Never blank.
 * @param maximum how many uses there are, either a written number or a
 *                reference to the ability that grants them. Never null.
 * @param action  the action it costs, empty when it costs none.
 */
public record TacticalResource(String label, TacticalSubject maximum, String action) implements TacticalBlock
{
	public TacticalResource
	{
		label = TacticalText.required(label, "label");
		if (maximum == null)
		{
			throw new IllegalArgumentException("Resource '" + label + "' needs a maximum");
		}
		action = TacticalText.optional(action);
	}
}
