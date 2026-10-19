package pcgen.gui2.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import org.junit.jupiter.api.Test;

import pcgen.cdom.enumeration.ObjectKey;
import pcgen.cdom.enumeration.StringKey;
import pcgen.core.Ability;
import pcgen.core.AbilityCategory;
import pcgen.core.Race;
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
	void linksPaizoEntriesWhoseSourceDataHasNoUsableLink()
	{
		Ability feat = new Ability();
		feat.setName("Power Attack");
		feat.put(ObjectKey.ABILITY_CAT, new AbilityCategory("FEAT"));
		feat.setSourceURI(URI.create("file:/data/pathfinder/paizo/roleplaying_game/core_rulebook/cr_feats.lst"));
		assertEquals("<html>Description<br><a href=\"https://aonprd.com/FeatDisplay.aspx?ItemName=Power%20Attack\">"
			+ "Archives of Nethys</a></html>",
			new HtmlInfoBuilder().withSource(feat).append("Description").toString());

		Spell spell = new Spell();
		spell.setName("Magic Missile");
		spell.setSourceURI(URI.create("file:/data/pathfinder/paizo/roleplaying_game/core_rulebook/cr_spells.lst"));
		spell.put(StringKey.SOURCE_LINK, "http://paizo.com/pathfinderRPG/prd/spells/magicMissile.html");
		assertEquals("<html>Description<br><a href=\"https://aonprd.com/SpellDisplay.aspx?ItemName=Magic%20Missile\">"
			+ "Archives of Nethys</a></html>",
			new HtmlInfoBuilder().withSource(spell).append("Description").toString());

		Race creature = new Race();
		creature.setName("Wolf");
		creature.setSourceURI(URI.create("file:/data/pathfinder/paizo/roleplaying_game/bestiary/bestiary_races.lst"));
		assertEquals("<html>Description<br><a href=\"https://aonprd.com/MonsterDisplay.aspx?ItemName=Wolf\">"
			+ "Archives of Nethys</a></html>",
			new HtmlInfoBuilder().withSource(creature).append("Description").toString());
	}

	@Test
	void searchesNethysForPaizoAbilitiesWithoutADedicatedPageType()
	{
		Ability ability = new Ability();
		ability.setName("Rogue Talent");
		ability.put(ObjectKey.ABILITY_CAT, new AbilityCategory("Special Ability"));
		ability.setSourceURI(URI.create("file:/data/pathfinder/paizo/roleplaying_game/core_rulebook/cr_abilities.lst"));
		assertEquals("<html>Description<br><a href=\"https://aonprd.com/Search.aspx?Query=Rogue%20Talent\">"
			+ "Archives of Nethys</a></html>",
			new HtmlInfoBuilder().withSource(ability).append("Description").toString());

		ability.setSourceURI(URI.create("file:/data/3e/paizo/abilities.lst"));
		assertEquals("<html>Description</html>",
			new HtmlInfoBuilder().withSource(ability).append("Description").toString());
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
