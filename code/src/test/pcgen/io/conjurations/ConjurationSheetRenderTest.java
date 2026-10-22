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
import java.util.Locale;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pcgen.AbstractCharacterTestCase;
import pcgen.core.Globals;
import pcgen.core.PlayerCharacter;
import pcgen.core.Race;
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
	public void rendersGuideInTheInterfaceLanguage() throws IOException, ExportException
	{
		getCharacter().setConjurationCatalog("## IV\nsummon: Archon (Hound)\n");
		Locale previous = Locale.getDefault();
		try
		{
			Locale.setDefault(Locale.ENGLISH);
			LanguageBundle.reload();
			String english = render(getCharacter());
			assertShows("Durable defender", english);
			assertShows("Why summon it", english);
			Locale.setDefault(Locale.FRENCH);
			LanguageBundle.reload();
			String french = render(getCharacter());
			assertShows("Combattant solide", french);
			assertShows("Pourquoi la convoquer", french);
			assertFalse(french.contains("Durable defender"));
		}
		finally
		{
			Locale.setDefault(previous);
			LanguageBundle.reload();
		}
	}

	@Test
	public void routesEveryReferenceLinkOutOfTheEmbeddedSheet() throws IOException, ExportException
	{
		getCharacter().setConjurationCatalog(A_CATALOG);
		String html = render(getCharacter());
		Matcher anchors = Pattern.compile("<a\\b[^>]*href=\"https:[^\"]+\"[^>]*>").matcher(html);
		int references = 0;
		while (anchors.find())
		{
			references++;
			assertTrue(anchors.group().contains("data-reference"), anchors.group());
		}
		assertTrue(references > 0);
		assertShows("closest(\"a[data-reference]\")", html);
	}

	@Test
	public void showsNoPublishedListsWhenTheCharacterHasNoLoadedSources() throws IOException, ExportException
	{
		String html = render(getCharacter());
		assertShows(LanguageBundle.getString("in_conjurations_empty"), html);
		assertFalse(html.contains("data-creature=\"Dire rat"));
	}

	private static final String BEAR_VARIANTS = """
		## Summon Monster VI
		summon: Bear (Dire)
		  template: Resolute Creature, Mythic Simple Template ~ Savage
		summon: Bear (Dire)
		  template: Celestial Creature
		summon: Bear (Dire)
		  template: Resolute Creature
		summon: Archon (Legion)
		""";

	@Test
	public void foldsTheVariantsOfACreatureUnderTheOneWithFewestTemplates() throws IOException, ExportException
	{
		getCharacter().setConjurationCatalog(BEAR_VARIANTS);

		String html = render(getCharacter());

		assertShows("data-creature=\"Bear (Dire) (Celestial Creature)\" data-variant-group=\"Bear (Dire)|6\" "
			+ "data-variant-base=\"true\"", html);
	}

	@Test
	public void hidesTheOtherVariantsUntilUnfolded() throws IOException, ExportException
	{
		getCharacter().setConjurationCatalog(BEAR_VARIANTS);

		String html = render(getCharacter());

		assertShows("data-creature=\"Bear (Dire) (Resolute Creature)\" data-variant-group=\"Bear (Dire)|6\" "
			+ "data-variant-base=\"false\" data-folded=\"true\"", html);
	}

	@Test
	public void countsTheFoldedVariantsOnTheUnfoldButton() throws IOException, ExportException
	{
		getCharacter().setConjurationCatalog(BEAR_VARIANTS);

		String html = render(getCharacter());

		assertShows("class=\"unfold\" data-variant-group=\"Bear (Dire)|6\" aria-expanded=\"false\"", html);
	}

	@Test
	public void givesNoUnfoldButtonToACreatureWithoutVariants() throws IOException, ExportException
	{
		getCharacter().setConjurationCatalog(BEAR_VARIANTS);

		String html = render(getCharacter());

		assertFalse(html.contains("class=\"unfold\" data-variant-group=\"Archon (Legion)|6\""));
	}

	@Test
	public void namesTheTemplatesOfAConjuredVariantOnItsCard() throws IOException, ExportException
	{
		PlayerCharacter character = getCharacter();
		character.setConjurationCatalog(BEAR_VARIANTS);
		character.setConjurationSession(new ConjurationSession(0, List.of(new ConjuredCreature(
			"Bear (Dire) (Resolute Creature) (Mythic Simple Template ~ Savage)", 1, 0, OptionalInt.empty(), List.of(), ""))));

		String html = render(character);

		assertShows("<p class=\"card-templates\"><span class=\"template-chip\">Resolute Creature</span> "
			+ "<span class=\"template-chip\">Mythic Simple Template ~ Savage</span></p>", html);
	}

	@Test
	public void leavesRoomOnACardForWhatItsTemplatesChange() throws IOException, ExportException
	{
		PlayerCharacter character = getCharacter();
		character.setConjurationCatalog(BEAR_VARIANTS);
		character.setConjurationSession(new ConjurationSession(0, List.of(new ConjuredCreature(
			"Bear (Dire) (Celestial Creature)", 1, 0, OptionalInt.empty(), List.of(), ""))));

		String html = render(character);

		assertShows("<dl class=\"template-differences\" hidden", html);
	}

		@Test
	public void linksACreatureToItsFirstD20pfsrdResult() throws IOException, ExportException
	{
		Globals.getContext().getReferenceContext().constructCDOMObject(Race.class, "Wolf");
		getCharacter().setConjurationCatalog("## Summon Monster II\nsummon: Wolf\n");

		String html = render(getCharacter());

		assertShows("href=\"https://duckduckgo.com/?q=%5CWolf%20monster%20site%3Ad20pfsrd.com\"", html);
	}

	@Test
	public void linksACreatureToItsFirstPathfinderFrResult() throws IOException, ExportException
	{
		Globals.getContext().getReferenceContext().constructCDOMObject(Race.class, "Wolf");
		getCharacter().setConjurationCatalog("## Summon Monster II\nsummon: Wolf\n");

		String html = render(getCharacter());

		assertShows("href=\"https://duckduckgo.com/?q=%5CWolf%20Pathfinder-RPG%20site%3Apathfinder-fr.org\"", html);
	}

		@Test
	public void offersTheRolesInAlphabeticalOrder() throws IOException, ExportException
	{
		getCharacter().setConjurationCatalog("## Summon Monster II\nsummon: Wolf\n  role: Tireur, Éclaireur, Cogneur\n");

		String html = render(getCharacter());

		assertShows("<option value=\"Cogneur\">Cogneur</option><option value=\"Éclaireur\">Éclaireur</option>"
			+ "<option value=\"Tireur\">Tireur</option>", html);
	}

		@Test
	public void asksHowManyCreaturesToConjureFromOneToFifteen() throws IOException, ExportException
	{
		getCharacter().setConjurationCatalog("## Summon Monster II\nsummon: Wolf\n");

		String html = render(getCharacter());

		assertShows("<input type=\"number\" class=\"conjure-count\" min=\"1\" max=\"15\" value=\"1\"", html);
	}

		@Test
	public void showsTheSelectedFormOfAHoundArchonInPlay() throws IOException, ExportException
	{
		PlayerCharacter character = getCharacter();
		character.setConjurationCatalog("## IV\nsummon: Archon (Hound)\n");
		character.setConjurationSession(new ConjurationSession(0,
			List.of(new ConjuredCreature("Archon (Hound)", 1, 0, OptionalInt.empty(), List.of(), "wolf"))));

		String html = render(character);

		assertShows("data-form=\"wolf\"", html);
		assertShows("value=\"wolf\" selected", html);
		assertShows("Beast shape II", html);
	}

	@Test
	public void letsThePlayerRenameACreatureInPlay() throws IOException, ExportException
	{
		PlayerCharacter character = getCharacter();
		character.setConjurationCatalog("## IV\nsummon: Archon (Hound)\n");
		character.setConjurationSession(new ConjurationSession(0,
			List.of(new ConjuredCreature("Archon (Hound)", 1, 0, OptionalInt.empty(), List.of(), ""))));

		String html = render(character);

		assertShows("class=\"card-label\" contenteditable=\"true\"", html);
	}

	@Test
	public void givesAPublishedCreatureItsRolesAndHowToPlayEach() throws IOException, ExportException
	{
		getCharacter().setConjurationCatalog("## IV\nsummon: Archon (Hound)\n");
		Locale previous = Locale.getDefault();
		try
		{
			Locale.setDefault(Locale.ENGLISH);
			LanguageBundle.reload();

			String html = render(getCharacter());

			assertShows("data-roles=\"Blocker|Bruiser\"", html);
			assertShows("<option value=\"Blocker\">Blocker</option>", html);
			assertShows("How to play it</h3><p><b>Blocker</b>", html);
		}
		finally
		{
			Locale.setDefault(previous);
			LanguageBundle.reload();
		}
	}

	@Test
	public void filtersByRoleAndByLowestAndHighestLevel() throws IOException, ExportException
	{
		getCharacter().setConjurationCatalog("## IV\nsummon: Archon (Hound)\n  role: tank\n");

		String html = render(getCharacter());

		assertShows("<option value=\"tank\">tank</option>", html);
		assertShows("<select class=\"level-min\"><option value=\"1\" selected>I</option>", html);
		assertShows("<option value=\"9\" selected>IX</option></select>", html);
	}

	@Test
	public void titlesATableWithEveryGroup() throws IOException, ExportException
	{
		getCharacter().setConjurationCatalog("## Summon Monster IV\nsummon: Archon (Hound)\n"
			+ "\n## Summon Monster V\nsummon: Azata (Bralani)\n");

		String html = render(getCharacter());

		assertShows("Summon Monster IV", html);
		assertShows("Summon Monster V", html);
		assertShows("data-creature=\"Archon (Hound)\"", html);
		assertShows("data-creature=\"Azata (Bralani)\"", html);
		assertFalse(html.contains("class=\"group-picker\""), "The catalog must be one list, not a group picker");
	}

	@Test
	public void showsSortableComputedColumnsAndSummoningLevel() throws IOException, ExportException
	{
		getCharacter().setConjurationCatalog("""
			## Convocation de monstres IV
			summon: Archon (Hound)
			  role: tank
			  note: Holds the line
			""");

		String html = render(getCharacter());

		assertShows("data-sort=\"hitDice\"", html);
		assertShows("data-sort=\"expectedDamagePerRound\"", html);
		assertShows("data-sort=\"touchArmorClass\"", html);
		assertShows("data-sort=\"ranking\"", html);
		assertShows("data-sort=\"combatManeuverDefense\"", html);
		assertShows("data-level=\"4\"", html);
		assertShows("tank", html);
		assertShows("Holds the line", html);
		assertShows("class=\"creature-drawer\"", html);
		assertShows("class=\"inspect-creature\" data-creature=\"Archon (Hound)\"", html);
		assertShows("<select class=\"bonus-select\" data-action=\"add-group-bonus\"", html);
		assertShows("<select class=\"bonus-select\" data-action=\"add-bonus\"", html);
		assertShows("class=\"bonus-apply\"", html);
	}

	@Test
	public void aMappedOlderEntryUsesComputedStatsWhileKeepingItsNote() throws IOException, ExportException
	{
		getCharacter().setConjurationCatalog("""
			## VI
			summon: Tigre sanguinaire céleste
			  race: Tiger (Dire)
			  template: Celestial Creature
			  role: frappe
			  note: Bondit
			  hp: 133
			""");

		String html = render(getCharacter());

		assertShows("data-creature=\"Tigre sanguinaire céleste\"", html);
		assertShows("data-authored=\"false\"", html);
		assertShows("Bondit", html);
		assertTrue(html.indexOf("Bondit") > html.indexOf("class=\"creature-drawer\""));
		assertShows("fr".equals(Locale.getDefault().getLanguage())
			? "Principal choix de dégâts au contact" : "Strong melee damage", html);
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
			List.of(new ConjuredCreature("Archon (Hound)", 1, 14, OptionalInt.empty())), List.of(PRAYER))
			.withForm("Archon (Hound)", 1, "wolf").withSkeletonUsed(true));

		String html = render(character.cloneForExport());

		assertShows("Summon Monster IV", html);
		assertShows("<span class=\"hp-damage\">14</span>", html);
		assertShows("data-bonus=\"SPELL=Prayer\"", html);
		assertShows("data-form=\"wolf\"", html);
	}
}
