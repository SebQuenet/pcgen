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

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.table.AbstractTableModel;

import pcgen.core.gametable.catalog.Catalog;
import pcgen.core.gametable.catalog.CatalogEntry;
import pcgen.core.gametable.catalog.CatalogField;
import pcgen.core.gametable.catalog.FieldValue;
import pcgen.core.gametable.catalog.Match;
import pcgen.system.LanguageBundle;

/**
 * The bestiary's filtered entries as a table, one column per field the game master chose.
 */
final class CatalogTableModel extends AbstractTableModel
{
	private List<Catalog.Filtered> rows = List.of();
	private List<CatalogField> columns;

	CatalogTableModel(List<CatalogField> columns)
	{
		this.columns = List.copyOf(columns);
	}

	void setRows(List<Catalog.Filtered> filtered)
	{
		rows = List.copyOf(filtered);
		fireTableDataChanged();
	}

	void setColumns(List<CatalogField> chosen)
	{
		columns = List.copyOf(chosen);
		fireTableStructureChanged();
	}

	List<CatalogField> columns()
	{
		return columns;
	}

	CatalogEntry entryAt(int row)
	{
		return rows.get(row).entry();
	}

	boolean isUnknownAt(int row)
	{
		return rows.get(row).match() == Match.UNKNOWN;
	}

	@Override
	public int getRowCount()
	{
		return rows.size();
	}

	@Override
	public int getColumnCount()
	{
		return columns.size();
	}

	@Override
	public String getColumnName(int column)
	{
		return LanguageBundle.getString(columns.get(column).labelKey());
	}

	@Override
	public Class<?> getColumnClass(int column)
	{
		return Cell.class;
	}

	@Override
	public Object getValueAt(int row, int column)
	{
		CatalogEntry entry = rows.get(row).entry();
		CatalogField field = columns.get(column);
		if (field == CatalogField.NAME)
		{
			return new Cell(Optional.of(new FieldValue.Text(entry.name())));
		}
		return new Cell(entry.facts().value(field));
	}

	/**
	 * One cell: a value shown as the game master reads it, sorted by its value.
	 *
	 * @param value the value; empty when not computed.
	 */
	record Cell(Optional<FieldValue> value) implements Comparable<Cell>
	{
		private static final DecimalFormat WHOLE_OR_DECIMAL = new DecimalFormat("0.##");

		@Override
		public String toString()
		{
			return value.map(Cell::shown).orElse("—");
		}

		@Override
		public int compareTo(Cell other)
		{
			if (value.isEmpty() || other.value().isEmpty())
			{
				// Values not computed yet go last
				return Boolean.compare(value.isEmpty(), other.value().isEmpty());
			}
			if (value.get() instanceof FieldValue.Number mine && other.value().get() instanceof FieldValue.Number theirs)
			{
				return Double.compare(mine.value(), theirs.value());
			}
			return String.CASE_INSENSITIVE_ORDER.compare(toString(), other.toString());
		}

		static String shown(FieldValue shown)
		{
			return switch (shown)
			{
				case FieldValue.Number number -> number(number.value());
				case FieldValue.Text text -> text.value();
				case FieldValue.Items items -> items.values().stream().sorted(String.CASE_INSENSITIVE_ORDER)
					.collect(Collectors.joining(", "));
				case FieldValue.Scores scores -> scores.values().entrySet().stream()
					.sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
					.map(score -> score.getKey() + " " + score.getValue()).collect(Collectors.joining(", "));
				case FieldValue.Flag flag -> LanguageBundle.getString(flag.value() ? "in_gametable_yes" : "in_gametable_no");
			};
		}

		static String number(double value)
		{
			for (int denominator : new int[]{2, 3, 4, 6, 8})
			{
				double numerator = value * denominator;
				if (value > 0 && value < 1 && Math.abs(numerator - Math.rint(numerator)) < 1e-9)
				{
					return (long) Math.rint(numerator) + "/" + denominator;
				}
			}
			return WHOLE_OR_DECIMAL.format(value);
		}
	}
}
