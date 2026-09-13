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
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.event.ActionListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionListener;

import pcgen.core.tactics.TacticalSection;
import pcgen.facade.core.CharacterFacade;
import pcgen.facade.core.TacticalSheetFacade;
import pcgen.gui2.tabs.tactics.TacticalSectionTableModel;
import pcgen.gui2.tools.FlippingSplitPane;
import pcgen.system.LanguageBundle;
import pcgen.util.enumeration.Tab;

/**
 * The Tactical tab: what the character should do round by round and in which
 * circumstances.
 *
 * <p>
 * Sections are listed on the left, the lines of the selected section are edited
 * on the right. The sheet is usually written by an AI agent through the MCP
 * server; this tab lets the player correct it and add to it.
 */
@SuppressWarnings("serial")
public class TacticalInfoTab extends FlippingSplitPane implements CharacterInfoTab
{

	private final TabTitle tabTitle = new TabTitle(Tab.TACTICAL);
	private final JList<TacticalSection> sectionList = new JList<>();
	private final JTable entryTable = new JTable();
	private final JButton addSectionButton = new JButton();
	private final JButton removeSectionButton = new JButton();
	private final JButton addEntryButton = new JButton();
	private final JButton removeEntryButton = new JButton();
	private final JPanel rightPanel = new JPanel(new CardLayout());

	private static final String EMPTY_CARD = "empty"; //$NON-NLS-1$
	private static final String TABLE_CARD = "table"; //$NON-NLS-1$

	public TacticalInfoTab()
	{
		super(HORIZONTAL_SPLIT);
		initComponents();
	}

	private void initComponents()
	{
		sectionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		sectionList.setCellRenderer(new SectionTitleRenderer());

		addSectionButton.setText(LanguageBundle.getString("in_tactical_add_section")); //$NON-NLS-1$
		removeSectionButton.setText(LanguageBundle.getString("in_tactical_remove")); //$NON-NLS-1$
		addEntryButton.setText(LanguageBundle.getString("in_tactical_add_entry")); //$NON-NLS-1$
		removeEntryButton.setText(LanguageBundle.getString("in_tactical_remove")); //$NON-NLS-1$

		JPanel sectionPanel = new JPanel(new BorderLayout());
		sectionPanel.add(new JScrollPane(sectionList), BorderLayout.CENTER);
		sectionPanel.add(buttonRow(addSectionButton, removeSectionButton), BorderLayout.SOUTH);

		entryTable.setFillsViewportHeight(true);
		entryTable.setRowHeight(entryTable.getRowHeight() * 2);
		JPanel entryPanel = new JPanel(new BorderLayout());
		entryPanel.add(new JScrollPane(entryTable), BorderLayout.CENTER);
		entryPanel.add(buttonRow(addEntryButton, removeEntryButton), BorderLayout.SOUTH);

		JLabel emptyLabel = new JLabel(LanguageBundle.getString("in_tactical_empty"), SwingConstants.CENTER); //$NON-NLS-1$
		rightPanel.add(emptyLabel, EMPTY_CARD);
		rightPanel.add(entryPanel, TABLE_CARD);

		setLeftComponent(sectionPanel);
		setRightComponent(rightPanel);
		setResizeWeight(0.25);
	}

	private static JPanel buttonRow(JButton first, JButton second)
	{
		JPanel row = new JPanel();
		row.add(first);
		row.add(second);
		return row;
	}

	@Override
	public ModelMap createModels(CharacterFacade character)
	{
		ModelMap models = new ModelMap();
		models.put(TacticalSheetHandler.class, new TacticalSheetHandler(character.getTacticalSheetFacade()));
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
	 * Keeps one character's tactical sheet wired to this tab's widgets.
	 */
	private final class TacticalSheetHandler
	{

		private final TacticalSheetFacade sheet;
		private final TacticalSectionTableModel tableModel;
		private final DefaultListModel<TacticalSection> listModel = new DefaultListModel<>();
		private final ListSelectionListener sectionSelected = event -> showSelectedSection();
		private final ActionListener addSectionPressed = event -> addSection();
		private final ActionListener removeSectionPressed = event -> removeSelectedSection();
		private final ActionListener addEntryPressed = event -> addEntry();
		private final ActionListener removeEntryPressed = event -> removeSelectedEntry();

		private TacticalSheetHandler(TacticalSheetFacade sheet)
		{
			this.sheet = sheet;
			this.tableModel = new TacticalSectionTableModel(this::sectionEdited);
		}

		private void install()
		{
			entryTable.setModel(tableModel);
			refreshSectionList(0);

			sectionList.addListSelectionListener(sectionSelected);
			addSectionButton.addActionListener(addSectionPressed);
			removeSectionButton.addActionListener(removeSectionPressed);
			addEntryButton.addActionListener(addEntryPressed);
			removeEntryButton.addActionListener(removeEntryPressed);
		}

		private void uninstall()
		{
			sectionList.removeListSelectionListener(sectionSelected);
			addSectionButton.removeActionListener(addSectionPressed);
			removeSectionButton.removeActionListener(removeSectionPressed);
			addEntryButton.removeActionListener(addEntryPressed);
			removeEntryButton.removeActionListener(removeEntryPressed);
		}

		/**
		 * Rebuilds the list of sections and selects one of them. Called when a
		 * section is added or removed, never while the user edits a line.
		 *
		 * @param positionToSelect which section to leave selected, clamped to
		 *                         what the sheet now holds.
		 */
		private void refreshSectionList(int positionToSelect)
		{
			listModel.clear();
			sheet.getSections().forEach(listModel::addElement);
			sectionList.setModel(listModel);

			if (listModel.isEmpty())
			{
				tableModel.showNoSection();
				showCard(EMPTY_CARD);
				return;
			}

			sectionList.setSelectedIndex(Math.clamp(positionToSelect, 0, listModel.getSize() - 1));
			showCard(TABLE_CARD);
		}

		private void showCard(String card)
		{
			((CardLayout) rightPanel.getLayout()).show(rightPanel, card);
		}

		private void showSelectedSection()
		{
			TacticalSection selected = sectionList.getSelectedValue();
			if (selected == null)
			{
				tableModel.showNoSection();
			}
			else
			{
				tableModel.showSection(selected);
			}
		}

		/**
		 * Stores a section the user has just edited. The list of sections is
		 * updated in place rather than rebuilt, so neither the selection nor the
		 * cell being edited moves under the user.
		 *
		 * @param edited the new state of the selected section.
		 */
		private void sectionEdited(TacticalSection edited)
		{
			int position = sectionList.getSelectedIndex();
			if (position < 0)
			{
				return;
			}
			sheet.replaceSection(position, edited);
			listModel.set(position, edited);
		}

		private void addSection()
		{
			String title = JOptionPane.showInputDialog(TacticalInfoTab.this,
				LanguageBundle.getString("in_tactical_add_section")); //$NON-NLS-1$
			if (title == null || title.isBlank())
			{
				return;
			}
			int positionOfTheNewSection = listModel.getSize();
			sheet.addSection(title);
			refreshSectionList(positionOfTheNewSection);
		}

		private void removeSelectedSection()
		{
			int position = sectionList.getSelectedIndex();
			if (position < 0)
			{
				return;
			}
			sheet.removeSection(position);
			refreshSectionList(position);
		}

		private void addEntry()
		{
			if (sectionList.getSelectedValue() == null)
			{
				return;
			}
			tableModel.addEntry(LanguageBundle.getString("in_tactical_when"), //$NON-NLS-1$
				LanguageBundle.getString("in_tactical_do")); //$NON-NLS-1$
		}

		private void removeSelectedEntry()
		{
			tableModel.removeEntry(entryTable.getSelectedRow());
		}
	}

	/**
	 * Shows a section in the list by its title alone.
	 */
	private static final class SectionTitleRenderer extends DefaultListCellRenderer
	{
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
			boolean cellHasFocus)
		{
			Object shown = (value instanceof TacticalSection section) ? section.title() : value;
			return super.getListCellRendererComponent(list, shown, index, isSelected, cellHasFocus);
		}
	}
}
