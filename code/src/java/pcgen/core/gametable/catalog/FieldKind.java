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
package pcgen.core.gametable.catalog;

/**
 * What sort of value a field of the bestiary holds, which decides the operators a criterion may use on it.
 */
public enum FieldKind
{
	/** A number, such as a challenge rating or an armor class. */
	NUMBER,
	/** A text, such as a name. */
	TEXT,
	/** A set of names, such as subtypes or languages. */
	ITEMS,
	/** Numbers by name, such as skill totals or energy resistances. */
	SCORES,
	/** Yes or no. */
	FLAG
}
