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

import java.util.List;
import java.util.function.UnaryOperator;

import pcgen.core.PlayerCharacter;
import pcgen.core.conjurations.ConjurationSession;
import pcgen.core.conjurations.ConjuredCreature;
import pcgen.core.conjurations.SummonCatalogParser;
import pcgen.core.conjurations.SummonParseError;
import pcgen.core.conjurations.SummonParseFailure;
import pcgen.core.conjurations.SummonParseSuccess;
import pcgen.facade.core.ConjurationSheetFacade;

/**
 * Edits the conjurations of a character on behalf of the Conjurations tab and
 * the page it shows.
 *
 * <p>
 * The character is the source of truth: every change is written straight
 * through to it, and nothing is cached here that the character could
 * contradict.
 */
class ConjurationSheetFacadeImpl implements ConjurationSheetFacade
{

	private final PlayerCharacter theCharacter;

	ConjurationSheetFacadeImpl(PlayerCharacter pc)
	{
		theCharacter = pc;
	}

	@Override
	public String getCatalogSource()
	{
		return theCharacter.getDisplay().getConjurationCatalog().orElse("");
	}

	@Override
	public void setCatalogSource(String source)
	{
		if (source == null || source.isBlank())
		{
			theCharacter.clearConjurations();
			return;
		}
		theCharacter.setConjurationCatalog(source);
	}

	@Override
	public List<SummonParseError> errorsIn(String source)
	{
		if (source == null || source.isBlank())
		{
			return List.of();
		}
		return switch (SummonCatalogParser.parse(source))
		{
			case SummonParseSuccess ignored -> List.of();
			case SummonParseFailure failure -> failure.errors();
		};
	}

	@Override
	public int conjure(String creature)
	{
		ConjurationSession fight = theCharacter.getDisplay().getConjurationSession();
		ConjuredCreature arriving = fight.arrival(creature);
		theCharacter.setConjurationSession(fight.with(arriving));
		return arriving.number();
	}

	@Override
	public void setDamage(String creature, int number, int damage)
	{
		change(fight -> fight.withDamage(creature, number, Math.max(damage, 0)));
	}

	@Override
	public void setRoundsLeft(String creature, int number, int rounds)
	{
		change(fight -> fight.withRoundsLeft(creature, number, Math.max(rounds, 0)));
	}

	@Override
	public void nextRound()
	{
		change(ConjurationSession::nextRound);
	}

	@Override
	public void dismiss(String creature, int number)
	{
		change(fight -> fight.dismiss(creature, number));
	}

	@Override
	public void dismissAll()
	{
		change(ConjurationSession::dismissAll);
	}

	@Override
	public void setDuration(int rounds)
	{
		change(fight -> fight.withDuration(Math.max(rounds, 0)));
	}

	private void change(UnaryOperator<ConjurationSession> change)
	{
		theCharacter.setConjurationSession(change.apply(theCharacter.getDisplay().getConjurationSession()));
	}
}
