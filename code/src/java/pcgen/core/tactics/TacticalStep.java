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
 * One step of the rotation: the circumstance the character is in, and what the
 * character does about it.
 *
 * @param trigger the circumstance in plain words, such as "Round 1", "enemy at
 *                range" or "HP below 12". Never blank.
 * @param actions what the character does. Never blank.
 * @param note    extra detail, empty when there is none.
 */
public record TacticalStep(String trigger, String actions, String note) implements TacticalBlock
{
	public TacticalStep
	{
		trigger = TacticalText.required(trigger, "trigger");
		actions = TacticalText.required(actions, "actions");
		note = TacticalText.optional(note);
	}
}
