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
package pcgen.session.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pcgen.core.Deity;
import pcgen.core.PCAlignment;
import pcgen.core.PCClass;
import pcgen.core.PCStat;
import pcgen.core.Race;
import pcgen.facade.core.CharacterFacade;
import pcgen.facade.core.DataSetFacade;
import pcgen.facade.core.TodoFacade;
import pcgen.session.service.model.CharacterSummary;
import pcgen.session.service.model.ClassLevel;

/**
 * Reads a character down to the handful of figures a sheet's header shows.
 */
public final class CharacterSummaries
{
	private CharacterSummaries()
	{
	}

	public static CharacterSummary of(CharacterFacade character)
	{
		DataSetFacade dataSet = character.getDataSet();
		Race race = character.getRaceRef().get();
		PCAlignment alignment = character.getAlignmentRef().get();
		Deity deity = character.getDeityRef().get();
		Integer hp = character.getTotalHPRef().get();

		Map<String, Integer> scores = new LinkedHashMap<>();
		Map<String, Integer> modifiers = new LinkedHashMap<>();
		for (PCStat stat : dataSet.getStats())
		{
			scores.put(stat.getKeyName(), character.getScoreBase(stat));
			modifiers.put(stat.getKeyName(), character.getModTotal(stat));
		}

		List<ClassLevel> classes = new ArrayList<>();
		for (PCClass pcClass : dataSet.getClasses())
		{
			int level = character.getClassLevel(pcClass);
			if (level > 0)
			{
				classes.add(new ClassLevel(pcClass.getDisplayName(), level));
			}
		}

		List<String> todo = new ArrayList<>();
		for (TodoFacade item : character.getTodoList())
		{
			todo.add(item.getMessageKey());
		}

		return new CharacterSummary(
			character.getNameRef().get(),
			race == null ? null : race.getDisplayName(),
			alignment == null ? null : alignment.getDisplayName(),
			deity == null ? null : deity.getDisplayName(),
			Map.copyOf(scores),
			Map.copyOf(modifiers),
			List.copyOf(classes),
			hp == null ? 0 : hp,
			orZero(character.getXPRef().get()),
			orZero(character.getXPForNextLevelRef().get()),
			character.getCarriedWeightRef().get(),
			character.getLoadRef().get(),
			List.copyOf(todo));
	}

	private static int orZero(Integer value)
	{
		return value == null ? 0 : value;
	}
}
