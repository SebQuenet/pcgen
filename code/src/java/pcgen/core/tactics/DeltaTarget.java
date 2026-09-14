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
 * What a buff's declared delta changes on the sheet.
 *
 * <p>
 * A closed set, because a target the sheet cannot find would otherwise be a
 * delta that silently changes nothing.
 */
public enum DeltaTarget
{
	/** Every attack line's attack bonus. */
	ATTACK,
	/** Every attack line's damage. */
	DAMAGE,
	/** Armour class. */
	AC,
	/** Touch armour class. */
	TOUCH,
	/** Flat footed armour class. */
	FLAT,
	/** The Fortitude save. */
	FORTITUDE,
	/** The Reflex save. */
	REFLEX,
	/** The Will save. */
	WILL,
	/** Initiative. */
	INITIATIVE,
	/** Speed. */
	SPEED
}
