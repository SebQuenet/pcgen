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
package pcgen.cdom.facet;

import java.util.List;

import pcgen.cdom.enumeration.CharID;
import pcgen.cdom.facet.base.AbstractItemFacet;
import pcgen.cdom.testsupport.AbstractItemFacetTest;
import pcgen.core.tactics.TacticalEntry;
import pcgen.core.tactics.TacticalSection;
import pcgen.core.tactics.TacticalSheet;

public class TacticalSheetFacetTest extends AbstractItemFacetTest<TacticalSheet>
{

	private final TacticalSheetFacet facet = new TacticalSheetFacet();

	private int sheetCount = 0;

	@Override
	protected AbstractItemFacet<CharID, TacticalSheet> getFacet()
	{
		return facet;
	}

	@Override
	protected TacticalSheet getItem()
	{
		sheetCount++;
		TacticalEntry entry = new TacticalEntry("Round " + sheetCount, "Attack " + sheetCount, "");
		return new TacticalSheet(List.of(new TacticalSection("Opening " + sheetCount, List.of(entry))));
	}
}
