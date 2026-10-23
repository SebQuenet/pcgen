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

import java.util.Collection;
import java.util.List;

import pcgen.core.Campaign;
import pcgen.core.PlayerCharacter;
import pcgen.facade.util.ListFacades;
import pcgen.session.PcgenSession;
import pcgen.system.FacadeFactory;
import pcgen.util.TestHelper;

/**
 * The real Pathfinder data conjuration tests build creatures from: the Core
 * Rulebook, the Bestiaries, the summoning books and Celmae, loaded once for the whole test run.
 */
public final class BestiarySources
{
	private static final String TEST_CONFIG_FILE = "config.ini.junit-conjuration";
	private static final String GAME_MODE = "Pathfinder_RPG";
	private static final List<String> CAMPAIGNS = List.of("Core Rulebook", "Bestiary", "Bestiary 2", "Bestiary 3", "Bestiary 5",
		"Mythic Adventures", "Monster Summoner's Handbook", "Champions of Purity", "Celmae - Udaeus Race and Paragon Class");

	private static PcgenSession session;

	private BestiarySources()
	{
	}

	/**
	 * @return the session holding the loaded sources, loading them the first time.
	 */
	public static synchronized PcgenSession session()
	{
		if (session == null)
		{
			TestHelper.loadGameModes(TEST_CONFIG_FILE);
			FacadeFactory.initialize();
			session = new PcgenSession();
			session.loadSources(GAME_MODE, CAMPAIGNS);
		}
		return session;
	}

	/**
	 * @return the campaigns the session loaded.
	 */
	public static Collection<Campaign> campaigns()
	{
		return ListFacades.wrap(session().getCurrentDataSet().getCampaigns());
	}

	/**
	 * @param name the new character's name
	 * @return a new character on the loaded sources, open in the session.
	 */
	public static PlayerCharacter newCharacter(String name)
	{
		PcgenSession loaded = session();
		return loaded.getPlayerCharacter(loaded.createCharacter(name));
	}
}
