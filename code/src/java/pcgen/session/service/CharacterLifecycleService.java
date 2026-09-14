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

import java.io.File;
import java.util.Map;

import pcgen.facade.core.CharacterFacade;
import pcgen.session.PcgenSession;
import pcgen.session.service.model.CharacterClosed;
import pcgen.session.service.model.CharacterCreated;
import pcgen.session.service.model.CharacterOpened;
import pcgen.session.service.model.CharacterSaved;
import pcgen.session.service.model.CharacterSummary;

/**
 * Opening, reading, saving and closing the characters a session holds.
 */
public final class CharacterLifecycleService
{
	private final PcgenSession session;
	private final CharacterLookup characters;

	public CharacterLifecycleService(PcgenSession session)
	{
		this.session = session;
		this.characters = new CharacterLookup(session);
	}

	public ServiceResult<CharacterCreated> createCharacter(String name)
	{
		try
		{
			return ServiceResult.success(new CharacterCreated(session.createCharacter(name)));
		}
		catch (IllegalStateException e)
		{
			return ServiceResult.failure(new ServiceError.NoSourcesLoaded());
		}
		catch (RuntimeException e)
		{
			return ServiceResult.failure(new ServiceError.DataFailure("Failed to create character: " + e.getMessage()));
		}
	}

	public ServiceResult<CharacterSummary> getCharacter(String characterId)
	{
		return characters.byId(characterId).map(CharacterSummaries::of);
	}

	public ServiceResult<CharacterOpened> openCharacter(String filePath)
	{
		File file = new File(filePath);
		if (!file.isFile())
		{
			return ServiceResult.failure(new ServiceError.EntryNotFound("Character file", filePath));
		}
		try
		{
			PcgenSession.OpenCharacterResult opened = session.openCharacter(file);
			return ServiceResult.success(new CharacterOpened(
				opened.getCharacterId(),
				opened.isSourcesMerged(),
				Map.copyOf(opened.getReopenedCharacters())));
		}
		catch (IllegalStateException e)
		{
			return ServiceResult.failure(new ServiceError.NoSourcesLoaded());
		}
		catch (RuntimeException e)
		{
			return ServiceResult.failure(new ServiceError.DataFailure("Failed to open character: " + e.getMessage()));
		}
	}

	public ServiceResult<CharacterSaved> saveCharacter(String characterId, String filePath)
	{
		return characters.byId(characterId).andThen(character -> {
			if (filePath != null && !filePath.isBlank())
			{
				character.setFile(new File(filePath));
			}
			try
			{
				boolean saved = session.saveCharacter(characterId);
				return ServiceResult.success(new CharacterSaved(saved, pathOf(character)));
			}
			catch (RuntimeException e)
			{
				return ServiceResult.failure(
					new ServiceError.DataFailure("Failed to save character: " + e.getMessage()));
			}
		});
	}

	public ServiceResult<CharacterClosed> closeCharacter(String characterId)
	{
		return characters.byId(characterId).andThen(character -> {
			session.closeCharacter(characterId);
			return ServiceResult.success(new CharacterClosed(characterId));
		});
	}

	private static String pathOf(CharacterFacade character)
	{
		File file = character.getFileRef().get();
		return file == null ? null : file.getAbsolutePath();
	}
}
