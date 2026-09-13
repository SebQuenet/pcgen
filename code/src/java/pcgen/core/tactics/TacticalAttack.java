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
 * One line of the attack table, and how it changes per kind of target.
 *
 * <p>
 * The three number fields are empty when the sheet should read them off the
 * character, and present when the writer supplied them instead.
 *
 * @param subject  the weapon, either referenced or written. Never null.
 * @param toHit    the attack bonus, empty to read it off the character.
 * @param damage   the damage, empty to read it off the character.
 * @param critical the critical, empty to read it off the character.
 * @param variants what changes per kind of target, in reading order. Never
 *                 modifiable through the list passed in.
 * @param note     extra detail, empty when there is none.
 */
public record TacticalAttack(TacticalSubject subject, Optional<String> toHit, Optional<String> damage,
		Optional<String> critical, List<TacticalVariant> variants, String note) implements TacticalBlock
{
	public TacticalAttack
	{
		if (subject == null)
		{
			throw new IllegalArgumentException("An attack needs a subject");
		}
		toHit = emptyWhenBlank(toHit);
		damage = emptyWhenBlank(damage);
		critical = emptyWhenBlank(critical);
		variants = (variants == null) ? List.of() : List.copyOf(variants);
		note = TacticalText.optional(note);
	}

	private static Optional<String> emptyWhenBlank(Optional<String> candidate)
	{
		return (candidate == null) ? Optional.empty() : candidate.map(String::strip).filter(text -> !text.isEmpty());
	}
}
