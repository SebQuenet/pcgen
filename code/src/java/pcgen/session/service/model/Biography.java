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
 * Everything about a character that is not a rule: who they are, what they look
 * like, and the notes kept about them.
 */
public record Biography(
	String name,
	String playersName,
	String gender,
	Integer age,
	String ageCategory,
	Integer weight,
	String hairColor,
	String eyeColor,
	String skinColor,
	String handed,
	String bio,
	String description,
	String companions,
	String assets,
	String magic,
	String gmNotes)
{
}
