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
package pcgen.core.conjurations;

import java.util.ArrayList;
import java.util.List;

import pcgen.core.Globals;
import pcgen.core.PCTemplate;
import pcgen.core.Race;
import pcgen.rules.context.AbstractReferenceContext;

/**
 * Checks a catalog against the loaded sources: every race and template it
 * names must be there, or no creature can be built from it.
 */
public final class CatalogResolution
{
	private CatalogResolution()
	{
	}

	/**
	 * @param catalog a catalog that reads.
	 * @return one error per race or template the loaded sources lack, on the line of its summon.
	 */
	public static List<SummonParseError> unresolvedIn(SummonCatalog catalog)
	{
		AbstractReferenceContext data = Globals.getContext().getReferenceContext();
		List<SummonParseError> errors = new ArrayList<>();
		for (SummonGroup group : catalog.groups())
		{
			for (SummonTemplate summon : group.summons())
			{
				if (data.silentlyGetConstructedCDOMObject(Race.class, summon.raceKey()) == null)
				{
					errors.add(new SummonParseError(summon.line(),
						"The loaded sources hold no race '" + summon.raceKey() + "'"));
				}
				for (String template : summon.templateKeys())
				{
					if (data.silentlyGetConstructedCDOMObject(PCTemplate.class, template) == null)
					{
						errors.add(new SummonParseError(summon.line(),
							"The loaded sources hold no template '" + template + "'"));
					}
				}
			}
		}
		return errors;
	}
}
