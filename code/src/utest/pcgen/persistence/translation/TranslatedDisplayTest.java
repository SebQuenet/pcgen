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

import java.util.List;

import pcgen.cdom.enumeration.ListKey;
import pcgen.cdom.enumeration.StringKey;
import pcgen.core.Description;
import pcgen.core.analysis.OutputNameFormatting;
import pcgen.core.spell.Spell;
import pcgen.rules.context.AbstractReferenceContext;
import pcgen.rules.context.ConsolidatedListCommitStrategy;
import pcgen.rules.context.RuntimeLoadContext;
import pcgen.rules.context.RuntimeReferenceContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Behaviour of what a translated object shows: its output name, temporary description and
 * descriptions, each falling back to English.
 */
class TranslatedDisplayTest
{
	private AbstractReferenceContext references;
	private Spell bless;

	@BeforeEach
	void setUp()
	{
		references = new RuntimeLoadContext(RuntimeReferenceContext.createRuntimeReferenceContext(),
			new ConsolidatedListCommitStrategy()).getReferenceContext();
		bless = references.constructCDOMObject(Spell.class, "Bless");
	}

	private void apply(String table)
	{
		TranslationApplier.apply(TranslationTableParser.parse(table), references);
	}

	@Test
	void showsTheTranslatedOutputName()
	{
		bless.put(StringKey.OUTPUT_NAME, "Bless (Spell)");

		apply("SPELL\t\tBless\tOUTPUTNAME\tBénédiction (sort)");

		assertEquals("Bénédiction (sort)", OutputNameFormatting.getOutputName(bless));
	}

	@Test
	void showsTheTranslatedNameWhenThereIsNoOutputName()
	{
		apply("SPELL\t\tBless\tNAME\tBénédiction");

		assertEquals("Bénédiction", OutputNameFormatting.getOutputName(bless));
	}

	@Test
	void fillsAnEnglishOutputNamePatternWithTheTranslatedName()
	{
		bless.put(StringKey.OUTPUT_NAME, "[NAME] (Greater)");

		apply("SPELL\t\tBless\tNAME\tBénédiction");

		assertEquals("Bénédiction (Greater)", OutputNameFormatting.getOutputName(bless));
	}

	@Test
	void showsTheTranslatedTemporaryDescription()
	{
		bless.put(StringKey.TEMP_DESCRIPTION, "+1 morale bonus");

		apply("SPELL\t\tBless\tTEMPDESC\tBonus de moral de +1");

		assertEquals("Bonus de moral de +1", bless.getLocalizedSafe(StringKey.TEMP_DESCRIPTION));
	}

	@Test
	void showsTheEnglishTemporaryDescriptionWithoutATranslation()
	{
		bless.put(StringKey.TEMP_DESCRIPTION, "+1 morale bonus");

		assertEquals("+1 morale bonus", bless.getLocalizedSafe(StringKey.TEMP_DESCRIPTION));
	}

	@Test
	void showsTheTranslatedDescription()
	{
		bless.addToListFor(ListKey.DESCRIPTION, new Description("Allies gain a bonus"));

		apply("SPELL\t\tBless\tDESC\tLes alliés gagnent un bonus");

		assertEquals(List.of("Les alliés gagnent un bonus"),
			bless.getLocalizedDescriptions(ListKey.DESCRIPTION).stream().map(Description::getPCCText).toList());
	}

	@Test
	void showsTheEnglishDescriptionsWithoutATranslation()
	{
		bless.addToListFor(ListKey.DESCRIPTION, new Description("Allies gain a bonus"));

		assertEquals(List.of("Allies gain a bonus"),
			bless.getLocalizedDescriptions(ListKey.DESCRIPTION).stream().map(Description::getPCCText).toList());
	}
}
