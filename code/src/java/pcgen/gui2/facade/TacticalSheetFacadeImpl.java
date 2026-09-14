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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import pcgen.core.PlayerCharacter;
import pcgen.core.tactics.TacticalParseError;
import pcgen.core.tactics.TacticalParseFailure;
import pcgen.core.tactics.TacticalParseSuccess;
import pcgen.core.tactics.TacticalPlanParser;
import pcgen.core.tactics.TacticalSessionState;
import pcgen.facade.core.CharacterFacade;
import pcgen.facade.core.TacticalSheetFacade;
import pcgen.facade.core.TempBonusFacade;

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
	private final CharacterFacade characterFacade;

	TacticalSheetFacadeImpl(PlayerCharacter pc, CharacterFacade facade)
	{
		theCharacter = pc;
		characterFacade = facade;
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
		theCharacter.setTacticalSession(
			new TacticalSessionState(Math.max(damage, 0), session.resourcesSpent(), session.activeBuffs()));
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
		theCharacter.setTacticalSession(
			new TacticalSessionState(session.damageTaken(), spent, session.activeBuffs()));
	}

	@Override
	public void setBuffActive(String label, boolean active)
	{
		TacticalSessionState session = theCharacter.getDisplay().getTacticalSession();
		Set<String> running = new HashSet<>(session.activeBuffs());
		if (active)
		{
			running.add(label);
		}
		else
		{
			running.remove(label);
		}
		theCharacter.setTacticalSession(
			new TacticalSessionState(session.damageTaken(), session.resourcesSpent(), running));
	}

	@Override
	public boolean applyTemporaryBonus(String name, boolean active)
	{
		Optional<TempBonusFacade> held = availableBonusNamed(name);
		held.ifPresent(bonus -> characterFacade.setTempBonusActive(bonus, active));
		return held.isPresent();
	}

	private Optional<TempBonusFacade> availableBonusNamed(String name)
	{
		if (characterFacade == null || name == null || name.isBlank())
		{
			return Optional.empty();
		}
		for (TempBonusFacade bonus : characterFacade.getTempBonuses())
		{
			if (bonus.toString().equalsIgnoreCase(name.strip()))
			{
				return Optional.of(bonus);
			}
		}
		return Optional.empty();
	}
}
