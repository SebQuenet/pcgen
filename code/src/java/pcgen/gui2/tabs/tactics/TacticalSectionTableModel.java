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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.table.AbstractTableModel;

import pcgen.core.tactics.TacticalEntry;
import pcgen.core.tactics.TacticalSection;
import pcgen.system.LanguageBundle;

/**
 * The lines of one tactical section, as a table of When / Do / Note.
 *
 * <p>
 * Every edit publishes the section as a whole to the listener the tab passed in,
 * which stores it on the character. An edit the model cannot represent — blanking
 * a trigger or the actions, or removing the last line of a section, since a
 * section is never empty — publishes nothing and leaves the table as it was.
 */
@SuppressWarnings("serial")
public class TacticalSectionTableModel extends AbstractTableModel
{

	private static final int TRIGGER_COLUMN = 0;
	private static final int ACTIONS_COLUMN = 1;
	private static final int NOTE_COLUMN = 2;

	private final Consumer<TacticalSection> onSectionEdited;
	private String title = "";
	private final List<TacticalEntry> entries = new ArrayList<>();

	/**
	 * @param onSectionEdited told the new state of the section after each edit
	 *                        the model accepts.
	 */
	public TacticalSectionTableModel(Consumer<TacticalSection> onSectionEdited)
	{
		this.onSectionEdited = onSectionEdited;
	}

	/**
	 * Shows the lines of a section.
	 *
	 * @param section the section the user selected.
	 */
	public void showSection(TacticalSection section)
	{
		title = section.title();
		entries.clear();
		entries.addAll(section.entries());
		fireTableDataChanged();
	}

	/**
	 * Empties the table, for when no section is selected.
	 */
	public void showNoSection()
	{
		title = "";
		entries.clear();
		fireTableDataChanged();
	}

	/**
	 * Adds a line at the end of the section.
	 *
	 * @param trigger the circumstance the line applies to.
	 * @param actions what the character does.
	 */
	public void addEntry(String trigger, String actions)
	{
		entries.add(new TacticalEntry(trigger, actions, ""));
		fireTableRowsInserted(entries.size() - 1, entries.size() - 1);
		publish();
	}

	/**
	 * Removes a line, unless it is the only one left: a section is never empty,
	 * so the tab removes the section itself instead.
	 *
	 * @param row the line to remove.
	 */
	public void removeEntry(int row)
	{
		if (row < 0 || row >= entries.size() || entries.size() == 1)
		{
			return;
		}
		entries.remove(row);
		fireTableRowsDeleted(row, row);
		publish();
	}

	@Override
	public int getRowCount()
	{
		return entries.size();
	}

	@Override
	public int getColumnCount()
	{
		return 3;
	}

	@Override
	public String getColumnName(int column)
	{
		return switch (column)
		{
			case TRIGGER_COLUMN -> LanguageBundle.getString("in_tactical_when");
			case ACTIONS_COLUMN -> LanguageBundle.getString("in_tactical_do");
			default -> LanguageBundle.getString("in_tactical_note");
		};
	}

	@Override
	public Class<?> getColumnClass(int column)
	{
		return String.class;
	}

	@Override
	public boolean isCellEditable(int row, int column)
	{
		return true;
	}

	@Override
	public Object getValueAt(int row, int column)
	{
		TacticalEntry entry = entries.get(row);
		return switch (column)
		{
			case TRIGGER_COLUMN -> entry.trigger();
			case ACTIONS_COLUMN -> entry.actions();
			default -> entry.note();
		};
	}

	@Override
	public void setValueAt(Object value, int row, int column)
	{
		String text = (value == null) ? "" : value.toString();
		if (column != NOTE_COLUMN && text.isBlank())
		{
			return;
		}

		TacticalEntry current = entries.get(row);
		TacticalEntry edited = switch (column)
		{
			case TRIGGER_COLUMN -> new TacticalEntry(text, current.actions(), current.note());
			case ACTIONS_COLUMN -> new TacticalEntry(current.trigger(), text, current.note());
			default -> new TacticalEntry(current.trigger(), current.actions(), text);
		};

		entries.set(row, edited);
		fireTableCellUpdated(row, column);
		publish();
	}

	private void publish()
	{
		onSectionEdited.accept(new TacticalSection(title, entries));
	}
}
