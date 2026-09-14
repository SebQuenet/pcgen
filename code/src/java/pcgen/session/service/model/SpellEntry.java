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
package pcgen.session.service.model;

/**
 * A spell as a caster sees it on a list: what it is called, what level it is at
 * for which class, and the figures a player reads at the table. Count and the
 * list it sits on are filled in only where they mean something - a prepared
 * spell or one written in a book.
 */
public record SpellEntry(
	String key,
	String name,
	String level,
	String characterClass,
	String school,
	String subschool,
	String components,
	String range,
	String duration,
	String castTime,
	Integer count,
	String spellList)
{
}
