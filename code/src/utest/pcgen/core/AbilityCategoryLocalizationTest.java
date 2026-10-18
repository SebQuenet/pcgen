package pcgen.core;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pcgen.system.LanguageBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbilityCategoryLocalizationTest
{
	private Locale originalLocale;

	@BeforeEach
	void useFrench()
	{
		originalLocale = Locale.getDefault();
		Locale.setDefault(Locale.FRENCH);
		LanguageBundle.reload();
	}

	@AfterEach
	void restoreLocale()
	{
		Locale.setDefault(originalLocale);
		LanguageBundle.reload();
	}

	@Test
	void translatesCategoryNamesWithoutChangingTheirKeys()
	{
		AbilityCategory favoredClass = new AbilityCategory("Favored Class");
		AbilityCategory summonerAspect = new AbilityCategory("Summoner Aspect");
		AbilityCategory summonerFeature = new AbilityCategory("Summoner Class Feature");

		assertEquals("Classe de prédilection", favoredClass.getDisplayName());
		assertEquals("Aspect de l'invocateur", summonerAspect.getDisplayName());
		assertEquals("Aptitude de classe de l'invocateur", summonerFeature.getDisplayName());
		assertEquals("Favored Class", favoredClass.getKeyName());
		assertEquals("Summoner Aspect", summonerAspect.getKeyName());
		assertEquals("Summoner Class Feature", summonerFeature.getKeyName());
	}

	@Test
	void translatesLocationsAndKeepsUnknownCategoriesReadable()
	{
		assertEquals("Dons", LanguageBundle.getTerm("in_abilityLocation_", "Feats"));
		assertEquals("Capacités raciales", LanguageBundle.getTerm("in_abilityLocation_", "Racial Abilities"));
		assertEquals("Historique", LanguageBundle.getTerm("in_abilityLocation_", "Background"));
		assertEquals("Aptitudes de classe", LanguageBundle.getTerm("in_abilityLocation_", "Class Features"));
		assertEquals("Other Feature", new AbilityCategory("Other Feature").getDisplayName());
	}
}
