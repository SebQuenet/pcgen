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
package pcgen.mcp.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pcgen.core.tactics.TacticalEntry;
import pcgen.core.tactics.TacticalSection;
import pcgen.core.tactics.TacticalSheet;

/**
 * Translates the JSON an AI agent sends through MCP into a {@link TacticalSheet},
 * and a sheet back into the plain maps the agent reads.
 *
 * <p>
 * Anything malformed is rejected with a message naming what is wrong and where,
 * so the agent can fix its call rather than store half a sheet.
 */
final class TacticalSheetPayload
{

	private TacticalSheetPayload()
	{
	}

	/**
	 * Builds a tactical sheet from the {@code sections} argument of a tool call.
	 *
	 * @param sectionsArgument the raw value received for {@code sections}.
	 * @return the sheet described by the argument.
	 * @throws IllegalArgumentException when the argument does not describe a
	 *                                  complete, valid sheet.
	 */
	static TacticalSheet toSheet(Object sectionsArgument)
	{
		if (sectionsArgument == null)
		{
			throw new IllegalArgumentException("sections is required");
		}
		if (!(sectionsArgument instanceof List<?> rawSections))
		{
			throw new IllegalArgumentException("sections must be a list of sections");
		}
		if (rawSections.isEmpty())
		{
			throw new IllegalArgumentException("sections must hold at least one section");
		}

		List<TacticalSection> sections = new ArrayList<>(rawSections.size());
		for (Object rawSection : rawSections)
		{
			sections.add(toSection(rawSection));
		}
		return new TacticalSheet(sections);
	}

	/**
	 * Describes a tactical sheet as the plain maps returned to the agent.
	 *
	 * @param sheet the sheet to describe.
	 * @return one map per section, each holding its title and its entries.
	 */
	static List<Map<String, Object>> toPayload(TacticalSheet sheet)
	{
		List<Map<String, Object>> sections = new ArrayList<>(sheet.sections().size());
		for (TacticalSection section : sheet.sections())
		{
			List<Map<String, Object>> entries = new ArrayList<>(section.entries().size());
			for (TacticalEntry entry : section.entries())
			{
				Map<String, Object> described = new LinkedHashMap<>();
				described.put("trigger", entry.trigger());
				described.put("actions", entry.actions());
				described.put("note", entry.note());
				entries.add(described);
			}
			Map<String, Object> described = new LinkedHashMap<>();
			described.put("title", section.title());
			described.put("entries", entries);
			sections.add(described);
		}
		return sections;
	}

	private static TacticalSection toSection(Object rawSection)
	{
		if (!(rawSection instanceof Map<?, ?> section))
		{
			throw new IllegalArgumentException("Each section must be an object with a title and entries");
		}

		String title = text(section.get("title"));
		if (title == null)
		{
			throw new IllegalArgumentException("Each section needs a non blank title");
		}

		Object rawEntries = section.get("entries");
		if (!(rawEntries instanceof List<?> entryList) || entryList.isEmpty())
		{
			throw new IllegalArgumentException("Section '" + title + "' needs at least one entry");
		}

		List<TacticalEntry> entries = new ArrayList<>(entryList.size());
		for (Object rawEntry : entryList)
		{
			entries.add(toEntry(rawEntry, title));
		}

		try
		{
			return new TacticalSection(title, entries);
		}
		catch (IllegalArgumentException e)
		{
			throw new IllegalArgumentException("Section '" + title + "': " + e.getMessage(), e);
		}
	}

	private static TacticalEntry toEntry(Object rawEntry, String sectionTitle)
	{
		if (!(rawEntry instanceof Map<?, ?> entry))
		{
			throw new IllegalArgumentException(
				"Section '" + sectionTitle + "': each entry must be an object with a trigger and actions");
		}

		String trigger = text(entry.get("trigger"));
		String actions = text(entry.get("actions"));
		if (trigger == null || actions == null)
		{
			throw new IllegalArgumentException(
				"Section '" + sectionTitle + "': each entry needs a non blank trigger and actions");
		}

		String note = text(entry.get("note"));
		return new TacticalEntry(trigger, actions, (note == null) ? "" : note);
	}

	private static String text(Object candidate)
	{
		if (!(candidate instanceof String value) || value.isBlank())
		{
			return null;
		}
		return value;
	}
}
