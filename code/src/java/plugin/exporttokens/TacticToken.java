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
package plugin.exporttokens;

import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;

import pcgen.core.PlayerCharacter;
import pcgen.core.tactics.TacticalEntry;
import pcgen.core.tactics.TacticalSection;
import pcgen.core.tactics.TacticalSheet;
import pcgen.io.ExportHandler;
import pcgen.io.exporttoken.Token;

/**
 * Exports the tactical sheet of a character.
 *
 * <p>
 * Sections and entries are addressed by their position, counting from zero:
 * <ul>
 * <li>{@code TACTIC.COUNT} — how many sections the sheet holds, 0 when the
 * character has no tactical sheet.</li>
 * <li>{@code TACTIC.<section>.TITLE} — the heading of a section.</li>
 * <li>{@code TACTIC.<section>.COUNT} — how many entries a section holds.</li>
 * <li>{@code TACTIC.<section>.<entry>.TRIGGER} — the circumstance an entry
 * applies to, and likewise {@code .ACTIONS} and {@code .NOTE}.</li>
 * </ul>
 * The whole sheet is also readable as one flat run of entries, for templates
 * whose loop cannot nest one counter inside another:
 * <ul>
 * <li>{@code TACTIC.FLAT.COUNT} — how many entries the sheet holds in all.</li>
 * <li>{@code TACTIC.FLAT.<entry>.SECTION} — the heading of the section that
 * entry belongs to, and likewise {@code .TRIGGER}, {@code .ACTIONS} and
 * {@code .NOTE}.</li>
 * </ul>
 * An index that points past the end of the sheet exports nothing, so a template
 * that loops one step too far stays silent instead of failing. Text is returned
 * as it was written: the export handler escapes it for the output format, since
 * {@link Token#isEncoded()} is left at its default.
 */
//TACTIC
public class TacticToken extends Token
{
	public static final String TOKENNAME = "TACTIC";

	private static final String COUNT = "COUNT";
	private static final String FLAT = "FLAT";
	private static final String SECTION = "SECTION";
	private static final String TITLE = "TITLE";
	private static final String TRIGGER = "TRIGGER";
	private static final String ACTIONS = "ACTIONS";
	private static final String NOTE = "NOTE";

	@Override
	public String getTokenName()
	{
		return TOKENNAME;
	}

	@Override
	public String getToken(String tokenSource, PlayerCharacter pc, ExportHandler eh)
	{
		StringTokenizer tokens = new StringTokenizer(tokenSource, ".");
		tokens.nextToken();

		List<TacticalSection> sections = sectionsOf(pc);
		if (!tokens.hasMoreTokens())
		{
			return "";
		}

		String firstPart = tokens.nextToken();
		if (COUNT.equals(firstPart))
		{
			return Integer.toString(sections.size());
		}
		if (FLAT.equals(firstPart))
		{
			return flatToken(sections, tokens);
		}

		Optional<TacticalSection> section = at(sections, firstPart);
		if (section.isEmpty() || !tokens.hasMoreTokens())
		{
			return "";
		}

		String secondPart = tokens.nextToken();
		if (COUNT.equals(secondPart))
		{
			return Integer.toString(section.get().entries().size());
		}
		if (TITLE.equals(secondPart))
		{
			return section.get().title();
		}

		Optional<TacticalEntry> entry = at(section.get().entries(), secondPart);
		if (entry.isEmpty() || !tokens.hasMoreTokens())
		{
			return "";
		}

		return fieldOf(entry.get(), tokens.nextToken());
	}

	/**
	 * Reads the part of the token that follows {@code TACTIC.FLAT}, where the
	 * whole sheet is one run of entries rather than sections holding entries.
	 *
	 * @param sections the sections of the sheet.
	 * @param tokens   the rest of the token, after {@code FLAT}.
	 * @return the text to export, empty when the token names nothing that
	 *         exists.
	 */
	private static String flatToken(List<TacticalSection> sections, StringTokenizer tokens)
	{
		if (!tokens.hasMoreTokens())
		{
			return "";
		}

		String part = tokens.nextToken();
		if (COUNT.equals(part))
		{
			return Integer.toString(sections.stream().mapToInt(section -> section.entries().size()).sum());
		}

		int position;
		try
		{
			position = Integer.parseInt(part);
		}
		catch (NumberFormatException nfe)
		{
			return "";
		}
		if (position < 0 || !tokens.hasMoreTokens())
		{
			return "";
		}

		String fieldName = tokens.nextToken();
		for (TacticalSection section : sections)
		{
			if (position < section.entries().size())
			{
				return SECTION.equals(fieldName) ? section.title()
					: fieldOf(section.entries().get(position), fieldName);
			}
			position -= section.entries().size();
		}
		return "";
	}

	private static List<TacticalSection> sectionsOf(PlayerCharacter pc)
	{
		return pc.getDisplay().getTacticalSheet().map(TacticalSheet::sections).orElse(List.of());
	}

	private static String fieldOf(TacticalEntry entry, String fieldName)
	{
		return switch (fieldName)
		{
			case TRIGGER -> entry.trigger();
			case ACTIONS -> entry.actions();
			case NOTE -> entry.note();
			default -> "";
		};
	}

	/**
	 * Reads a position and returns the element it points at.
	 *
	 * @param elements the sections of a sheet or the entries of a section.
	 * @param position the text that should hold a position, counting from zero.
	 * @return the element at that position, empty when the text is not a number
	 *         or points outside the list.
	 */
	private static <T> Optional<T> at(List<T> elements, String position)
	{
		int index;
		try
		{
			index = Integer.parseInt(position);
		}
		catch (NumberFormatException nfe)
		{
			return Optional.empty();
		}
		if (index < 0 || index >= elements.size())
		{
			return Optional.empty();
		}
		return Optional.of(elements.get(index));
	}
}
