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

import java.io.File;
import java.util.List;

import pcgen.facade.core.CharacterFacade;
import pcgen.facade.core.TacticalSheetFacade;
import pcgen.gui2.tabs.tactics.TacticalBridge;
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
	private static final String TEMPLATE = "tactics" + File.separator + "tactical.htm.ftl";
	private static final double EDITOR_SHARE = 0.38;

	private final TabTitle tabTitle = new TabTitle(Tab.TACTICAL);
	private final SourceEditorPanel editorPanel = new SourceEditorPanel();
	private final RenderedSheetPanel sheetPanel =
			new RenderedSheetPanel(TEMPLATE, character -> new TacticalBridge(character.getTacticalSheetFacade()));

	public TacticalInfoTab()
	{
		super(HORIZONTAL_SPLIT);
		setLeftComponent(editorPanel);
		setRightComponent(sheetPanel);
		setResizeWeight(EDITOR_SHARE);
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
	 */
	private final class TacticalSheetHandler implements SourceEditorPanel.EditedText
	{
		private final CharacterFacade character;
		private final TacticalSheetFacade sheet;

		private TacticalSheetHandler(CharacterFacade character)
		{
			this.character = character;
			this.sheet = character.getTacticalSheetFacade();
		}

		private void install()
		{
			editorPanel.install(this);
			sheetPanel.setCharacter(character);
		}

		private void uninstall()
		{
			editorPanel.uninstall();
			sheetPanel.setCharacter(null);
		}

		@Override
		public String stored()
		{
			return sheet.getPlanSource();
		}

		@Override
		public List<String> problemsIn(String text)
		{
			return sheet.errorsIn(text).stream()
				.map(error -> LanguageBundle.getFormattedString("in_tactical_error", error.line(), error.message()))
				.toList();
		}

		@Override
		public void store(String text, boolean reads)
		{
			sheet.setPlanSource(text);
			if (reads)
			{
				sheetPanel.refresh();
			}
		}
	}
}
