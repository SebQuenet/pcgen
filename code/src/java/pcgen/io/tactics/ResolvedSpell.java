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
package pcgen.io.tactics;

/**
 * One spell the character holds, read off the character rather than written on
 * the sheet.
 *
 * @param name  the spell's name.
 * @param level the level it is held at.
 * @param book  the spellbook holding it.
 * @param times how many times it is held, so a spell prepared twice says so.
 */
public record ResolvedSpell(String name, int level, String book, int times)
{
}
