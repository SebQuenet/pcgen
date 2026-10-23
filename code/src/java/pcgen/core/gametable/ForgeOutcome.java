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
 * Something read from a monster once built, or why the monster cannot be built.
 *
 * @param <T> what is read
 */
public sealed interface ForgeOutcome<T>
{
	/**
	 * @param value what was read
	 */
	record Built<T>(T value) implements ForgeOutcome<T>
	{
	}

	/**
	 * @param reason why the monster cannot be built
	 */
	record Failed<T>(String reason) implements ForgeOutcome<T>
	{
	}
}
