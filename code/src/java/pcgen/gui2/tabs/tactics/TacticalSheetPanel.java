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
package pcgen.gui2.tabs.tactics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import pcgen.facade.core.CharacterFacade;
import pcgen.facade.core.TacticalSheetFacade;
import pcgen.io.ExportException;
import pcgen.io.ExportHandler;
import pcgen.system.ConfigurationSettings;
import pcgen.util.Logging;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

/**
 * Shows the rendered tactical sheet, and lets it write back.
 *
 * <p>
 * The page is the same HTML the export writes to a file: one renderer, two
 * consumers. Inside PCGen a bridge is planted on the page's window, so ticking
 * a resource reaches the character; the exported file finds no bridge.
 */
public final class TacticalSheetPanel extends JFXPanel
{
	private static final long DEBOUNCE_DELAY_MS = 200;
	private static final String TEMPLATE = "tactics" + File.separator + "tactical.htm.ftl";

	private final Executor executor = Executors.newSingleThreadExecutor();
	private final AtomicBoolean refreshPending = new AtomicBoolean(false);
	private final AtomicBoolean sceneInitialised = new AtomicBoolean(false);
	private WebView browser;
	private CharacterFacade character;
	private TacticalBridge bridge;
	private volatile String lastRenderedContent;

	/**
	 * Attaching a scene to a panel that is not yet on screen wedges JavaFX
	 * rendering, so the scene is built once the panel has a peer.
	 */
	@Override
	public void addNotify()
	{
		super.addNotify();
		if (sceneInitialised.compareAndSet(false, true))
		{
			Platform.runLater(() -> {
				browser = new WebView();
				browser.setContextMenuEnabled(true);
				browser.getEngine().setJavaScriptEnabled(true);
				browser.getEngine().getLoadWorker().stateProperty()
					.addListener((observed, was, is) -> plantBridge(is));
				setScene(new Scene(browser));
				refresh();
			});
		}
	}

	/**
	 * @param shown the character whose sheet to show, null for none.
	 */
	public void setCharacter(CharacterFacade shown)
	{
		character = shown;
		bridge = (shown == null) ? null : new TacticalBridge(shown.getTacticalSheetFacade());
		lastRenderedContent = null;
		refresh();
	}

	/**
	 * Renders again, coalescing the calls that arrive while someone is typing.
	 */
	public void refresh()
	{
		if (refreshPending.compareAndSet(false, true))
		{
			executor.execute(() -> {
				try
				{
					Thread.sleep(DEBOUNCE_DELAY_MS);
				}
				catch (InterruptedException interrupted)
				{
					Thread.currentThread().interrupt();
					return;
				}
				refreshPending.set(false);
				render();
			});
		}
	}

	private void render()
	{
		String content = (character == null) ? "<html><body></body></html>" : exported();
		if (content.equals(lastRenderedContent))
		{
			return;
		}
		lastRenderedContent = content;
		Platform.runLater(() -> {
			if (browser != null)
			{
				browser.getEngine().loadContent(content);
			}
		});
	}

	private String exported()
	{
		File template = Path.of(ConfigurationSettings.getOutputSheetsDir(), TEMPLATE).toFile();
		if (!template.isFile())
		{
			Logging.errorPrint("No tactical sheet template at " + template);
			return "<html><body></body></html>";
		}
		StringWriter written = new StringWriter();
		try (BufferedWriter out = new BufferedWriter(written))
		{
			character.export(ExportHandler.createExportHandler(template), out);
		}
		catch (ExportException | java.io.IOException e)
		{
			Logging.errorPrint("Failed to render the tactical sheet", e);
			return "<html><body></body></html>";
		}
		return written.toString();
	}

	/**
	 * The page reads the bridge lazily, so planting it once the document is
	 * loaded is soon enough.
	 *
	 * @param state where the page load has got to.
	 */
	private void plantBridge(Worker.State state)
	{
		if (state != Worker.State.SUCCEEDED || bridge == null || browser == null)
		{
			return;
		}
		try
		{
			Object window = browser.getEngine().executeScript("window");
			if (window instanceof JSObject page)
			{
				page.setMember("pcgen", bridge);
			}
		}
		catch (RuntimeException e)
		{
			Logging.errorPrint("Could not give the tactical sheet its bridge to PCGen", e);
		}
	}

	/**
	 * @return the facade the panel writes through, null when no character is
	 *         shown.
	 */
	public TacticalSheetFacade getSheet()
	{
		return (character == null) ? null : character.getTacticalSheetFacade();
	}
}
