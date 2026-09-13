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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import pcgen.core.tactics.TacticalEntry;
import pcgen.core.tactics.TacticalSection;
import pcgen.core.tactics.TacticalSheet;

import org.junit.jupiter.api.Test;

/**
 * Translation of the JSON an AI agent sends through MCP into the tactical sheet
 * model, and back.
 */
class TacticalSheetPayloadTest
{

	private static Map<String, Object> anEntryPayload()
	{
		return Map.of("trigger", "Round 1", "actions", "Cast bless, then advance");
	}

	@Test
	void buildsASheetFromWellFormedSections()
	{
		Object sections = List.of(Map.of("title", "Opening", "entries", List.of(anEntryPayload())));

		TacticalSheet sheet = TacticalSheetPayload.toSheet(sections);

		assertEquals(new TacticalSheet(List.of(
			new TacticalSection("Opening", List.of(new TacticalEntry("Round 1", "Cast bless, then advance", ""))))),
			sheet);
	}

	@Test
	void keepsTheNoteWhenOneIsGiven()
	{
		Object sections = List.of(Map.of("title", "Emergency",
			"entries", List.of(Map.of("trigger", "HP below 12", "actions", "Withdraw", "note", "Provokes"))));

		TacticalSheet sheet = TacticalSheetPayload.toSheet(sections);

		assertEquals("Provokes", sheet.sections().get(0).entries().get(0).note());
	}

	@Test
	void keepsSectionsAndEntriesInTheOrderReceived()
	{
		Object sections = List.of(
			Map.of("title", "Opening", "entries", List.of(anEntryPayload())),
			Map.of("title", "Emergency", "entries",
				List.of(Map.of("trigger", "HP below 12", "actions", "Withdraw"),
					Map.of("trigger", "Outnumbered", "actions", "Fall back"))));

		TacticalSheet sheet = TacticalSheetPayload.toSheet(sections);

		assertEquals(List.of("Opening", "Emergency"), sheet.sections().stream().map(TacticalSection::title).toList());
		assertEquals(List.of("HP below 12", "Outnumbered"),
			sheet.sections().get(1).entries().stream().map(TacticalEntry::trigger).toList());
	}

	@Test
	void rejectsMissingSections()
	{
		IllegalArgumentException thrown =
				assertThrows(IllegalArgumentException.class, () -> TacticalSheetPayload.toSheet(null));

		assertTrue(thrown.getMessage().contains("sections"), thrown.getMessage());
	}

	@Test
	void rejectsSectionsThatAreNotAList()
	{
		assertThrows(IllegalArgumentException.class, () -> TacticalSheetPayload.toSheet("Opening"));
	}

	@Test
	void rejectsASectionWithoutATitle()
	{
		Object sections = List.of(Map.of("entries", List.of(anEntryPayload())));

		assertThrows(IllegalArgumentException.class, () -> TacticalSheetPayload.toSheet(sections));
	}

	@Test
	void rejectsAnEntryWithoutActions()
	{
		Object sections = List.of(Map.of("title", "Opening", "entries", List.of(Map.of("trigger", "Round 1"))));

		assertThrows(IllegalArgumentException.class, () -> TacticalSheetPayload.toSheet(sections));
	}

	@Test
	void namesTheOffendingSectionInTheError()
	{
		Object sections = List.of(Map.of("title", "Opening", "entries", List.of(anEntryPayload())),
			Map.of("title", "Emergency", "entries", List.of(Map.of("trigger", "HP below 12"))));

		IllegalArgumentException thrown =
				assertThrows(IllegalArgumentException.class, () -> TacticalSheetPayload.toSheet(sections));

		assertTrue(thrown.getMessage().contains("Emergency"), thrown.getMessage());
	}

	@Test
	void describesASheetAsPlainMapsForTheAgent()
	{
		TacticalSheet sheet = new TacticalSheet(List.of(
			new TacticalSection("Opening", List.of(new TacticalEntry("Round 1", "Charge", "Provokes")))));

		assertEquals(List.of(Map.of("title", "Opening",
			"entries", List.of(Map.of("trigger", "Round 1", "actions", "Charge", "note", "Provokes")))),
			TacticalSheetPayload.toPayload(sheet));
	}
}
