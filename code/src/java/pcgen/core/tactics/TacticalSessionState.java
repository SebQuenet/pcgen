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

import java.util.Map;
import java.util.Set;

/**
 * What the session has used up so far: damage the character has taken, and how
 * much of each tactical resource is gone.
 *
 * <p>
 * This is play state, kept apart from the plan, because a plan is not a
 * counter. It is the only play state PCGen holds.
 *
 * @param damageTaken    damage taken, never negative.
 * @param resourcesSpent how many uses are gone, keyed by the resource's label.
 *                       No count is negative, and the map is never modifiable
 *                       through the one passed in.
 * @param activeBuffs    the labels of the buffs currently up. A fight survives
 *                       a pause, so which buffs are running is worth keeping.
 */
public record TacticalSessionState(int damageTaken, Map<String, Integer> resourcesSpent, Set<String> activeBuffs)
{
	public TacticalSessionState
	{
		activeBuffs = (activeBuffs == null) ? Set.of() : Set.copyOf(activeBuffs);
		if (damageTaken < 0)
		{
			throw new IllegalArgumentException("Damage taken cannot be negative, and " + damageTaken + " is");
		}
		resourcesSpent = (resourcesSpent == null) ? Map.of() : Map.copyOf(resourcesSpent);
		for (Map.Entry<String, Integer> spent : resourcesSpent.entrySet())
		{
			if (spent.getValue() == null || spent.getValue() < 0)
			{
				throw new IllegalArgumentException(
					"Resource '" + spent.getKey() + "' cannot have spent " + spent.getValue() + " uses");
			}
		}
	}

	/**
	 * @return a session where nothing has happened yet
	 */
	public static TacticalSessionState untouched()
	{
		return new TacticalSessionState(0, Map.of(), Set.of());
	}

	/**
	 * @param label the resource's label
	 * @return how many of its uses are gone, zero for a resource never touched
	 */
	public int spent(String label)
	{
		return resourcesSpent.getOrDefault(label, 0);
	}

	/**
	 * @return true when nothing has been used up, so there is nothing to save
	 */
	public boolean isUntouched()
	{
		return damageTaken == 0 && activeBuffs.isEmpty()
			&& resourcesSpent.values().stream().allMatch(count -> count == 0);
	}

	/**
	 * @param label the buff's label
	 * @return true when that buff is up
	 */
	public boolean isBuffActive(String label)
	{
		return activeBuffs.contains(label);
	}
}
