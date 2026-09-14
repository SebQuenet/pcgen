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

import java.util.ArrayList;
import java.util.List;

import pcgen.core.Campaign;
import pcgen.core.GameMode;
import pcgen.session.PcgenSession;
import pcgen.session.service.model.GameModeSummary;
import pcgen.session.service.model.NamedEntry;
import pcgen.session.service.model.SourcesLoaded;

/**
 * What game data is available, and which of it is loaded.
 *
 * <p>
 * Only one source set can be loaded at a time: loading a second one empties the
 * global reference context that the first one filled.
 */
public final class SourceService
{
	private final PcgenSession session;

	public SourceService(PcgenSession session)
	{
		this.session = session;
	}

	public ServiceResult<List<GameModeSummary>> listGameModes()
	{
		List<GameModeSummary> modes = new ArrayList<>();
		for (GameMode mode : session.getGameModes())
		{
			modes.add(new GameModeSummary(mode.getName(), mode.getDisplayName()));
		}
		return ServiceResult.success(List.copyOf(modes));
	}

	public ServiceResult<List<NamedEntry>> listSources(String gameModeName)
	{
		GameMode gameMode = session.findGameMode(gameModeName);
		if (gameMode == null)
		{
			return ServiceResult.failure(new ServiceError.EntryNotFound("Game mode", gameModeName));
		}
		List<NamedEntry> campaigns = new ArrayList<>();
		for (Campaign campaign : session.getSupportedCampaigns(gameMode))
		{
			campaigns.add(new NamedEntry(campaign.getKeyName(), campaign.getDisplayName()));
		}
		return ServiceResult.success(List.copyOf(campaigns));
	}

	public ServiceResult<SourcesLoaded> loadSources(String gameModeName, List<String> campaignKeys)
	{
		try
		{
			PcgenSession.LoadSourcesResult loaded = session.loadSources(gameModeName, campaignKeys);
			List<String> autoAdded = new ArrayList<>();
			for (Campaign campaign : loaded.getResolved().getAutoAdded())
			{
				autoAdded.add(campaign.getKeyName());
			}
			return ServiceResult.success(new SourcesLoaded(
				loaded.getSourceSetId(),
				List.copyOf(autoAdded),
				List.copyOf(loaded.getResolved().getWarnings())));
		}
		catch (IllegalArgumentException e)
		{
			return ServiceResult.failure(new ServiceError.InvalidArgument("campaigns", e.getMessage()));
		}
		catch (RuntimeException e)
		{
			return ServiceResult.failure(new ServiceError.DataFailure("Failed to load sources: " + e.getMessage()));
		}
	}
}
