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
 * What kind of thing a tactical block points at when it borrows the
 * character's own numbers.
 *
 * <p>
 * Only kinds PCGen can actually answer are listed. A name pointing at
 * something PCGen has no concept of would resolve to nothing, so the sheet
 * writes such a value out instead.
 */
public enum ReferenceKind
{
	/** An equipped weapon, for its attack bonus, damage and critical. */
	WEAPON,
	/** A variable PCGen computes, for a number such as uses per day. */
	VAR
}
