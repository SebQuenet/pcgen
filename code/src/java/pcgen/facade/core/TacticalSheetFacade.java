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

import pcgen.core.tactics.TacticalSection;
import pcgen.facade.util.ListFacade;

/**
 * Tracks the tactical sheet of a character — what to do round by round and in
 * which circumstances — between the user interface and the core.
 *
 * <p>
 * Sections are held in reading order and edited one at a time: the user
 * interface never rebuilds the whole sheet to change one line.
 */
public interface TacticalSheetFacade
{

	/**
	 * Retrieves the sections of the character's tactical sheet, in reading
	 * order. The list is empty when the character has no tactical sheet.
	 *
	 * @return the sections of the sheet.
	 */
	ListFacade<TacticalSection> getSections();

	/**
	 * Adds a section at the end of the sheet, holding one placeholder line for
	 * the user to fill in. A section is never empty, so a new one comes with a
	 * line already.
	 *
	 * @param title the heading of the new section.
	 */
	void addSection(String title);

	/**
	 * Replaces the section at a position, keeping that position in the sheet.
	 * A position outside the sheet changes nothing — two sections may hold the
	 * same title and the same lines, so sections are addressed by where they
	 * are rather than by what they contain.
	 *
	 * @param position    where the section sits, counting from zero.
	 * @param replacement what to put in its place.
	 */
	void replaceSection(int position, TacticalSection replacement);

	/**
	 * Removes the section at a position. A position outside the sheet changes
	 * nothing.
	 *
	 * @param position where the section sits, counting from zero.
	 */
	void removeSection(int position);
}
