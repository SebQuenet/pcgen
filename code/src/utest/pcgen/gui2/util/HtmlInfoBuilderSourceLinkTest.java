package pcgen.gui2.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import pcgen.cdom.base.CDOMObject;
import pcgen.cdom.enumeration.ObjectKey;
import pcgen.cdom.enumeration.StringKey;
import pcgen.core.Ability;
import pcgen.core.AbilityCategory;
import pcgen.core.Deity;
import pcgen.core.Domain;
import pcgen.core.PCClass;
import pcgen.core.Race;
import pcgen.core.Skill;
import pcgen.core.spell.Spell;

class HtmlInfoBuilderSourceLinkTest
{
	@Test
	void showsExistingLinksFromSupportedSources()
	{
		assertLink("https://www.d20pfsrd.com/feats/combat-feats/power-attack-combat/", "d20PFSRD");
		assertLink("https://aonprd.com/FeatDisplay.aspx?ItemName=Power%20Attack&Source=Core", "Archives of Nethys");
		assertLink("https://pathfinderwiki.com/wiki/Absalom", "PathfinderWiki");
	}

	@Test
	void omitsMissingUnsupportedOrMalformedLinks()
	{
		Ability ability = new Ability();
		assertEquals("<html>Description</html>", new HtmlInfoBuilder().withSource(ability).append("Description").toString());
		for (String link : new String[] {"https://example.com/feat", "https://d20pfsrd.com.evil.example/",
			"javascript:alert(1)", "not a URL"})
		{
			ability.put(StringKey.SOURCE_LINK, link);
			assertEquals("<html>Description</html>", new HtmlInfoBuilder().withSource(ability).append("Description").toString());
		}
	}

	@Test
	void sendsEachSiteLinkToTheFirstSearchResultOnThatSite()
	{
		Ability feat = paizoEntry(new Ability(), "Power Attack", "core_rulebook/cr_feats.lst");
		feat.put(ObjectKey.ABILITY_CAT, new AbilityCategory("FEAT"));
		assertEquals("<html>Description"
			+ "<br><a href=\"https://duckduckgo.com/?q=%5CPower%20Attack%20feat%20site%3Aaonprd.com\">Archives of Nethys</a>"
			+ "<br><a href=\"https://duckduckgo.com/?q=%5CPower%20Attack%20don%20Pathfinder-RPG%20site%3Apathfinder-fr.org\">"
			+ "Pathfinder-fr</a>"
			+ "<br><a href=\"https://duckduckgo.com/?q=%5CPower%20Attack%20feat%20site%3Apathfinderwiki.com\">PathfinderWiki</a>"
			+ "<br><a href=\"https://duckduckgo.com/?q=%5CPower%20Attack%20feat%20site%3Ad20pfsrd.com\">d20PFSRD</a></html>",
			new HtmlInfoBuilder().withSource(feat).append("Description").toString());
	}

	@Test
	void searchesPathfinderFrWithTheTranslatedName()
	{
		Spell spell = paizoEntry(new Spell(), "Fireball", "core_rulebook/cr_spells.lst");
		spell.put(StringKey.LOCALIZED_NAME, "Boule de feu");
		assertEquals(searchLinks("Fireball spell", "Boule de feu sort"),
			new HtmlInfoBuilder().withSource(spell).append("Description").toString());
	}

	@Test
	void namesTheKindOfEntryInBothLanguages()
	{
		assertEquals(searchLinks("Wolf monster", "Wolf"), describe(paizoEntry(new Race(), "Wolf", "bestiary/bestiary_races.lst")));
		assertEquals(searchLinks("Elf race", "Elf race"), describe(paizoEntry(new Race(), "Elf", "core_rulebook/cr_races.lst")));
		assertEquals(searchLinks("Wizard class", "Wizard classe"), describe(paizoEntry(new PCClass(), "Wizard", "core_rulebook/cr_classes.lst")));
		assertEquals(searchLinks("Desna deity", "Desna divinité"), describe(paizoEntry(new Deity(), "Desna", "core_rulebook/cr_deities.lst")));
		assertEquals(searchLinks("Fire domain", "Fire domaine"), describe(paizoEntry(new Domain(), "Fire", "core_rulebook/cr_domains.lst")));
		assertEquals(searchLinks("Acrobatics skill", "Acrobatics compétence"),
			describe(paizoEntry(new Skill(), "Acrobatics", "core_rulebook/cr_skills.lst")));
	}

	@Test
	void searchesWithTheBareNameForEntriesOfAnotherKind()
	{
		Ability ability = paizoEntry(new Ability(), "Rogue Talent", "core_rulebook/cr_abilities.lst");
		ability.put(ObjectKey.ABILITY_CAT, new AbilityCategory("Special Ability"));
		assertEquals(searchLinks("Rogue Talent", "Rogue Talent"), describe(ability));
	}

	@Test
	void omitsSearchLinksForDataOutsidePaizoPathfinder()
	{
		Ability ability = new Ability();
		ability.setName("Rogue Talent");
		ability.setSourceURI(URI.create("file:/data/3e/paizo/abilities.lst"));
		assertEquals("<html>Description</html>", describe(ability));
	}

	@Test
	void doesNotRepeatTheSiteOfTheSourceLink()
	{
		Ability feat = new Ability();
		feat.setName("Power Attack");
		feat.put(StringKey.SOURCE_LINK, "https://www.d20pfsrd.com/feats/combat-feats/power-attack-combat/");
		assertEquals("<html>Description<br><a href=\"https://www.d20pfsrd.com/feats/combat-feats/power-attack-combat/\">"
			+ "d20PFSRD</a>"
			+ "<br><a href=\"https://duckduckgo.com/?q=%5CPower%20Attack%20site%3Aaonprd.com\">Archives of Nethys</a>"
			+ "<br><a href=\"https://duckduckgo.com/?q=%5CPower%20Attack%20Pathfinder-RPG%20site%3Apathfinder-fr.org\">"
			+ "Pathfinder-fr</a>"
			+ "<br><a href=\"https://duckduckgo.com/?q=%5CPower%20Attack%20site%3Apathfinderwiki.com\">PathfinderWiki</a></html>",
			describe(feat));
	}

	private static <T extends CDOMObject> T paizoEntry(T entry, String name, String bookFile)
	{
		entry.setName(name);
		entry.setSourceURI(URI.create("file:/data/pathfinder/paizo/roleplaying_game/" + bookFile));
		return entry;
	}

	private static String describe(CDOMObject source)
	{
		return new HtmlInfoBuilder().withSource(source).append("Description").toString();
	}

	private static String searchLinks(String englishQuery, String frenchTerms)
	{
		return "<html>Description"
			+ "<br><a href=\"" + firstResult(englishQuery + " site:aonprd.com") + "\">Archives of Nethys</a>"
			+ "<br><a href=\"" + firstResult(frenchTerms + " Pathfinder-RPG site:pathfinder-fr.org") + "\">Pathfinder-fr</a>"
			+ "<br><a href=\"" + firstResult(englishQuery + " site:pathfinderwiki.com") + "\">PathfinderWiki</a>"
			+ "<br><a href=\"" + firstResult(englishQuery + " site:d20pfsrd.com") + "\">d20PFSRD</a></html>";
	}

	private static String firstResult(String query)
	{
		return "https://duckduckgo.com/?q=" + URLEncoder.encode("\\" + query, StandardCharsets.UTF_8).replace("+", "%20");
	}

	private void assertLink(String url, String label)
	{
		Ability ability = new Ability();
		ability.put(StringKey.SOURCE_LINK, url);
		String escapedUrl = url.replace("&", "&amp;");
		assertEquals("<html>Description<br><a href=\"" + escapedUrl + "\">" + label + "</a></html>",
			new HtmlInfoBuilder().withSource(ability).append("Description").toString());
	}
}
