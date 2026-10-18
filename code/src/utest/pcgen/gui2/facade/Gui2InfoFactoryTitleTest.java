package pcgen.gui2.facade;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import pcgen.cdom.base.CDOMObject;
import pcgen.cdom.enumeration.ObjectKey;
import pcgen.core.Ability;
import pcgen.core.AbilityCategory;
import pcgen.core.Equipment;
import pcgen.core.spell.Spell;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Gui2InfoFactoryTitleTest
{
	private final Locale originalLocale = Locale.getDefault();

	@AfterEach
	void restoreLocale()
	{
		Locale.setDefault(originalLocale);
	}

	@Test
	void showsOriginalNamesOfTranslatedFeatsSpellsAndEquipmentInFrench()
	{
		Locale.setDefault(Locale.FRENCH);
		assertAbilityTitle("FEAT", "Power Attack", "Attaque en puissance");
		assertAbilityTitle("Special Ability", "Mythic Power", "Pouvoir mythique");
		assertAbilityTitle("Special Ability", "Rogue Talent", "Talent de roublard");
		assertAbilityTitle("Trait", "Anatomist", "Anatomiste");
		assertTitle(new Spell(), "Magic Missile", "Projectile magique");
		assertTitle(new Equipment(), "Longsword", "Épée longue");
		Locale.setDefault(Locale.of("fre", "FRE"));
		assertTitle(new Ability(), "Power Attack", "Attaque en puissance");
	}

	@Test
	void leavesUntranslatedNamesAlone()
	{
		Locale.setDefault(Locale.FRENCH);
		Ability feat = new Ability();
		feat.setName("Power Attack");
		assertEquals("Power Attack", Gui2InfoFactory.titleWithEnglishName(feat));
	}

	@Test
	void leavesOtherLanguagesAlone()
	{
		Locale.setDefault(Locale.ENGLISH);
		Ability feat = new Ability();
		feat.setName("Power Attack");
		feat.setLocalizedName("Attaque en puissance");
		assertEquals("Attaque en puissance", Gui2InfoFactory.titleWithEnglishName(feat));
	}

	private static void assertTitle(CDOMObject object, String english, String french)
	{
		object.setName(english);
		object.setLocalizedName(french);
		assertEquals(french + " (" + english + ")", Gui2InfoFactory.titleWithEnglishName(object));
	}

	private static void assertAbilityTitle(String category, String english, String french)
	{
		Ability ability = new Ability();
		ability.put(ObjectKey.ABILITY_CAT, new AbilityCategory(category));
		assertTitle(ability, english, french);
	}
}
