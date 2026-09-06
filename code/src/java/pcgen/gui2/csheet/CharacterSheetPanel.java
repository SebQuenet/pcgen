/*
 * Copyright 2008 Connor Petty <cpmeister@users.sourceforge.net>
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
 *
 */
package pcgen.gui2.csheet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.StringWriter;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import pcgen.core.Globals;
import pcgen.facade.core.CharacterFacade;
import pcgen.gui2.PCGenFrame;
import pcgen.gui2.PCGenStatusBar;
import pcgen.gui2.tools.CharacterSelectionListener;
import pcgen.gui3.GuiAssertions;
import pcgen.io.ExportException;
import pcgen.io.ExportHandler;
import pcgen.system.LanguageBundle;
import pcgen.util.Logging;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebView;

/**
 * This class is confusing because it is a model, view, and controller.
 * It also has a load of weird ideas about how handle threads,
 * for example calling the swing worker thread from the javafx thread.
 * We'll need a more developed sense of how to correctly handle this,
 * but it exists for now to force us to deal with the dual-platform tech.
 */
public final class CharacterSheetPanel extends JFXPanel implements CharacterSelectionListener
{
    private static final long DEBOUNCE_DELAY_MS = 250;

    private PreviewVariablesHandler previewVariableHandler = new PreviewVariablesHandler();
    private WebView browser;
    private CharacterFacade character;
    private ExportHandler handler;

    private final Executor executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean refreshPending = new AtomicBoolean(false);
    private final AtomicBoolean sceneInitialised = new AtomicBoolean(false);
    private volatile String lastRenderedContent;

    public CharacterSheetPanel()
    {
        GuiAssertions.assertIsNotJavaFXThread();
    }

    /**
     * Attaching a scene to a JFXPanel that is not yet on screen makes JavaFX read the
     * embedded scene's state before it exists, which throws inside the render lock and
     * leaves rendering wedged. Build the scene once the panel has a peer instead.
     */
    @Override
    public void addNotify()
    {
        super.addNotify();
        if (sceneInitialised.compareAndSet(false, true))
        {
            Platform.runLater(() -> {
                browser = new WebView();
                previewVariableHandler = new PreviewVariablesHandler();
                previewVariableHandler.setCharacter(character);
                browser.setContextMenuEnabled(true);
                browser.getEngine().setJavaScriptEnabled(true);
                browser.getEngine().documentProperty().addListener(previewVariableHandler);
                this.setScene(new Scene(browser));
                refresh();
            });
        }
    }

    public void setCharacterSheet(File sheet)
    {
        handler = (sheet == null) ? null : ExportHandler.createExportHandler(sheet);
    }

    /**
     * Schedules a debounced refresh. Multiple calls within DEBOUNCE_DELAY_MS
     * are coalesced into a single export operation.
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
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    return;
                }
                refreshPending.set(false);
                doRefresh();
            });
        }
    }

    private void doRefresh()
    {
        final PCGenStatusBar statusBar = ((PCGenFrame) Globals.getRootFrame()).getStatusBar();
        SwingUtilities.invokeLater(() ->
                statusBar.startShowingProgress(LanguageBundle.getString("in_loadingCharacterPreview"), true)
        );

        String content;
        if (handler == null || character == null)
        {
            content = "<html><body>No Character Found.</body></html>";
        }
        else
        {
            try
            {
                StringWriter out = new StringWriter();
                BufferedWriter buf = new BufferedWriter(out);
                character.export(handler, buf);
                content = out.toString();
            }
            catch (ExportException e)
            {
                content = "<html><body>Exception when exporting</body></html>";
                Logging.errorPrint("failed to export", e);
            }
        }

        if (content.equals(lastRenderedContent))
        {
            SwingUtilities.invokeLater(statusBar::endShowingProgress);
            return;
        }
        lastRenderedContent = content;

        final String finalContent = content;
        Platform.runLater(() -> {
            try
            {
                if (browser == null)
                {
                    // the panel is not on screen yet; addNotify will refresh once it is
                    lastRenderedContent = null;
                    SwingUtilities.invokeLater(statusBar::endShowingProgress);
                    return;
                }
                browser.getEngine().loadContent(finalContent);
            }
            catch (Throwable e)
            {
                Logging.errorPrint("Exception in GUI update", e);
            }
            SwingUtilities.invokeLater(statusBar::endShowingProgress);
        });
    }

    @Override
    public void setCharacter(CharacterFacade character)
    {
        this.character = character;
        lastRenderedContent = null;
        previewVariableHandler.setCharacter(character);
        refresh();
    }
}
