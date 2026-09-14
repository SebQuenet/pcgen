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

import java.util.List;
import java.util.Optional;

/**
 * Something the character can have up, and what it changes while it is.
 *
 * <p>
 * A buff changes the numbers one of two ways. Declaring deltas has the page do
 * the arithmetic, which works anywhere including in an exported file, but the
 * sums are the writer's. Naming a temporary bonus has PCGen recompute
 * everything, which is exact and knows what does not stack, but only works
 * inside PCGen.
 *
 * @param label    what to call it. Never blank.
 * @param duration how long it lasts, empty when there is nothing to say.
 * @param gives    the deltas it declares, empty when it names a temporary bonus
 *                 instead. Never modifiable through the list passed in.
 * @param applies  the temporary bonus it leans on, empty when it declares
 *                 deltas instead.
 * @param note     extra detail, empty when there is none.
 */
public record TacticalBuff(String label, String duration, List<TacticalDelta> gives,
		Optional<TacticalReference> applies, String note) implements TacticalBlock
{
	public TacticalBuff
	{
		label = TacticalText.required(label, "label");
		duration = TacticalText.optional(duration);
		gives = (gives == null) ? List.of() : List.copyOf(gives);
		applies = (applies == null) ? Optional.empty() : applies;
		note = TacticalText.optional(note);
		if (gives.isEmpty() && applies.isEmpty())
		{
			throw new IllegalArgumentException(
				"Buff '" + label + "' changes nothing: it needs a 'gives:' line or an 'applies:' line");
		}
	}
}
