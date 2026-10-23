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
package pcgen.core.gametable;

/**
 * Something the turn passing wore off.
 */
public sealed interface Expiry
{
	/** @return the name of the combatant it concerned. */
	String combatant();

	/** A conjured creature whose time ran out and left the fight. */
	record CreatureLeft(String combatant) implements Expiry
	{
	}

	/** A condition that ended with a new round. */
	record ConditionEnded(String combatant, String condition) implements Expiry
	{
	}

	/** A spell the combatant cast, ended as its turn began. */
	record SpellEnded(String combatant, String spell) implements Expiry
	{
	}
}
