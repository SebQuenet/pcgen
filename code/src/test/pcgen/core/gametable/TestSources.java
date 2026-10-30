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
package pcgen.core.gametable;

import java.util.Collection;
import java.util.List;

import pcgen.core.Campaign;
import pcgen.facade.util.ListFacades;
import pcgen.session.HeadlessUIDelegate;
import pcgen.session.PcgenSession;
import pcgen.system.FacadeFactory;
import pcgen.util.TestHelper;

/**
 * Pathfinder sources a test class loads for itself. Each slow test class runs in its own JVM, so a class
 * may load another set than the conjuration tests' Bestiaries.
 */
final class TestSources
{
	private static final String TEST_CONFIG_FILE = "config.ini.junit-conjuration";
	private static final String GAME_MODE = "Pathfinder_RPG";

	private TestSources()
	{
	}

	/**
	 * @param campaigns the keys of the sources to load.
	 * @return the campaigns loaded.
	 */
	static Collection<Campaign> load(List<String> campaigns)
	{
		TestHelper.loadGameModes(TEST_CONFIG_FILE);
		FacadeFactory.initialize();
		PcgenSession session = new PcgenSession();
		session.loadSources(GAME_MODE, campaigns);
		return ListFacades.wrap(session.getCurrentDataSet().getCampaigns());
	}

	/**
	 * @param campaigns the keys of the sources to load.
	 * @return a forge building monsters on them.
	 */
	static MonsterForge forge(List<String> campaigns)
	{
		return new MonsterForge(load(campaigns), new HeadlessUIDelegate());
	}
}
