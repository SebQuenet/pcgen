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

import pcgen.core.tactics.TacticalParseError;

/**
 * The tactical sheet of a character, between the user interface and the core.
 *
 * <p>
 * Two different acts live here. Changing the plan rewrites its source text,
 * which is what the character stores. Playing the session ticks off damage and
 * resources, which is play state kept apart from the plan.
 */
public interface TacticalSheetFacade
{

	/**
	 * The source text of the character's plan.
	 *
	 * @return the plan as written, empty when the character has none.
	 */
	String getPlanSource();

	/**
	 * Replaces the plan with what the writer has typed, whether or not it
	 * reads: losing someone's typing because of a half finished line would be
	 * worse than storing it.
	 *
	 * @param source the plan's source text. Blank removes the plan.
	 */
	void setPlanSource(String source);

	/**
	 * Reads a candidate plan without storing it, so an editor can say what is
	 * wrong while it is being typed.
	 *
	 * @param source the plan's source text.
	 * @return every error found, empty when the plan reads.
	 */
	List<TacticalParseError> errorsIn(String source);

	/**
	 * Records damage the character has taken.
	 *
	 * @param damage total damage taken, never negative.
	 */
	void setDamage(int damage);

	/**
	 * Records how much of a resource is gone.
	 *
	 * @param label the resource's label, as the plan writes it.
	 * @param count how many of its uses are gone, never negative.
	 */
	void spend(String label, int count);
}
