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
package pcgen.gui2.gametable;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.HyperlinkEvent;
import javax.swing.filechooser.FileNameExtensionFilter;

import pcgen.core.Campaign;
import pcgen.core.GameMode;
import pcgen.core.Globals;
import pcgen.core.SystemCollections;
import pcgen.core.gametable.GameTableSession;
import pcgen.core.gametable.HttpTabletopTransfer;
import pcgen.core.gametable.MonsterForge;
import pcgen.core.gametable.Scenario;
import pcgen.core.gametable.ScenarioReading;
import pcgen.core.gametable.TabletopTransfer;
import pcgen.facade.core.DataSetFacade;
import pcgen.facade.util.ListFacades;
import pcgen.gui2.PCGenFrame;
import pcgen.gui2.tools.DesktopBrowserLauncher;
import pcgen.session.HeadlessUIDelegate;
import pcgen.system.FacadeFactory;
import pcgen.system.LanguageBundle;
import pcgen.system.PCGenSettings;
import pcgen.util.Logging;

/**
 * The game table workspace: the scenario the game master runs, with its party,
 * encounters, fight and bestiary.
 */
public final class GameTablePanel extends JPanel
{
	private static final int DIE_FACES = 20;

	private final PCGenFrame frame;
	private final GameTableSession session;
	private final JLabel scenarioLabel = new JLabel();
	private final JTabbedPane tabs = new JTabbedPane();
	private final PartyPanel partyPanel;
	private final EncountersPanel encountersPanel;
	private final CombatPanel combatPanel;
	private final BestiaryPanel bestiaryPanel;
	private final TabletopTransfer tabletop = HttpTabletopTransfer.fromEnvironment();

	/**
	 * @param frame PCGen's main window, which loads sources and opens characters.
	 */
	public GameTablePanel(PCGenFrame frame)
	{
		super(new BorderLayout());
		this.frame = frame;
		Random random = new SecureRandom();
		this.session = new GameTableSession(() -> random.nextInt(DIE_FACES) + 1);
		this.partyPanel = new PartyPanel(this);
		this.encountersPanel = new EncountersPanel(this);
		this.combatPanel = new CombatPanel(this);
		this.bestiaryPanel = new BestiaryPanel(this);
		tabs.addTab(LanguageBundle.getString("in_gametable_party"), partyPanel);
		tabs.addTab(LanguageBundle.getString("in_gametable_encounters"), encountersPanel);
		tabs.addTab(LanguageBundle.getString("in_gametable_combat"), combatPanel);
		tabs.addTab(LanguageBundle.getString("in_gametable_bestiary"), bestiaryPanel);
		add(toolBar(), BorderLayout.NORTH);
		add(tabs, BorderLayout.CENTER);
		session.addListener(() -> SwingUtilities.invokeLater(this::refresh));
		frame.getLoadedDataSetRef().addReferenceListener(event -> sourcesChanged(event.getNewReference()));
		sourcesChanged(frame.getLoadedDataSetRef().get());
		refresh();
	}

	GameTableSession session()
	{
		return session;
	}

	/**
	 * @return what sends images to Tabletop Simulator.
	 */
	TabletopTransfer tabletop()
	{
		return tabletop;
	}

	PCGenFrame frame()
	{
		return frame;
	}

	EncountersPanel encounters()
	{
		return encountersPanel;
	}

	/** Shows the fight. */
	void showCombat()
	{
		tabs.setSelectedComponent(combatPanel);
	}

	/**
	 * Called when the game master comes back from the Characters workspace, where the
	 * character files of some monsters may have changed.
	 */
	public void workspaceShown()
	{
		inBackground(session::refreshFromFiles, this::reportProblems);
	}

	private JPanel toolBar()
	{
		JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
		bar.add(button("in_gametable_newScenario", this::newScenario));
		bar.add(button("in_gametable_openScenario", this::openScenario));
		bar.add(button("in_gametable_saveScenario", () -> saveScenario(false)));
		bar.add(button("in_gametable_saveScenarioAs", () -> saveScenario(true)));
		bar.add(scenarioLabel);
		return bar;
	}

	static JButton button(String key, Runnable action)
	{
		JButton button = new JButton(LanguageBundle.getString(key));
		button.addActionListener(event -> action.run());
		return button;
	}

	/** Opens a link of a sheet to a rules site in the browser; a link without an address is left alone. */
	static void browse(HyperlinkEvent event)
	{
		if (event.getEventType() != HyperlinkEvent.EventType.ACTIVATED || event.getURL() == null)
		{
			return;
		}
		try
		{
			DesktopBrowserLauncher.viewInBrowser(event.getURL().toURI());
		}
		catch (IOException | URISyntaxException failed)
		{
			Logging.errorPrint("Failed to open " + event.getURL(), failed);
		}
	}

	private void refresh()
	{
		String name = session.file().map(File::getName).orElse(LanguageBundle.getString("in_gametable_untitled"));
		String sources = session.hasSources() ? "" : " — " + LanguageBundle.getString("in_gametable_noSources");
		scenarioLabel.setText(name + (session.isDirty() ? " *" : "") + sources);
		partyPanel.refresh();
		encountersPanel.refresh();
		combatPanel.refresh();
	}

	private void sourcesChanged(DataSetFacade dataSet)
	{
		if (dataSet == null)
		{
			session.dropSources();
			bestiaryPanel.refreshSources();
			return;
		}
		List<Campaign> campaigns = ListFacades.wrap(dataSet.getCampaigns());
		List<String> keys = campaigns.stream().map(Campaign::getKeyName).toList();
		String gameMode = dataSet.getGameMode().getName();
		bestiaryPanel.refreshSources();
		// Building a monster may ask the data's questions off the event thread: the first answers are taken
		HeadlessUIDelegate choices = new HeadlessUIDelegate();
		choices.setAutoChoose(true);
		inBackground(() -> session.useSources(new MonsterForge(campaigns, choices), gameMode, keys),
			this::reportProblems);
	}

	private void newScenario()
	{
		if (!mayDiscard())
		{
			return;
		}
		DataSetFacade dataSet = frame.getLoadedDataSetRef().get();
		if (dataSet == null)
		{
			session.newScenario("", List.of());
			return;
		}
		session.newScenario(dataSet.getGameMode().getName(),
			ListFacades.wrap(dataSet.getCampaigns()).stream().map(Campaign::getKeyName).toList());
	}

	private void openScenario()
	{
		if (!mayDiscard())
		{
			return;
		}
		JFileChooser chooser = scenarioChooser();
		if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}
		File chosen = chooser.getSelectedFile();
		ScenarioReading reading = session.read(chosen);
		if (reading instanceof ScenarioReading.Unreadable unreadable)
		{
			showError(LanguageBundle.getFormattedString("in_gametable_unreadableScenario", chosen.getName(),
				unreadable.reason()));
			return;
		}
		Scenario scenario = ((ScenarioReading.Read) reading).scenario();
		loadSourcesOf(scenario);
		inBackground(() -> session.open(scenario, chosen), this::reportProblems);
	}

	/** Asks PCGen to load the scenario's sources when others are loaded. */
	private void loadSourcesOf(Scenario scenario)
	{
		GameMode gameMode = SystemCollections.getGameModeNamed(scenario.gameMode());
		if (gameMode == null)
		{
			return;
		}
		List<Campaign> campaigns = new ArrayList<>();
		for (String key : scenario.campaigns())
		{
			Campaign campaign = Globals.getCampaignKeyedSilently(key);
			if (campaign != null)
			{
				campaigns.add(campaign);
			}
		}
		DataSetFacade loaded = frame.getLoadedDataSetRef().get();
		boolean same = loaded != null && loaded.getGameMode() == gameMode
			&& ListFacades.wrap(loaded.getCampaigns()).containsAll(campaigns);
		if (!same && !campaigns.isEmpty())
		{
			frame.loadSourceSelection(FacadeFactory.createSourceSelection(gameMode, campaigns));
		}
	}

	private void saveScenario(boolean choosing)
	{
		Optional<File> target = choosing ? Optional.empty() : session.file();
		if (target.isEmpty())
		{
			JFileChooser chooser = scenarioChooser();
			if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
			{
				return;
			}
			File chosen = chooser.getSelectedFile();
			if (!chosen.getName().endsWith("." + Scenario.FILE_EXTENSION))
			{
				chosen = new File(chosen.getParentFile(), chosen.getName() + "." + Scenario.FILE_EXTENSION);
			}
			target = Optional.of(chosen);
		}
		session.save(target.get()).ifPresent(reason -> showError(
			LanguageBundle.getFormattedString("in_gametable_unsavedScenario", reason)));
	}

	private boolean mayDiscard()
	{
		return !session.isDirty() || JOptionPane.showConfirmDialog(this,
			LanguageBundle.getString("in_gametable_discardChanges"), LanguageBundle.getString("in_gametable_title"),
			JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
	}

	private static JFileChooser scenarioChooser()
	{
		JFileChooser chooser = new JFileChooser(PCGenSettings.getPcgDir());
		chooser.setFileFilter(new FileNameExtensionFilter(LanguageBundle.getString("in_gametable_scenarioFiles"),
			Scenario.FILE_EXTENSION));
		return chooser;
	}

	/**
	 * Runs slow work, such as building monsters, off the event thread.
	 *
	 * @param work what to run.
	 * @param then what to do with its result, on the event thread.
	 * @param <T>  what the work gives.
	 */
	<T> void inBackground(Supplier<T> work, Consumer<T> then)
	{
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		new SwingWorker<T, Void>()
		{
			@Override
			protected T doInBackground()
			{
				return work.get();
			}

			@Override
			protected void done()
			{
				setCursor(Cursor.getDefaultCursor());
				try
				{
					then.accept(get());
				}
				catch (InterruptedException interrupted)
				{
					Thread.currentThread().interrupt();
				}
				catch (ExecutionException failed)
				{
					Logging.errorPrint("The game table failed", failed.getCause());
					showError(String.valueOf(failed.getCause().getMessage()));
				}
			}
		}.execute();
	}

	/**
	 * @param problems what could not be built, shown to the game master when there is any.
	 */
	void reportProblems(List<String> problems)
	{
		if (!problems.isEmpty())
		{
			JOptionPane.showMessageDialog(this, String.join("\n", problems),
				LanguageBundle.getString("in_gametable_problemsTitle"), JOptionPane.WARNING_MESSAGE);
		}
	}

	void showError(String message)
	{
		JOptionPane.showMessageDialog(this, message, LanguageBundle.getString("in_gametable_title"),
			JOptionPane.ERROR_MESSAGE);
	}
}
