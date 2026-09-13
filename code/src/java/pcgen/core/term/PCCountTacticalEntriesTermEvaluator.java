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
package pcgen.core.term;

import pcgen.core.display.CharacterDisplay;

/**
 * Resolves {@code COUNT[TACTICENTRIES]} to the number of entries in the
 * character's tactical sheet, across every section.
 */
public class PCCountTacticalEntriesTermEvaluator extends BasePCDTermEvaluator implements TermEvaluator
{
	public PCCountTacticalEntriesTermEvaluator(String originalText)
	{
		this.originalText = originalText;
	}

	@Override
	public Float resolve(CharacterDisplay display)
	{
		return (float) display.getTacticalStepCount();
	}

	@Override
	public boolean isSourceDependant()
	{
		return false;
	}

	public boolean isStatic()
	{
		return false;
	}
}
