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
package pcgen.facade.core;

import java.util.List;

import pcgen.core.conjurations.SummonParseError;

/**
 * The conjurations of a character, between the user interface and the core.
 *
 * <p>
 * Two different acts live here. Changing the catalog rewrites its source text,
 * which is what the character stores. Playing the fight brings creatures in,
 * wounds them and sends them away, which is play state kept apart from the
 * catalog. A creature in play is named by its stat line and its number.
 */
public interface ConjurationSheetFacade
{

	/**
	 * @return the catalog as written, empty when the character has none.
	 */
	String getCatalogSource();

	/**
	 * Replaces the catalog with what the writer has typed, whether or not it
	 * reads: losing someone's typing because of a half finished line would be
	 * worse than storing it.
	 *
	 * @param source the catalog's source text. Blank removes the catalog, and
	 *               the fight with it.
	 */
	void setCatalogSource(String source);

	/**
	 * Reads a candidate catalog without storing it, so an editor can say what
	 * is wrong while it is being typed.
	 *
	 * @param source the catalog's source text.
	 * @return every error found, empty when the catalog reads.
	 */
	List<SummonParseError> errorsIn(String source);

	/**
	 * Brings a creature into the fight, unhurt, for the duration set.
	 *
	 * @param creature the name of its stat line in the catalog.
	 * @return its number: the lowest no creature of that name in play holds.
	 */
	int conjure(String creature);

	/**
	 * @param creature the name of a creature in play.
	 * @param number   its number.
	 * @param damage   the total damage it has taken; below zero counts as none.
	 */
	void setDamage(String creature, int number, int damage);

	/**
	 * @param creature the name of a creature in play.
	 * @param number   its number.
	 * @param rounds   how many rounds it has left; below zero counts as none.
	 */
	void setRoundsLeft(String creature, int number, int rounds);

	/**
	 * Takes a round from every creature whose rounds are counted.
	 */
	void nextRound();

	/**
	 * @param creature the name of a creature in play.
	 * @param number   its number.
	 */
	void dismiss(String creature, int number);

	/**
	 * Sends every creature away. The duration set stays.
	 */
	void dismissAll();

	/**
	 * @param rounds how many rounds a newly conjured creature stays; zero or
	 *               less not to count them.
	 */
	void setDuration(int rounds);
}
