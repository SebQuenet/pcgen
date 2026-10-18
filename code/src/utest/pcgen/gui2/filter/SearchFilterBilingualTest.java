package pcgen.gui2.filter;

import org.junit.jupiter.api.Test;

import pcgen.cdom.enumeration.ObjectKey;
import pcgen.core.Ability;
import pcgen.core.AbilityCategory;
import pcgen.core.BodyStructure;
import pcgen.core.Equipment;
import pcgen.core.PCClass;
import pcgen.core.spell.Spell;
import pcgen.facade.core.SpellSupportFacade.RootNode;
import pcgen.facade.core.SpellSupportFacade.SpellNode;
import pcgen.gui2.facade.EquipNode;
import pcgen.gui2.facade.SpellFacadeImplem;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchFilterBilingualTest
{
	@Test
	void findsFeatsAndOtherAbilitiesByEitherName()
	{
		assertSearchableAbility("FEAT", "Power Attack", "Attaque en puissance");
		assertSearchableAbility("Special Ability", "Mythic Power", "Pouvoir mythique");
		assertSearchableAbility("Special Ability", "Rogue Talent", "Talent de roublard");
		assertSearchableAbility("Trait", "Anatomist", "Anatomiste");
	}

	private static void assertSearchableAbility(String category, String english, String french)
	{
		Ability ability = new Ability();
		ability.put(ObjectKey.ABILITY_CAT, new AbilityCategory(category));
		ability.setName(english);
		ability.setLocalizedName(french);
		assertTrue(SearchFilterPanel.matchesSearchText(ability, english.toLowerCase()));
		assertTrue(SearchFilterPanel.matchesSearchText(ability, french.toLowerCase()));
		assertFalse(SearchFilterPanel.matchesSearchText(ability, "unrelated"));
	}

	@Test
	void findsSpellsInTheirTreeNodesByEitherName()
	{
		Spell spell = new Spell();
		spell.setName("Magic Missile");
		spell.setLocalizedName("Projectile magique");
		SpellFacadeImplem facade = new SpellFacadeImplem(null, spell, null, null);
		SpellNode node = new SpellNode()
		{
			@Override
			public PCClass getSpellcastingClass()
			{
				return null;
			}

			@Override
			public String getSpellLevel()
			{
				return "1";
			}

			@Override
			public SpellFacadeImplem getSpell()
			{
				return facade;
			}

			@Override
			public RootNode getRootNode()
			{
				return null;
			}

			@Override
			public int getCount()
			{
				return 1;
			}

			@Override
			public void addCount(int count)
			{
			}
		};
		assertTrue(SearchFilterPanel.matchesSearchText(node, "magic missile"));
		assertTrue(SearchFilterPanel.matchesSearchText(node, "projectile magique"));
	}

	@Test
	void findsEquipmentInItsTreeNodeByEitherName()
	{
		Equipment equipment = new Equipment();
		equipment.setName("Longsword");
		equipment.setLocalizedName("Épée longue");
		EquipNode parent = new EquipNode(new BodyStructure("Hand"), 0);
		EquipNode node = new EquipNode(parent, null, equipment, "1");
		assertTrue(SearchFilterPanel.matchesSearchText(node, "longsword"));
		assertTrue(SearchFilterPanel.matchesSearchText(node, "épée longue"));
	}
}
