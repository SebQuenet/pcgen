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
package pcgen.gui2.tabs.tactics;

import pcgen.facade.core.TacticalSheetFacade;

/**
 * What the rendered sheet can call back into. Planted on the page's window
 * object so that ticking a pip or taking damage reaches the character.
 *
 * <p>
 * Public, with public methods, because JavaScript running in the WebView
 * reaches it by reflection. The same page opened in an ordinary browser finds
 * no bridge and keeps its state to itself.
 */
public final class TacticalBridge
{

	private final TacticalSheetFacade sheet;

	/**
	 * @param sheet the sheet to write through. Never null.
	 */
	public TacticalBridge(TacticalSheetFacade sheet)
	{
		if (sheet == null)
		{
			throw new IllegalArgumentException("A tactical bridge needs a sheet to write through");
		}
		this.sheet = sheet;
	}

	/**
	 * Called from the page when the damage control is used.
	 *
	 * @param damage total damage taken.
	 */
	public void setDamage(int damage)
	{
		sheet.setDamage(damage);
	}

	/**
	 * Called from the page when a resource's pips are ticked.
	 *
	 * @param label the resource's label.
	 * @param count how many of its uses are gone.
	 */
	public void spend(String label, int count)
	{
		sheet.spend(label, count);
	}
}
