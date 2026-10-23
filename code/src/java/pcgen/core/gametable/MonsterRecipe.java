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
package pcgen.core.gametable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * How to rebuild a monster: its source, then the touches the game master made to it.
 * A scenario keeps the recipe, never a copy of the character, so the source file is
 * never changed by the game table.
 *
 * @param source           the race or character file it starts from
 * @param addedTemplates   template keys laid on top of the source, in order
 * @param removedTemplates template keys the source has and the monster loses
 * @param addedHitDice     racial hit dice added to the source. Never negative.
 */
public record MonsterRecipe(MonsterSource source, List<String> addedTemplates, List<String> removedTemplates,
	int addedHitDice)
{
	public MonsterRecipe
	{
		Objects.requireNonNull(source);
		addedTemplates = List.copyOf(addedTemplates);
		removedTemplates = List.copyOf(removedTemplates);
		if (addedHitDice < 0)
		{
			throw new IllegalArgumentException("Hit dice are added, never taken away: " + addedHitDice);
		}
	}

	/**
	 * @param source where the monster comes from.
	 * @return the monster as its source has it.
	 */
	public static MonsterRecipe of(MonsterSource source)
	{
		return new MonsterRecipe(source, List.of(), List.of(), 0);
	}

	/**
	 * @param template a template key.
	 * @return this monster with the template laid on it; a template it had lost comes back.
	 */
	public MonsterRecipe withTemplate(String template)
	{
		List<String> removed = new ArrayList<>(removedTemplates);
		if (removed.remove(template))
		{
			return new MonsterRecipe(source, addedTemplates, removed, addedHitDice);
		}
		if (addedTemplates.contains(template))
		{
			return this;
		}
		List<String> added = new ArrayList<>(addedTemplates);
		added.add(template);
		return new MonsterRecipe(source, added, removedTemplates, addedHitDice);
	}

	/**
	 * @param template a template key.
	 * @return this monster without the template, whether it was added or came with the source.
	 */
	public MonsterRecipe withoutTemplate(String template)
	{
		List<String> added = new ArrayList<>(addedTemplates);
		if (added.remove(template))
		{
			return new MonsterRecipe(source, added, removedTemplates, addedHitDice);
		}
		if (removedTemplates.contains(template))
		{
			return this;
		}
		List<String> removed = new ArrayList<>(removedTemplates);
		removed.add(template);
		return new MonsterRecipe(source, addedTemplates, removed, addedHitDice);
	}

	/**
	 * @param hitDice the racial hit dice to add to the source.
	 * @return this monster with that many added hit dice.
	 */
	public MonsterRecipe withHitDice(int hitDice)
	{
		return new MonsterRecipe(source, addedTemplates, removedTemplates, hitDice);
	}
}
