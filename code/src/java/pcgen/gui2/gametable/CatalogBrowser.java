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
import java.awt.Component;
import java.awt.Font;
import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

import pcgen.core.gametable.catalog.BestiaryPreferences;
import pcgen.core.gametable.catalog.Catalog;
import pcgen.core.gametable.catalog.CatalogEntry;
import pcgen.core.gametable.catalog.CatalogField;
import pcgen.system.LanguageBundle;

/**
 * The bestiary's entries filtered by the game master's criteria, as a sortable table.
 */
final class CatalogBrowser extends JPanel
{
	private final CatalogTableModel model;
	private final JTable grid;
	private final CriteriaPanel criteria;
	private final JLabel count = new JLabel();
	private final JLabel indexing = new JLabel();
	private Catalog catalog = Catalog.of(List.of());

	CatalogBrowser(BestiaryPreferences preferences)
	{
		super(new BorderLayout(4, 4));
		model = new CatalogTableModel(preferences.columns());
		grid = new JTable(model);
		grid.setAutoCreateRowSorter(false);
		grid.setRowSorter(new TableRowSorter<>(model));
		grid.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		grid.setDefaultRenderer(CatalogTableModel.Cell.class, new DefaultTableCellRenderer()
		{
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column)
			{
				Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				int modelRow = table.convertRowIndexToModel(row);
				boolean isUnknown = model.isUnknownAt(modelRow);
				boolean isBroken = model.entryAt(modelRow).facts().buildFailure().isPresent();
				cell.setFont(cell.getFont().deriveFont(isUnknown || isBroken ? Font.ITALIC : Font.PLAIN));
				setToolTipText(isBroken ? model.entryAt(modelRow).facts().buildFailure().get()
					: isUnknown ? LanguageBundle.getString("in_gametable_notComputed") : null);
				return cell;
			}
		});
		criteria = new CriteriaPanel(preferences, this::refilter);

		JPanel center = new JPanel(new BorderLayout());
		center.add(new JScrollPane(grid), BorderLayout.CENTER);
		JPanel status = new JPanel(new BorderLayout());
		status.add(count, BorderLayout.WEST);
		status.add(indexing, BorderLayout.EAST);
		center.add(status, BorderLayout.SOUTH);
		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, criteria, center);
		split.setResizeWeight(0.3);
		add(split, BorderLayout.CENTER);
	}

	/**
	 * @param shown the entries to filter.
	 */
	void setCatalog(Catalog shown)
	{
		catalog = shown;
		refilter();
	}

	/**
	 * @param columns the fields shown as columns.
	 */
	void setColumns(List<CatalogField> columns)
	{
		model.setColumns(columns);
	}

	/**
	 * @param progress what the index of the NPC files is doing; empty once done.
	 */
	void setIndexing(String progress)
	{
		indexing.setText(progress);
	}

	/**
	 * @param action run when the selected entries change.
	 */
	void onSelection(Runnable action)
	{
		grid.getSelectionModel().addListSelectionListener(event -> {
			if (!event.getValueIsAdjusting())
			{
				action.run();
			}
		});
	}

	/**
	 * @return the entries selected, in the order shown.
	 */
	List<CatalogEntry> selected()
	{
		return Arrays.stream(grid.getSelectedRows()).map(grid::convertRowIndexToModel).mapToObj(model::entryAt).toList();
	}

	private void refilter()
	{
		List<CatalogEntry> before = selected();
		List<Catalog.Filtered> kept = catalog.filter(criteria.criterion());
		model.setRows(kept);
		count.setText(LanguageBundle.getFormattedString("in_gametable_entriesShown", kept.size(), catalog.entries().size()));
		for (int row = 0; row < model.getRowCount(); row++)
		{
			if (before.contains(model.entryAt(row)))
			{
				int shown = grid.convertRowIndexToView(row);
				grid.getSelectionModel().addSelectionInterval(shown, shown);
			}
		}
	}
}
