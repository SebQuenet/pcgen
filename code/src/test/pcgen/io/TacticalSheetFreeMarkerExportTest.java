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
package pcgen.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import pcgen.AbstractCharacterTestCase;
import pcgen.core.PlayerCharacter;
import pcgen.core.tactics.TacticalEntry;
import pcgen.core.tactics.TacticalSection;
import pcgen.core.tactics.TacticalSheet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The loops the output sheets use to print a tactical sheet, run through the
 * real FreeMarker export handler.
 *
 * <p>
 * This is the shape `base.xml.ftl` and `csheet_fantasy_std.htm.ftl` carry: an
 * outer loop over `COUNT[TACTICS]` and an inner one over the entry count of
 * each section.
 */
public class TacticalSheetFreeMarkerExportTest extends AbstractCharacterTestCase
{

	private static final String TEMPLATE = """
		<@loop from=0 to=pcvar('COUNT[TACTICS]-1') ; section , section_has_next>
		[${pcstring('TACTIC.${section}.TITLE')}]
		<@loop from=0 to=pcstring('TACTIC.${section}.COUNT')?number-1 ; entry , entry_has_next>
		${pcstring('TACTIC.${section}.${entry}.TRIGGER')} -> ${pcstring('TACTIC.${section}.${entry}.ACTIONS')}
		</@loop>
		</@loop>
		""";

	private static final String OLDER_ENGINE_TEMPLATE =
			"|IIF(HASVAR:COUNT[TACTICENTRIES])|\n"
			+ "|FOR.0,COUNT[TACTICENTRIES],1,"
			+ "\\TACTIC.FLAT.%.SECTION\\/\\TACTIC.FLAT.%.TRIGGER\\/\\TACTIC.FLAT.%.ACTIONS\\/\\TACTIC.FLAT.%.NOTE\\,CRLF,CRLF,0|\n"
			+ "|ENDIF|\n";

	@TempDir
	private Path templateDirectory;

	private String export(PlayerCharacter character) throws IOException, ExportException
	{
		File template = templateDirectory.resolve("tactical.ftl").toFile();
		Files.writeString(template.toPath(), TEMPLATE);

		StringWriter written = new StringWriter();
		try (BufferedWriter out = new BufferedWriter(written))
		{
			ExportHandler.createExportHandler(template).write(character, out);
		}
		return written.toString();
	}

	@Test
	public void testPrintsEverySectionAndEveryLine() throws IOException, ExportException
	{
		PlayerCharacter character = getCharacter();
		character.setTacticalSheet(new TacticalSheet(List.of(
			new TacticalSection("Opening", List.of(new TacticalEntry("Round 1", "Cast bless", ""))),
			new TacticalSection("Emergency", List.of(new TacticalEntry("HP below 12", "Withdraw", ""),
				new TacticalEntry("Outnumbered", "Fall back", ""))))));

		String exported = export(character);

		assertTrue(exported.contains("[Opening]"), exported);
		assertTrue(exported.contains("Round 1 -> Cast bless"), exported);
		assertTrue(exported.contains("[Emergency]"), exported);
		assertTrue(exported.contains("HP below 12 -> Withdraw"), exported);
		assertTrue(exported.contains("Outnumbered -> Fall back"), exported);
	}

	@Test
	public void testCharacterWithoutASheetPrintsNothing() throws IOException, ExportException
	{
		assertEquals("", export(getCharacter()).strip());
	}

	/**
	 * Every export goes through a copy of the character, built by
	 * {@link PlayerCharacter#cloneForExport()}, so the facet holding the sheet
	 * must be one of the storage beans that copy carries over.
	 */
	@Test
	public void testTheExportCopyOfTheCharacterKeepsTheSheet() throws IOException, ExportException
	{
		PlayerCharacter character = getCharacter();
		character.setTacticalSheet(new TacticalSheet(
			List.of(new TacticalSection("Opening", List.of(new TacticalEntry("Round 1", "Cast bless", ""))))));

		String exported = export(character.cloneForExport());

		assertTrue(exported.contains("[Opening]"), exported);
		assertTrue(exported.contains("Round 1 -> Cast bless"), exported);
	}

	@Test
	public void testOlderEngineLoopsOverTheFlatViewOfTheSheet() throws IOException, ExportException
	{
		PlayerCharacter character = getCharacter();
		character.setTacticalSheet(new TacticalSheet(List.of(
			new TacticalSection("Opening", List.of(new TacticalEntry("Round 1", "Cast bless", ""))),
			new TacticalSection("Emergency", List.of(new TacticalEntry("HP below 12", "Withdraw", ""))))));

		String exported = exportThroughTheOlderEngine(character);

		assertTrue(exported.contains("Opening/Round 1/Cast bless/"), exported);
		assertTrue(exported.contains("Emergency/HP below 12/Withdraw/"), exported);
	}

	@Test
	public void testOlderEngineSkipsTheBlockWhenThereIsNoSheet() throws IOException, ExportException
	{
		assertEquals("", exportThroughTheOlderEngine(getCharacter()).strip());
	}

	/**
	 * Exports through the older |FOR...| engine the compact sheet uses, with the
	 * same construct as `csheet_fantasy_compact.htm`. The body prints the note of
	 * each entry, which is empty here: the loop must not stop on it.
	 *
	 * @param character the character to export.
	 * @return what the template printed.
	 */
	private String exportThroughTheOlderEngine(PlayerCharacter character) throws IOException, ExportException
	{
		File template = templateDirectory.resolve("tactical.htm").toFile();
		Files.writeString(template.toPath(), OLDER_ENGINE_TEMPLATE);

		StringWriter written = new StringWriter();
		try (BufferedWriter out = new BufferedWriter(written))
		{
			ExportHandler.createExportHandler(template).write(character, out);
		}
		return written.toString();
	}
}
