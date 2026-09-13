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
package pcgen.gui2.facade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pcgen.core.PlayerCharacter;
import pcgen.core.tactics.TacticalParseError;
import pcgen.core.tactics.TacticalParseFailure;
import pcgen.core.tactics.TacticalParseSuccess;
import pcgen.core.tactics.TacticalPlanParser;
import pcgen.core.tactics.TacticalSessionState;
import pcgen.facade.core.TacticalSheetFacade;

/**
 * Edits the tactical sheet of a character on behalf of the Tactical tab.
 *
 * <p>
 * The character is the source of truth: every edit is written straight through
 * to it, and nothing is cached here that the character could contradict.
 */
class TacticalSheetFacadeImpl implements TacticalSheetFacade
{

	private final PlayerCharacter theCharacter;

	TacticalSheetFacadeImpl(PlayerCharacter pc)
	{
		theCharacter = pc;
	}

	@Override
	public String getPlanSource()
	{
		return theCharacter.getDisplay().getTacticalPlan().orElse("");
	}

	@Override
	public void setPlanSource(String source)
	{
		if (source == null || source.isBlank())
		{
			theCharacter.clearTacticalSheet();
			return;
		}
		theCharacter.setTacticalPlan(source);
	}

	@Override
	public List<TacticalParseError> errorsIn(String source)
	{
		if (source == null || source.isBlank())
		{
			return List.of();
		}
		return switch (TacticalPlanParser.parse(source))
		{
			case TacticalParseSuccess ignored -> List.of();
			case TacticalParseFailure failure -> failure.errors();
		};
	}

	@Override
	public void setDamage(int damage)
	{
		TacticalSessionState session = theCharacter.getDisplay().getTacticalSession();
		theCharacter.setTacticalSession(new TacticalSessionState(Math.max(damage, 0), session.resourcesSpent()));
	}

	@Override
	public void spend(String label, int count)
	{
		TacticalSessionState session = theCharacter.getDisplay().getTacticalSession();
		Map<String, Integer> spent = new HashMap<>(session.resourcesSpent());
		if (count <= 0)
		{
			spent.remove(label);
		}
		else
		{
			spent.put(label, count);
		}
		theCharacter.setTacticalSession(new TacticalSessionState(session.damageTaken(), spent));
	}
}
