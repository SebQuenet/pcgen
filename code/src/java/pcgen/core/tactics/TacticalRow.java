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
 * One labelled row of a creature's stat block.
 *
 * <p>
 * The label is free text rather than one of a fixed set, because stat blocks
 * differ from creature to creature and the model should not have to change
 * when one wants a row nobody anticipated.
 *
 * @param label   the row's heading, such as "Def" or "DR/SR". Never blank.
 * @param content the row's contents. Never blank.
 */
public record TacticalRow(String label, String content)
{
	public TacticalRow
	{
		label = TacticalText.required(label, "label");
		content = TacticalText.required(content, "content");
	}
}
