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

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import pcgen.core.tactics.TacticalParseError;
import pcgen.facade.core.CharacterFacade;
import pcgen.facade.core.TacticalSheetFacade;
import pcgen.gui2.tabs.tactics.TacticalSheetPanel;
import pcgen.gui2.tools.FlippingSplitPane;
import pcgen.system.LanguageBundle;
import pcgen.util.enumeration.Tab;

/**
 * The Tactical tab: what the character should do round by round and in which
 * circumstances.
 *
 * <p>
 * Two different acts share the tab. On the left, the plan's source text is
 * edited; typing re-reads it, and a banner names the offending line while it
 * does not read. On the right, the rendered sheet is played: ticking a resource
 * or taking damage writes through to the character.
 *
 * <p>
 * The plan is usually written by an AI agent through the MCP server, in the
 * same syntax the editor shows, so what the agent wrote can be read and
 * corrected here.
 */
@SuppressWarnings("serial")
public class TacticalInfoTab extends FlippingSplitPane implements CharacterInfoTab
{
	private static final int TYPING_PAUSE_MS = 300;

	private final TabTitle tabTitle = new TabTitle(Tab.TACTICAL);
	private final JTextArea sourceEditor = new JTextArea();
	private final JLabel banner = new JLabel();
	private final TacticalSheetPanel sheetPanel = new TacticalSheetPanel();

	public TacticalInfoTab()
	{
		super(HORIZONTAL_SPLIT);
		initComponents();
	}

	private void initComponents()
	{
		sourceEditor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, sourceEditor.getFont().getSize()));
		sourceEditor.setTabSize(2);
		sourceEditor.setLineWrap(false);

		banner.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 6, 3, 6));

		JPanel editorPanel = new JPanel(new BorderLayout());
		editorPanel.add(new JScrollPane(sourceEditor), BorderLayout.CENTER);
		editorPanel.add(banner, BorderLayout.SOUTH);

		setLeftComponent(editorPanel);
		setRightComponent(sheetPanel);
		setResizeWeight(0.38);
	}

	@Override
	public ModelMap createModels(CharacterFacade character)
	{
		ModelMap models = new ModelMap();
		models.put(TacticalSheetHandler.class, new TacticalSheetHandler(character));
		return models;
	}

	@Override
	public void restoreModels(ModelMap models)
	{
		models.get(TacticalSheetHandler.class).install();
	}

	@Override
	public void storeModels(ModelMap models)
	{
		models.get(TacticalSheetHandler.class).uninstall();
	}

	@Override
	public TabTitle getTabTitle()
	{
		return tabTitle;
	}

	/**
	 * Keeps one character's plan wired to the editor and the rendered sheet.
	 *
	 * <p>
	 * Every keystroke restarts a short timer rather than storing at once, so a
	 * word being typed does not each time rewrite the character and re-render
	 * the page.
	 */
	private final class TacticalSheetHandler implements DocumentListener
	{
		private final CharacterFacade character;
		private final TacticalSheetFacade sheet;
		private final Timer typingPause;
		private boolean loading;

		private TacticalSheetHandler(CharacterFacade character)
		{
			this.character = character;
			this.sheet = character.getTacticalSheetFacade();
			typingPause = new Timer(TYPING_PAUSE_MS, event -> store());
			typingPause.setRepeats(false);
		}

		private void install()
		{
			loading = true;
			sourceEditor.setText(sheet.getPlanSource());
			sourceEditor.setCaretPosition(0);
			loading = false;
			showErrors(sheet.errorsIn(sourceEditor.getText()));
			sourceEditor.getDocument().addDocumentListener(this);
			sheetPanel.setCharacter(character);
		}

		private void uninstall()
		{
			typingPause.stop();
			sourceEditor.getDocument().removeDocumentListener(this);
			store();
			sheetPanel.setCharacter(null);
		}

		private void store()
		{
			if (loading)
			{
				return;
			}
			String source = sourceEditor.getText();
			List<TacticalParseError> errors = sheet.errorsIn(source);
			showErrors(errors);
			sheet.setPlanSource(source);
			if (errors.isEmpty())
			{
				sheetPanel.refresh();
			}
		}

		/**
		 * @param errors what is wrong with the text as it stands, empty when it
		 *               reads
		 */
		private void showErrors(List<TacticalParseError> errors)
		{
			if (errors.isEmpty())
			{
				banner.setText(" ");
				return;
			}
			TacticalParseError first = errors.getFirst();
			String more = (errors.size() > 1) ? " (+" + (errors.size() - 1) + ')' : "";
			banner.setText(LanguageBundle.getFormattedString("in_tactical_error", first.line(), first.message())
				+ more);
		}

		@Override
		public void insertUpdate(DocumentEvent event)
		{
			typingPause.restart();
		}

		@Override
		public void removeUpdate(DocumentEvent event)
		{
			typingPause.restart();
		}

		@Override
		public void changedUpdate(DocumentEvent event)
		{
			typingPause.restart();
		}
	}
}
