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
package pcgen.session;

import java.util.logging.Level;

import pcgen.persistence.CampaignFileLoader;
import pcgen.persistence.GameModeFileLoader;
import pcgen.system.ConfigurationSettings;
import pcgen.system.FacadeFactory;
import pcgen.system.Main;
import pcgen.system.PCGenTaskExecutor;
import pcgen.util.Logging;

/**
 * Brings PCGen up with no graphical interface: settings, plugins, game modes and
 * the campaign index. Every headless entry point starts here.
 *
 * <p>
 * Loading the campaign index is cheap; loading the data of a given campaign is
 * not, and happens later through {@link PcgenSession#loadSources}.
 */
public final class HeadlessBootstrap
{
	private HeadlessBootstrap()
	{
	}

	/**
	 * Load settings, plugins, game modes and the campaign index.
	 *
	 * @param settingsDir the directory holding the settings files, or null for the default
	 */
	public static void start(String settingsDir)
	{
		String resolvedSettingsDir =
			settingsDir != null ? settingsDir : ConfigurationSettings.getDefaultSettingsFilesPath();
		ConfigurationSettings.setSystemProperty(ConfigurationSettings.SETTINGS_FILES_PATH, resolvedSettingsDir);

		Main.loadProperties(false);

		Logging.log(Level.INFO, "Loading plugins and game data...");
		PCGenTaskExecutor executor = new PCGenTaskExecutor();
		executor.addPCGenTask(Main.createLoadPluginTask());
		executor.addPCGenTask(new GameModeFileLoader());
		executor.addPCGenTask(new CampaignFileLoader());
		executor.run();

		FacadeFactory.initialize();
		Logging.log(Level.INFO, "PCGen data loaded successfully.");
	}

	/**
	 * Read the settings directory from the command line, if it names one.
	 *
	 * @param args the process arguments
	 * @return the directory, or null when no {@code -s} or {@code --settingsdir} flag is present
	 */
	public static String settingsDirFrom(String... args)
	{
		for (int i = 0; i < args.length - 1; i++)
		{
			if ("-s".equals(args[i]) || "--settingsdir".equals(args[i]))
			{
				return args[i + 1];
			}
		}
		return null;
	}
}
