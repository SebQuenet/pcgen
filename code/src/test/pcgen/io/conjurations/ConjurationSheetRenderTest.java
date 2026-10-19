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
import pcgen.core.conjurations.AppliedBonus;
import pcgen.core.conjurations.BonusSourceKey;
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
		summon: Archon (Hound)

		## Summon Monster V
		summon: Azata (Bralani)
		  template: Augment Summoning
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
	public void keepsAuthoredRolesAndStatsVisibleInAnOlderCatalog() throws IOException, ExportException
	{
		getCharacter().setConjurationCatalog("""
			## VI
			summon: Lillend (azata)
			  role: lanceur, soutien
			  hp: 87
			  special: Sorts de barde
			""");

		String html = render(getCharacter());

		assertShows("lanceur", html);
		assertShows("Sorts de barde", html);
		assertShows("data-authored=\"true\"", html);
		assertShows("data-max=\"87\"", html);
	}

	@Test
	public void holdsACardReadyForEveryCreatureTheCatalogCanConjure() throws IOException, ExportException
	{
		getCharacter().setConjurationCatalog(A_CATALOG);

		String html = render(getCharacter());

		assertShows("<template data-creature=\"Archon (Hound)\"", html);
		assertShows("<template data-creature=\"Azata (Bralani) (Augment Summoning)\"", html);
	}

	@Test
	public void showsTheRoundsLeftToACreatureInPlay() throws IOException, ExportException
	{
		PlayerCharacter character = getCharacter();
		character.setConjurationCatalog(A_CATALOG);
		character.setConjurationSession(new ConjurationSession(12,
			List.of(new ConjuredCreature("Archon (Hound)", 1, 0, OptionalInt.of(7)))));

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

	private static final AppliedBonus PRAYER =
		new AppliedBonus(new BonusSourceKey(BonusSourceKey.SPELL, "", "Prayer"), OptionalInt.empty());

	@Test
	public void offersAStripForTheGroupBonuses() throws IOException, ExportException
	{
		getCharacter().setConjurationCatalog(A_CATALOG);

		assertShows(LanguageBundle.getString("in_conjurations_group"), render(getCharacter()));
	}

	@Test
	public void showsAChipForEveryGroupBonus() throws IOException, ExportException
	{
		PlayerCharacter character = getCharacter();
		character.setConjurationCatalog(A_CATALOG);
		character.setConjurationSession(new ConjurationSession(0, List.of(), List.of(PRAYER)));

		assertShows("data-bonus=\"SPELL=Prayer\"", render(character));
	}

	@Test
	public void showsAChipForACreaturesOwnBonus() throws IOException, ExportException
	{
		PlayerCharacter character = getCharacter();
		character.setConjurationCatalog(A_CATALOG);
		character.setConjurationSession(new ConjurationSession(0, List.of(
			new ConjuredCreature("Archon (Hound)", 1, 0, OptionalInt.empty(), List.of(PRAYER)))));

		assertShows("<span class=\"chip own\" data-bonus=\"SPELL=Prayer\"", render(character));
	}

	@Test
	public void escapesMarkupInABonusKey() throws IOException, ExportException
	{
		PlayerCharacter character = getCharacter();
		character.setConjurationCatalog(A_CATALOG);
		character.setConjurationSession(new ConjurationSession(0, List.of(), List.of(new AppliedBonus(
			new BonusSourceKey(BonusSourceKey.SPELL, "", "<script>window.pcgen.dismissAll()</script>"),
			OptionalInt.empty()))));

		assertFalse(render(character).contains("<script>window.pcgen"), "A bonus key reached the page unescaped");
	}

	@Test
	public void escapesMarkupWrittenIntoTheCatalog() throws IOException, ExportException
	{
		getCharacter().setConjurationCatalog("""
			## Summon Monster I
			summon: <script>window.pcgen.dismissAll()</script>
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
			List.of(new ConjuredCreature("Archon (Hound)", 1, 14, OptionalInt.empty())), List.of(PRAYER)));

		String html = render(character.cloneForExport());

		assertShows("Summon Monster IV", html);
		assertShows("<span class=\"hp-damage\">14</span>", html);
		assertShows("data-bonus=\"SPELL=Prayer\"", html);
	}
}
