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
package pcgen.persistence.translation;

/**
 * The parts of a game object a translation table can give in another language.
 */
public enum TranslatedField
{
	NAME, OUTPUTNAME, DESC, BENEFIT, TEMPDESC, ABB, TEMPVALUE_TITLE;

	/**
	 * @return whether an object can carry several texts in this field, each under its own prerequisites
	 */
	public boolean isDescription()
	{
		return this == DESC || this == BENEFIT;
	}
}
