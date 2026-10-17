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

/**
 * How a text sits in one column of a translation table: line breaks, tabs and backslashes are
 * written {@code \n}, {@code \t} and {@code \\}, so that one line stays one entry.
 */
final class TableText
{
	private TableText()
	{
	}

	static String escape(String text)
	{
		return text.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
	}

	static String unescape(String column)
	{
		StringBuilder text = new StringBuilder(column.length());
		for (int index = 0; index < column.length(); index++)
		{
			char current = column.charAt(index);
			if (current == '\\' && index + 1 < column.length())
			{
				char next = column.charAt(++index);
				text.append(switch (next)
				{
					case 'n' -> '\n';
					case 't' -> '\t';
					default -> next;
				});
			}
			else
			{
				text.append(current);
			}
		}
		return text.toString();
	}
}
