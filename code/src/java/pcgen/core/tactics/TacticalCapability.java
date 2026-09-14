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

/**
 * One thing a character can do, filed under the roles it plays.
 *
 * <p>
 * Written out rather than read off the character: a domain power, a mythic
 * ability, an item's power and a class feature are not one kind of thing in
 * PCGen, and what matters on a tactical sheet is what each is *for*.
 *
 * @param name   what to call it. Never blank.
 * @param tags   the roles it plays, such as buff or damage, in reading order.
 *               Never empty — an untagged capability could not be filtered, and
 *               filtering is the point of the list.
 * @param action the action it costs, empty when there is nothing to say.
 * @param uses   how often it is available, empty when there is nothing to say.
 * @param effect what it does, empty when there is nothing to say.
 */
public record TacticalCapability(String name, List<String> tags, String action, String uses, String effect)
{
	public TacticalCapability
	{
		name = TacticalText.required(name, "name");
		if (tags == null)
		{
			throw new IllegalArgumentException("Capability '" + name + "' needs at least one tag");
		}
		tags = tags.stream().map(String::strip).filter(tag -> !tag.isEmpty()).toList();
		if (tags.isEmpty())
		{
			throw new IllegalArgumentException("Capability '" + name + "' needs at least one non blank tag");
		}
		action = TacticalText.optional(action);
		uses = TacticalText.optional(uses);
		effect = TacticalText.optional(effect);
	}
}
