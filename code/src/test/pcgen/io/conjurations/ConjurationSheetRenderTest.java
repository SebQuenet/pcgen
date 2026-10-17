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
package pcgen.io.conjurations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.OptionalInt;

import pcgen.AbstractCharacterTestCase;
import pcgen.core.PlayerCharacter;
import pcgen.core.conjurations.ConjurationSession;
import pcgen.core.conjurations.ConjuredCreature;
import pcgen.io.ExportException;
import pcgen.io.ExportHandler;
import pcgen.system.LanguageBundle;

import org.junit.jupiter.api.Test;

/**
 * The conjurations template, run through the real export handler against a
 * real character.
 */
public class ConjurationSheetRenderTest extends AbstractCharacterTestCase
{

	private static final File TEMPLATE = new File("outputsheets/conjurations/conjurations.htm.ftl");

	private static final String A_CATALOG = """
		## Summon Monster IV
		summon: Hound Archon
		  role: Tank, Buff
		  hp: 51
		  ac: 19 / 10 / 19
		  attack: +11/+6 sword\\n+5 bite
		  special: Beast shape II

		## Summon Monster V
		summon: Bralani Azata
		  role: Ranged, healer
		  hp: 80
		""";

	private static String render(PlayerCharacter character) throws IOException, ExportException
	{
		StringWriter written = new StringWriter();
		try (BufferedWriter out = new BufferedWriter(written))
		{
			ExportHandler.createExportHandler(TEMPLATE).write(character, out);
		}
		return written.toString();
	}

	private static void assertShows(String expected, String html)
	{
		assertTrue(html.contains(expected) || html.contains(expected.replace("'", "&#39;")),
			() -> "Expected the page to show '" + expected + "':\n" + html);
	}

	@Test
	public void saysSoWhenTheCharacterHasNoCatalog() throws IOException, ExportException
	{
		assertShows(LanguageBundle.getString("in_conjurations_empty"), render(getCharacter()));
	}

	@Test
	public void titlesATableWithEveryGroup() throws IOException, ExportException
	{
		getCharacter().setConjurationCatalog(A_CATALOG);

		String html = render(getCharacter());

		assertShows("Summon Monster IV", html);
		assertShows("Summon Monster V", html);
	}

	@Test
	public void showsTheStatLineOfEveryCreature() throws IOException, ExportException
	{
		getCharacter().setConjurationCatalog(A_CATALOG);

		String html = render(getCharacter());

		assertShows("19 / 10 / 19", html);
		assertShows("Beast shape II", html);
	}

	@Test
	public void breaksAFieldOnItsLineBreaks() throws IOException, ExportException
	{
		getCharacter().setConjurationCatalog(A_CATALOG);

		assertShows("+11/+6 sword<br>+5 bite", render(getCharacter()));
	}

	@Test
	public void offersAFilterForEveryRole() throws IOException, ExportException
	{
		getCharacter().setConjurationCatalog(A_CATALOG);

		assertShows("data-roles=\"Tank,Buff,Ranged,healer\"", render(getCharacter()));
	}

	@Test
	public void holdsACardReadyForEveryCreatureTheCatalogCanConjure() throws IOException, ExportException
	{
		getCharacter().setConjurationCatalog(A_CATALOG);

		String html = render(getCharacter());

		assertShows("<template data-creature=\"Hound Archon\"", html);
		assertShows("<template data-creature=\"Bralani Azata\"", html);
	}

	@Test
	public void showsTheHitPointsLeftToEachCreatureInPlay() throws IOException, ExportException
	{
		PlayerCharacter character = getCharacter();
		character.setConjurationCatalog(A_CATALOG);
		character.setConjurationSession(new ConjurationSession(12, List.of(
			new ConjuredCreature("Hound Archon", 1, 14, OptionalInt.of(7)),
			new ConjuredCreature("Hound Archon", 2, 0, OptionalInt.of(12)))));

		String html = render(character);

		assertShows("<span class=\"hp-current\">37</span> / 51", html);
		assertShows("<span class=\"hp-current\">51</span> / 51", html);
	}

	@Test
	public void showsTheRoundsLeftToACreatureInPlay() throws IOException, ExportException
	{
		PlayerCharacter character = getCharacter();
		character.setConjurationCatalog(A_CATALOG);
		character.setConjurationSession(new ConjurationSession(12,
			List.of(new ConjuredCreature("Hound Archon", 1, 0, OptionalInt.of(7)))));

		assertShows("<span class=\"rounds-left\">7</span>", render(character));
	}

	@Test
	public void warnsAboutACreatureInPlayTheCatalogNoLongerHolds() throws IOException, ExportException
	{
		PlayerCharacter character = getCharacter();
		character.setConjurationCatalog(A_CATALOG);
		character.setConjurationSession(new ConjurationSession(0,
			List.of(new ConjuredCreature("Lantern Archon", 1, 0, OptionalInt.empty()))));

		assertShows(LanguageBundle.getFormattedString("in_conjurations_missing", "Lantern Archon"), render(character));
	}

	@Test
	public void escapesMarkupWrittenIntoTheCatalog() throws IOException, ExportException
	{
		getCharacter().setConjurationCatalog("""
			## Summon Monster I
			summon: Dire Rat
			  special: <script>window.pcgen.dismissAll()</script>
			""");

		String html = render(getCharacter());

		assertFalse(html.contains("<script>window.pcgen"), "Authored markup reached the page unescaped");
		assertShows("&lt;script&gt;", html);
	}

	@Test
	public void showsTheOffendingLineWhenTheCatalogDoesNotRead() throws IOException, ExportException
	{
		getCharacter().setConjurationCatalog("## Summon Monster I\nmonster: Dire Rat\n");

		String html = render(getCharacter());

		assertShows(LanguageBundle.getString("in_conjurations_does_not_read"), html);
		assertShows(LanguageBundle.getFormattedString("in_conjurations_error", 2, "").strip(), html);
	}

	/**
	 * Every export goes through a copy of the character built by
	 * {@link PlayerCharacter#cloneForExport()}, which carries only the facets
	 * declared as storage beans.
	 */
	@Test
	public void theExportCopyOfTheCharacterKeepsTheCatalogAndTheFight() throws IOException, ExportException
	{
		PlayerCharacter character = getCharacter();
		character.setConjurationCatalog(A_CATALOG);
		character.setConjurationSession(new ConjurationSession(0,
			List.of(new ConjuredCreature("Hound Archon", 1, 14, OptionalInt.empty()))));

		String html = render(character.cloneForExport());

		assertShows("Summon Monster IV", html);
		assertShows("<span class=\"hp-current\">37</span> / 51", html);
	}
}
