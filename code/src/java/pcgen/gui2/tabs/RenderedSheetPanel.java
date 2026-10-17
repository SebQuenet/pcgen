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
package pcgen.gui2.tabs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import pcgen.facade.core.CharacterFacade;
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
 * Shows a character rendered through an output sheet, and lets the page write
 * back.
 *
 * <p>
 * The page is the same HTML the export writes to a file: one renderer, two
 * consumers. Inside PCGen a bridge is planted on the page's window, so what the
 * page does reaches the character; the exported file finds no bridge.
 */
public final class RenderedSheetPanel extends JFXPanel
{
	private static final long DEBOUNCE_DELAY_MS = 200;
	private static final String EMPTY_PAGE = "<html><body></body></html>";

	private final String template;
	private final Function<CharacterFacade, Object> bridgeFor;
	private final Executor executor = Executors.newSingleThreadExecutor();
	private final AtomicBoolean refreshPending = new AtomicBoolean(false);
	private final AtomicBoolean sceneInitialised = new AtomicBoolean(false);
	private WebView browser;
	private CharacterFacade character;
	/** Held here because the page holds it only weakly: a bridge nobody keeps is collected. */
	private Object bridge;
	private volatile String lastRenderedContent;

	/**
	 * @param template  the output sheet to render, relative to the output
	 *                  sheets directory
	 * @param bridgeFor what the page of a character may call back into; its
	 *                  public methods are what the page's script sees
	 */
	public RenderedSheetPanel(String template, Function<CharacterFacade, Object> bridgeFor)
	{
		this.template = template;
		this.bridgeFor = bridgeFor;
	}

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
	 * @param shown the character whose page to show, null for none.
	 */
	public void setCharacter(CharacterFacade shown)
	{
		character = shown;
		bridge = (shown == null) ? null : bridgeFor.apply(shown);
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
		String content = (character == null) ? EMPTY_PAGE : exported();
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
		File sheet = Path.of(ConfigurationSettings.getOutputSheetsDir(), template).toFile();
		if (!sheet.isFile())
		{
			Logging.errorPrint("No output sheet at " + sheet);
			return EMPTY_PAGE;
		}
		StringWriter written = new StringWriter();
		try (BufferedWriter out = new BufferedWriter(written))
		{
			character.export(ExportHandler.createExportHandler(sheet), out);
		}
		catch (ExportException | IOException e)
		{
			Logging.errorPrint("Failed to render " + template, e);
			return EMPTY_PAGE;
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
			Logging.errorPrint("Could not give " + template + " its bridge to PCGen", e);
		}
	}
}
