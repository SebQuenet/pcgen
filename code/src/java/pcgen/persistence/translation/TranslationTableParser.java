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
package pcgen.persistence.translation;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads a translation table: one tab separated line per translated field, in the columns
 * TYPE, CATEGORY, KEY, FIELD, TEXT. A line that does not read is reported and skipped, so one
 * mistake never hides the rest of the table.
 */
public final class TranslationTableParser
{
	private static final String HEADER = "TYPE\tCATEGORY\tKEY\tFIELD\tTEXT";
	private static final String COMMENT_MARKER = "#";
	private static final String COLUMN_SEPARATOR = "\t";
	private static final int COLUMN_COUNT = 5;

	private TranslationTableParser()
	{
	}

	public static TranslationTable parse(String source)
	{
		List<TranslationEntry> entries = new ArrayList<>();
		List<TranslationProblem> problems = new ArrayList<>();
		List<String> lines = source.lines().toList();
		for (int index = 0; index < lines.size(); index++)
		{
			String line = lines.get(index);
			int lineNumber = index + 1;
			if (line.isBlank() || line.startsWith(COMMENT_MARKER) || line.equals(HEADER))
			{
				continue;
			}
			String[] columns = line.split(COLUMN_SEPARATOR, -1);
			if (columns.length != COLUMN_COUNT)
			{
				problems.add(new TranslationProblem(lineNumber,
					"Expected " + COLUMN_COUNT + " tab separated columns, found " + columns.length));
				continue;
			}
			TranslatedField field = fieldNamed(columns[3]);
			if (field == null)
			{
				problems.add(new TranslationProblem(lineNumber, "Unknown field " + columns[3]));
				continue;
			}
			if (columns[4].isBlank())
			{
				problems.add(new TranslationProblem(lineNumber, "Empty text"));
				continue;
			}
			entries.add(new TranslationEntry(lineNumber, columns[0], columns[1], columns[2], field,
				TableText.unescape(columns[4])));
		}
		return new TranslationTable(entries, problems);
	}

	private static TranslatedField fieldNamed(String name)
	{
		for (TranslatedField field : TranslatedField.values())
		{
			if (field.name().equals(name))
			{
				return field;
			}
		}
		return null;
	}
}
