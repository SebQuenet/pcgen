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
 * What an image of the scenario shows, in the order it is shown. The first two belong to the
 * scenario, the others to an encounter.
 */
public enum ImageCategory
{
	SCENARIO_ILLUSTRATION(true),
	GENERAL_MAP(true),
	LOCATION_ILLUSTRATION(false),
	BATTLEMAP(false),
	OTHER(false);

	private final boolean isScenarioWide;

	ImageCategory(boolean isScenarioWide)
	{
		this.isScenarioWide = isScenarioWide;
	}

	/**
	 * @return whether images of this kind belong to the whole scenario rather than to an encounter.
	 */
	public boolean isScenarioWide()
	{
		return isScenarioWide;
	}
}
