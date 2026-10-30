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
 * Whether an entry meets a criterion: yes, no, or unknown while a value it needs is not computed yet.
 */
public enum Match
{
	YES,
	NO,
	UNKNOWN;

	/**
	 * @param other the other answer.
	 * @return both: a no wins, then an unknown.
	 */
	public Match and(Match other)
	{
		if (this == NO || other == NO)
		{
			return NO;
		}
		return (this == UNKNOWN || other == UNKNOWN) ? UNKNOWN : YES;
	}

	/**
	 * @param other the other answer.
	 * @return either: a yes wins, then an unknown.
	 */
	public Match or(Match other)
	{
		if (this == YES || other == YES)
		{
			return YES;
		}
		return (this == UNKNOWN || other == UNKNOWN) ? UNKNOWN : NO;
	}

	/**
	 * @param met whether a known value meets the test.
	 * @return yes or no.
	 */
	public static Match of(boolean met)
	{
		return met ? YES : NO;
	}
}
