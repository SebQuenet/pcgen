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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import pcgen.cdom.enumeration.ListKey;
import pcgen.cdom.enumeration.ObjectKey;
import pcgen.cdom.enumeration.StringKey;
import pcgen.core.Ability;
import pcgen.core.AbilityCategory;
import pcgen.core.Description;
import pcgen.core.Equipment;
import pcgen.core.spell.Spell;
import pcgen.rules.context.AbstractReferenceContext;
import pcgen.rules.context.ConsolidatedListCommitStrategy;
import pcgen.rules.context.RuntimeLoadContext;
import pcgen.rules.context.RuntimeReferenceContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Behaviour of laying a translation table over the loaded game objects.
 */
class TranslationApplierTest
{
	private AbstractReferenceContext references;

	@BeforeEach
	void setUp()
	{
		references = new RuntimeLoadContext(RuntimeReferenceContext.createRuntimeReferenceContext(),
			new ConsolidatedListCommitStrategy()).getReferenceContext();
	}

	private Spell spellNamed(String name)
	{
		return references.constructCDOMObject(Spell.class, name);
	}

	private Ability featNamed(String name)
	{
		references.importObject(AbilityCategory.FEAT);
		Ability feat = AbilityCategory.FEAT.newInstance();
		feat.setName(name);
		references.importObject(feat);
		return feat;
	}

	private List<TranslationProblem> apply(String table)
	{
		return TranslationApplier.apply(TranslationTableParser.parse(table), references);
	}

	@Test
	void showsTheTranslatedName()
	{
		Spell spell = spellNamed("Magic Missile");

		apply("SPELL\t\tMagic Missile\tNAME\tProjectile magique");

		assertEquals("Projectile magique", spell.toString());
	}

	@Test
	void keepsTheKeyInEnglish()
	{
		Spell spell = spellNamed("Magic Missile");

		apply("SPELL\t\tMagic Missile\tNAME\tProjectile magique");

		assertEquals("Magic Missile", spell.getKeyName());
	}

	@Test
	void findsAnAbilityInItsCategory()
	{
		Ability feat = featNamed("Power Attack");

		apply("ABILITY\tFEAT\tPower Attack\tNAME\tAttaque en puissance");

		assertEquals("Attaque en puissance", feat.toString());
	}

	@Test
	void reportsAnObjectThatIsNotLoaded()
	{
		List<TranslationProblem> problems = apply("SPELL\t\tMagic Missile\tNAME\tProjectile magique");

		assertEquals(1, problems.getFirst().line());
	}

	@Test
	void reportsAnUnknownType()
	{
		List<TranslationProblem> problems = apply("POTATO\t\tMagic Missile\tNAME\tProjectile magique");

		assertEquals(1, problems.size());
	}

	@Test
	void dropsTheTranslatedNameWhenTheObjectIsRenamed()
	{
		Equipment longsword = references.constructCDOMObject(Equipment.class, "Longsword");
		apply("EQUIPMENT\t\tLongsword\tNAME\tÉpée longue");

		longsword.setName("Longsword (Large)");

		assertEquals("Longsword (Large)", longsword.toString());
	}

	@Test
	void keepsTheOutputNameAndTemporaryDescription()
	{
		Spell spell = spellNamed("Bless");

		apply("""
			SPELL\t\tBless\tOUTPUTNAME\tBénédiction (sort)
			SPELL\t\tBless\tTEMPDESC\tBonus de moral de +1
			""");

		assertEquals(List.of("Bénédiction (sort)", "Bonus de moral de +1"),
			List.of(spell.get(StringKey.LOCALIZED_OUTPUT_NAME), spell.get(StringKey.LOCALIZED_TEMP_DESCRIPTION)));
	}

	@Test
	void translatesTheDescriptionTemplateAndKeepsItsVariables()
	{
		Spell spell = spellNamed("Fireball");
		Description english = new Description("Deals %1d6 fire damage");
		english.addVariable("CASTERLEVEL");
		spell.addToListFor(ListKey.DESCRIPTION, english);

		apply("SPELL\t\tFireball\tDESC\tInflige %1d6 dégâts de feu");

		assertEquals("Inflige %1d6 dégâts de feu|CASTERLEVEL", spell.get(ObjectKey.LOCALIZED_DESCRIPTION).getPCCText());
	}

	@Test
	void refusesADescriptionThatLosesAMarker()
	{
		Spell spell = spellNamed("Fireball");
		Description english = new Description("Deals %1d6 fire damage");
		english.addVariable("CASTERLEVEL");
		spell.addToListFor(ListKey.DESCRIPTION, english);

		List<TranslationProblem> problems = apply("SPELL\t\tFireball\tDESC\tInflige des dégâts de feu");

		assertTrue(problems.size() == 1 && spell.get(ObjectKey.LOCALIZED_DESCRIPTION) == null);
	}

	@Test
	void refusesADescriptionForAnObjectWithoutOne()
	{
		Spell spell = spellNamed("Fireball");

		apply("SPELL\t\tFireball\tDESC\tInflige des dégâts de feu");

		assertNull(spell.get(ObjectKey.LOCALIZED_DESCRIPTION));
	}

	@Test
	void translatesTheBenefit()
	{
		Ability feat = featNamed("Power Attack");
		feat.addToListFor(ListKey.BENEFIT, new Description("You can take a penalty"));

		apply("ABILITY\tFEAT\tPower Attack\tBENEFIT\tVous pouvez subir un malus");

		assertEquals("Vous pouvez subir un malus", feat.get(ObjectKey.LOCALIZED_BENEFIT).getPCCText());
	}
}
