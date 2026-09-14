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
package pcgen.session.service;

import pcgen.cdom.base.Identified;

/**
 * Finds a piece of loaded game data by the name a caller used for it.
 *
 * <p>
 * Callers name things either by the key the data files use or by the name a
 * player would read, and PCGen matches both without regard to case.
 */
public final class DataSetLookup
{
	private DataSetLookup()
	{
	}

	/**
	 * The entry a caller named, or a failure saying what kind of thing was not found.
	 *
	 * @param kind what to call this sort of entry when it is missing, e.g. "Race"
	 * @param wanted the key or display name the caller used
	 * @param candidates everything of that kind the loaded data holds
	 */
	public static <T extends Identified> ServiceResult<T> require(String kind, String wanted, Iterable<T> candidates)
	{
		for (T candidate : candidates)
		{
			if (candidate.getKeyName().equalsIgnoreCase(wanted) || candidate.getDisplayName().equalsIgnoreCase(wanted))
			{
				return ServiceResult.success(candidate);
			}
		}
		return ServiceResult.failure(new ServiceError.EntryNotFound(kind, wanted));
	}
}
