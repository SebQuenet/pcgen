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

import javax.swing.SwingUtilities;

import pcgen.facade.core.CharacterFacade;
import pcgen.facade.core.ConjurationSheetFacade;
import pcgen.gui2.tabs.conjurations.ConjurationBridge;
import pcgen.gui2.tools.FlippingSplitPane;
import pcgen.system.LanguageBundle;
import pcgen.util.enumeration.Tab;

/**
 * The Conjurations tab: the creatures the character can conjure, and the ones
 * it has conjured into the fight.
 *
 * <p>
 * On the left, the catalog's source text is edited; typing re-reads it, and a
 * banner names the offending line while it does not read. On the right, the
 * rendered page shows the catalog on top and the fight underneath: conjuring,
 * wounding and dismissing a creature writes through to the character.
 */
@SuppressWarnings("serial")
public class ConjurationsInfoTab extends FlippingSplitPane implements CharacterInfoTab
{
	private static final String TEMPLATE = "conjurations" + File.separator + "conjurations.htm.ftl";
	private static final double EDITOR_SHARE = 0.32;

	private final TabTitle tabTitle = new TabTitle(Tab.CONJURATIONS);
	private final SourceEditorPanel editorPanel = new SourceEditorPanel();
	private ConjurationsHandler currentHandler;
	private final RenderedSheetPanel sheetPanel = new RenderedSheetPanel(TEMPLATE,
		character -> new ConjurationBridge(character.getConjurationSheetFacade(), name -> {
			if (currentHandler == null || currentHandler.character != character)
			{
				return false;
			}
			return character.getConjurationSheetFacade().selectionSource(name).map(block -> {
				SwingUtilities.invokeLater(() -> {
					if (currentHandler != null && currentHandler.character == character)
					{
						editorPanel.append(block);
					}
				});
				return true;
		}).orElse(false);
		}, this::isSheetShowing));

	private boolean isSheetShowing()
	{
		return sheetPanel.isShowing();
	}

	public ConjurationsInfoTab()
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
		models.put(ConjurationsHandler.class, new ConjurationsHandler(character));
		return models;
	}

	@Override
	public void restoreModels(ModelMap models)
	{
		models.get(ConjurationsHandler.class).install();
	}

	@Override
	public void storeModels(ModelMap models)
	{
		models.get(ConjurationsHandler.class).uninstall();
	}

	@Override
	public TabTitle getTabTitle()
	{
		return tabTitle;
	}

	/**
	 * Keeps one character's catalog wired to the editor and the rendered page.
	 */
	private final class ConjurationsHandler implements SourceEditorPanel.EditedText
	{
		private final CharacterFacade character;
		private final ConjurationSheetFacade conjurations;

		private ConjurationsHandler(CharacterFacade character)
		{
			this.character = character;
			this.conjurations = character.getConjurationSheetFacade();
		}

		private void install()
		{
			currentHandler = this;
			editorPanel.install(this);
			sheetPanel.setCharacter(character);
		}

		private void uninstall()
		{
			currentHandler = null;
			editorPanel.uninstall();
			sheetPanel.setCharacter(null);
		}

		@Override
		public String stored()
		{
			return conjurations.getCatalogSource();
		}

		@Override
		public List<String> problemsIn(String text)
		{
			return conjurations.errorsIn(text).stream()
				.map(error -> LanguageBundle.getFormattedString("in_conjurations_error", error.line(), error.message()))
				.toList();
		}

		@Override
		public void store(String text, boolean reads)
		{
			conjurations.setCatalogSource(text);
			if (reads)
			{
				sheetPanel.refresh();
			}
		}
	}
}
