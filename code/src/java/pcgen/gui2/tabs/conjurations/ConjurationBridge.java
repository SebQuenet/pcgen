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
package pcgen.gui2.tabs.conjurations;

import pcgen.facade.core.ConjurationSheetFacade;

/**
 * What the conjurations page can call back into. Planted on the page's window
 * object so that conjuring, wounding and dismissing a creature reaches the
 * character.
 *
 * <p>
 * Public, with public methods, because JavaScript running in the WebView
 * reaches it by reflection. The same page opened in an ordinary browser finds
 * no bridge and keeps the fight to itself.
 */
public final class ConjurationBridge
{

	private final ConjurationSheetFacade sheet;

	/**
	 * @param sheet the conjurations to write through. Never null.
	 */
	public ConjurationBridge(ConjurationSheetFacade sheet)
	{
		if (sheet == null)
		{
			throw new IllegalArgumentException("A conjuration bridge needs conjurations to write through");
		}
		this.sheet = sheet;
	}

	/**
	 * Called from the page when a creature of the catalog is conjured.
	 *
	 * @param creature the name of its stat line.
	 * @return the number it was given.
	 */
	public int conjure(String creature)
	{
		return sheet.conjure(creature);
	}

	/**
	 * Called from the page when a creature in play is harmed or healed.
	 *
	 * @param creature its name.
	 * @param number   its number.
	 * @param damage   the total damage it has taken.
	 */
	public void setDamage(String creature, int number, int damage)
	{
		sheet.setDamage(creature, number, damage);
	}

	/**
	 * Called from the page when the rounds left to a creature are corrected.
	 *
	 * @param creature its name.
	 * @param number   its number.
	 * @param rounds   the rounds it has left.
	 */
	public void setRoundsLeft(String creature, int number, int rounds)
	{
		sheet.setRoundsLeft(creature, number, rounds);
	}

	/**
	 * Called from the page when the fight moves on a round.
	 */
	public void nextRound()
	{
		sheet.nextRound();
	}

	/**
	 * Called from the page when a creature is sent away.
	 *
	 * @param creature its name.
	 * @param number   its number.
	 */
	public void dismiss(String creature, int number)
	{
		sheet.dismiss(creature, number);
	}

	/**
	 * Called from the page when every creature is sent away.
	 */
	public void dismissAll()
	{
		sheet.dismissAll();
	}

	/**
	 * Called from the page when the duration given to new creatures changes.
	 *
	 * @param rounds how many rounds a new creature stays, 0 not to count them.
	 */
	public void setDuration(int rounds)
	{
		sheet.setDuration(rounds);
	}
}
