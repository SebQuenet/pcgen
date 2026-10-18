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
package pcgen.persistence.translation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import pcgen.cdom.enumeration.ListKey;
import pcgen.cdom.enumeration.ObjectKey;
import pcgen.cdom.enumeration.StringKey;
import pcgen.cdom.enumeration.SubClassCategory;
import pcgen.core.Ability;
import pcgen.core.AbilityCategory;
import pcgen.core.Campaign;
import pcgen.core.Description;
import pcgen.core.SubClass;
import pcgen.core.bonus.Bonus;
import pcgen.core.spell.Spell;
import pcgen.rules.context.AbstractReferenceContext;
import pcgen.rules.context.ConsolidatedListCommitStrategy;
import pcgen.rules.context.RuntimeLoadContext;
import pcgen.rules.context.RuntimeReferenceContext;
import pcgen.util.enumeration.Visibility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Behaviour of listing, campaign by campaign, the English text a translation table can replace.
 */
class TranslationSourceExporterTest
{
	private AbstractReferenceContext references;
	private Campaign coreRulebook;

	@BeforeEach
	void setUp()
	{
		references = new RuntimeLoadContext(RuntimeReferenceContext.createRuntimeReferenceContext(),
			new ConsolidatedListCommitStrategy()).getReferenceContext();
		coreRulebook = new Campaign();
		coreRulebook.setName("Core Rulebook");
	}

	private Spell spellFromTheCoreRulebook(String name)
	{
		Spell spell = references.constructCDOMObject(Spell.class, name);
		spell.put(ObjectKey.SOURCE_CAMPAIGN, coreRulebook);
		return spell;
	}

	private List<TranslatableText> exported()
	{
		Map<Campaign, List<TranslatableText>> byCampaign = TranslationSourceExporter.textsByCampaign(references);
		return byCampaign.getOrDefault(coreRulebook, List.of());
	}

	@Test
	void listsTheNameOfAnObjectUnderItsCampaign()
	{
		spellFromTheCoreRulebook("Magic Missile");

		assertEquals(List.of(new TranslatableText("SPELL", "", "Magic Missile", TranslatedField.NAME, "Magic Missile", false)),
			exported());
	}

	@Test
	void listsTheDescriptionTemplateAsWrittenInTheData()
	{
		Spell fireball = spellFromTheCoreRulebook("Fireball");
		Description description = new Description("Deals %1d6 fire damage; a save halves it");
		description.addVariable("CASTERLEVEL");
		fireball.addToListFor(ListKey.DESCRIPTION, description);

		assertTrue(exported().contains(new TranslatableText("SPELL", "", "Fireball", TranslatedField.DESC,
			"Deals %1d6 fire damage; a save halves it", false)));
	}

	@Test
	void listsTheOutputNameTemporaryDescriptionAndAbbreviation()
	{
		Spell bless = spellFromTheCoreRulebook("Bless");
		bless.put(StringKey.OUTPUT_NAME, "Bless (Spell)");
		bless.put(StringKey.TEMP_DESCRIPTION, "+1 morale bonus");
		bless.put(StringKey.ABB_KR, "BLS");

		assertEquals(List.of(TranslatedField.NAME, TranslatedField.OUTPUTNAME, TranslatedField.TEMPDESC, TranslatedField.ABB),
			exported().stream().map(TranslatableText::field).sorted().toList());
	}

	@Test
	void givesAnAbilityItsCategory()
	{
		references.importObject(AbilityCategory.FEAT);
		Ability powerAttack = AbilityCategory.FEAT.newInstance();
		powerAttack.setName("Power Attack");
		powerAttack.put(ObjectKey.SOURCE_CAMPAIGN, coreRulebook);
		references.importObject(powerAttack);

		assertEquals("FEAT", exported().getFirst().category());
	}

	@Test
	void namesASubclassUnderItsParentClass()
	{
		SubClass abjurer = new SubClass();
		abjurer.setName("Abjurer");
		abjurer.setCDOMCategory(SubClassCategory.getConstant("Wizard"));
		abjurer.put(ObjectKey.SOURCE_CAMPAIGN, coreRulebook);
		references.importObject(abjurer);

		assertEquals(List.of(new TranslatableText("SUBCLASS", "Wizard", "Abjurer", TranslatedField.NAME, "Abjurer", false)),
			exported());
	}

	@Test
	void leavesOutAnObjectNobodySees()
	{
		spellFromTheCoreRulebook("Hidden Helper").put(ObjectKey.VISIBILITY, Visibility.HIDDEN);

		assertTrue(exported().isEmpty());
	}

	@Test
	void exportsHiddenTemporaryBonusesShownInTheBonusTab()
	{
		Spell spell = spellFromTheCoreRulebook("Temporary Effect");
		spell.put(ObjectKey.VISIBILITY, Visibility.HIDDEN);
		spell.put(StringKey.TEMP_DESCRIPTION, "Choose a value");
		spell.put(StringKey.TEMPVALUE, "MIN=1|MAX=20|TITLE=Ability Damage");
		spell.addToListFor(ListKey.BONUS_ANYPC, Bonus.newBonus(
			new RuntimeLoadContext(RuntimeReferenceContext.createRuntimeReferenceContext(),
				new ConsolidatedListCommitStrategy()), "COMBAT|AC|2"));

		assertEquals(List.of(TranslatedField.NAME, TranslatedField.TEMPDESC, TranslatedField.TEMPVALUE_TITLE),
			exported().stream().map(TranslatableText::field).toList());
	}

	@Test
	void leavesOutAnInternalAbility()
	{
		assertTrue(exportedAbilityOf(hiddenCategory("Internal")).isEmpty());
	}

	@Test
	void keepsAnAbilityOfAHiddenCategoryThatTheSheetShows()
	{
		assertEquals(1, exportedAbilityOf(hiddenCategory("Special Ability")).size());
	}

	private AbilityCategory hiddenCategory(String key)
	{
		AbilityCategory category = new AbilityCategory(key);
		category.setVisible(Visibility.HIDDEN);
		references.importObject(category);
		return category;
	}

	private List<TranslatableText> exportedAbilityOf(AbilityCategory category)
	{
		Ability ability = category.newInstance();
		ability.setName("Some Ability");
		ability.put(ObjectKey.SOURCE_CAMPAIGN, coreRulebook);
		references.importObject(ability);
		return exported();
	}

	@Test
	void listsEachDescriptionOfAnObjectThatHasSeveral()
	{
		Spell fireball = spellFromTheCoreRulebook("Fireball");
		fireball.addToListFor(ListKey.DESCRIPTION, new Description("First part"));
		fireball.addToListFor(ListKey.DESCRIPTION, new Description("Second part"));

		assertEquals(List.of(new TranslatableText("SPELL", "", "Fireball", TranslatedField.DESC, "First part", true),
				new TranslatableText("SPELL", "", "Fireball", TranslatedField.DESC, "Second part", true)),
			exported().stream().filter(text -> text.field() == TranslatedField.DESC).toList());
	}
}
