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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.     See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package plugin.exporttokens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static pcgen.util.TestHelper.evaluateToken;

import java.io.IOException;
import java.util.List;

import pcgen.AbstractCharacterTestCase;
import pcgen.core.PlayerCharacter;
import pcgen.core.tactics.TacticalStep;
import pcgen.core.tactics.TacticalSection;
import pcgen.core.tactics.TacticalSheet;
import pcgen.io.FileAccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Export of the tactical sheet through the TACTIC token.
 */
public class TacticTokenTest extends AbstractCharacterTestCase
{

	@BeforeEach
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		FileAccess.setCurrentOutputFilter("foo.htm");
		getCharacter().setTacticalSheet(new TacticalSheet(List.of(
			new TacticalSection("Opening",
				List.of(new TacticalStep("Round 1", "Cast bless, then advance", "Provokes"))),
			new TacticalSection("Emergency",
				List.of(new TacticalStep("HP below 12", "Drink a potion, withdraw", ""),
					new TacticalStep("Outnumbered", "Fall back to the corridor", ""))))));
	}

	@Test
	public void testSectionCount() throws IOException
	{
		assertEquals("2", evaluateToken("TACTIC.COUNT", getCharacter()));
	}

	@Test
	public void testSectionTitle() throws IOException
	{
		assertEquals("Opening", evaluateToken("TACTIC.0.TITLE", getCharacter()));
		assertEquals("Emergency", evaluateToken("TACTIC.1.TITLE", getCharacter()));
	}

	@Test
	public void testEntryCountPerSection() throws IOException
	{
		assertEquals("1", evaluateToken("TACTIC.0.COUNT", getCharacter()));
		assertEquals("2", evaluateToken("TACTIC.1.COUNT", getCharacter()));
	}

	@Test
	public void testEntryFields() throws IOException
	{
		PlayerCharacter character = getCharacter();
		assertEquals("Round 1", evaluateToken("TACTIC.0.0.TRIGGER", character));
		assertEquals("Cast bless, then advance", evaluateToken("TACTIC.0.0.ACTIONS", character));
		assertEquals("Provokes", evaluateToken("TACTIC.0.0.NOTE", character));
		assertEquals("Outnumbered", evaluateToken("TACTIC.1.1.TRIGGER", character));
	}

	@Test
	public void testMissingNoteExportsNothing() throws IOException
	{
		assertEquals("", evaluateToken("TACTIC.1.0.NOTE", getCharacter()));
	}

	@Test
	public void testOutOfRangeIndexesExportNothing() throws IOException
	{
		PlayerCharacter character = getCharacter();
		assertEquals("", evaluateToken("TACTIC.5.TITLE", character));
		assertEquals("", evaluateToken("TACTIC.0.5.ACTIONS", character));
	}

	@Test
	public void testCharacterWithoutASheetExportsAZeroCount() throws IOException
	{
		getCharacter().clearTacticalSheet();

		assertEquals("0", evaluateToken("TACTIC.COUNT", getCharacter()));
		assertEquals("", evaluateToken("TACTIC.0.TITLE", getCharacter()));
	}

	@Test
	public void testFlatViewCountsEveryEntryOfEverySection() throws IOException
	{
		assertEquals("3", evaluateToken("TACTIC.FLAT.COUNT", getCharacter()));
	}

	@Test
	public void testFlatViewNamesTheSectionOfEachEntry() throws IOException
	{
		PlayerCharacter character = getCharacter();
		assertEquals("Opening", evaluateToken("TACTIC.FLAT.0.SECTION", character));
		assertEquals("Emergency", evaluateToken("TACTIC.FLAT.1.SECTION", character));
		assertEquals("Emergency", evaluateToken("TACTIC.FLAT.2.SECTION", character));
	}

	@Test
	public void testFlatViewReadsEntriesInSheetOrder() throws IOException
	{
		PlayerCharacter character = getCharacter();
		assertEquals("Round 1", evaluateToken("TACTIC.FLAT.0.TRIGGER", character));
		assertEquals("Drink a potion, withdraw", evaluateToken("TACTIC.FLAT.1.ACTIONS", character));
		assertEquals("Outnumbered", evaluateToken("TACTIC.FLAT.2.TRIGGER", character));
	}

	@Test
	public void testFlatViewExportsNothingPastTheLastEntry() throws IOException
	{
		assertEquals("", evaluateToken("TACTIC.FLAT.3.TRIGGER", getCharacter()));
	}

	@Test
	public void testMarkupInTheSheetIsEscaped() throws IOException
	{
		getCharacter().setTacticalSheet(new TacticalSheet(List.of(new TacticalSection("Opening",
			List.of(new TacticalStep("Round 1", "Attack <b>hard</b> & fast", ""))))));

		assertEquals("Attack &lt;b&gt;hard&lt;/b&gt; &amp; fast", evaluateToken("TACTIC.0.0.ACTIONS", getCharacter()));
	}
}
