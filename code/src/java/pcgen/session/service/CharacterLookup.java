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

import pcgen.cdom.enumeration.CharID;
import pcgen.core.PlayerCharacter;
import pcgen.facade.core.CharacterFacade;
import pcgen.session.HeadlessUIDelegate;
import pcgen.session.PcgenSession;

/**
 * Finds the character a call names, and says so plainly when there is none.
 *
 * <p>
 * Looking a character up also points PCGen's one global chooser delegate at that
 * character's own, which is why every service goes through here rather than
 * holding on to a character between calls.
 */
public final class CharacterLookup
{
	private final PcgenSession session;

	public CharacterLookup(PcgenSession session)
	{
		this.session = session;
	}

	public ServiceResult<CharacterFacade> byId(String characterId)
	{
		try
		{
			return ServiceResult.success(session.getCharacter(characterId));
		}
		catch (IllegalArgumentException e)
		{
			return ServiceResult.failure(new ServiceError.CharacterNotFound(characterId));
		}
	}

	public ServiceResult<PlayerCharacter> playerCharacterById(String characterId)
	{
		return byId(characterId).map(character -> session.getPlayerCharacter(characterId));
	}

	public ServiceResult<HeadlessUIDelegate> delegateById(String characterId)
	{
		return byId(characterId).andThen(character -> {
			HeadlessUIDelegate delegate = session.getDelegate(characterId);
			return delegate == null
				? ServiceResult.failure(new ServiceError.CharacterNotFound(characterId))
				: ServiceResult.success(delegate);
		});
	}

	public CharID charIdOf(CharacterFacade character)
	{
		return character.getCharID();
	}

	public PcgenSession session()
	{
		return session;
	}
}
