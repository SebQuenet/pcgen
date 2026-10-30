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
package pcgen.core.gametable.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Criteria evaluated on an entry's values: a value not computed yet leaves the answer unknown.
 */
class CriterionTest
{
	private static EntryFacts facts(Map<CatalogField, FieldValue> values)
	{
		return new EntryFacts(values, Optional.empty());
	}

	private static Criterion.Test test(CatalogField field, Operator operator, String... operands)
	{
		return new Criterion.Test(field, operator, List.of(operands));
	}

	@Test
	void aNumberBetweenItsBoundsMatches()
	{
		Criterion between = test(CatalogField.CHALLENGE_RATING, Operator.BETWEEN, "5", "8");

		List<Match> matches = List.of(between.evaluate(facts(Map.of(CatalogField.CHALLENGE_RATING, new FieldValue.Number(7)))),
			between.evaluate(facts(Map.of(CatalogField.CHALLENGE_RATING, new FieldValue.Number(9)))));

		assertEquals(List.of(Match.YES, Match.NO), matches);
	}

	@Test
	void aFractionalChallengeRatingCompares()
	{
		Criterion atMostOne = test(CatalogField.CHALLENGE_RATING, Operator.AT_MOST, "1/2");

		assertEquals(Match.YES, atMostOne.evaluate(facts(Map.of(CatalogField.CHALLENGE_RATING, new FieldValue.Number(1.0 / 3)))));
	}

	@Test
	void aMissingComputedValueIsUnknown()
	{
		Criterion armor = test(CatalogField.ARMOR_CLASS, Operator.AT_LEAST, "20");

		assertEquals(Match.UNKNOWN, armor.evaluate(facts(Map.of())));
	}

	@Test
	void allOfAnUnknownAndAYesIsUnknown()
	{
		Criterion all = new Criterion.Group(Joiner.ALL, List.of(test(CatalogField.ARMOR_CLASS, Operator.AT_LEAST, "20"),
			test(CatalogField.HIT_DICE, Operator.AT_LEAST, "1")));

		assertEquals(Match.UNKNOWN, all.evaluate(facts(Map.of(CatalogField.HIT_DICE, new FieldValue.Number(4)))));
	}

	@Test
	void allOfAnUnknownAndANoIsNo()
	{
		Criterion all = new Criterion.Group(Joiner.ALL, List.of(test(CatalogField.ARMOR_CLASS, Operator.AT_LEAST, "20"),
			test(CatalogField.HIT_DICE, Operator.AT_LEAST, "10")));

		assertEquals(Match.NO, all.evaluate(facts(Map.of(CatalogField.HIT_DICE, new FieldValue.Number(4)))));
	}

	@Test
	void anyOfAnUnknownAndAYesIsYes()
	{
		Criterion any = new Criterion.Group(Joiner.ANY, List.of(test(CatalogField.ARMOR_CLASS, Operator.AT_LEAST, "20"),
			test(CatalogField.HIT_DICE, Operator.AT_LEAST, "1")));

		assertEquals(Match.YES, any.evaluate(facts(Map.of(CatalogField.HIT_DICE, new FieldValue.Number(4)))));
	}

	@Test
	void anEmptyGroupMatchesEverything()
	{
		assertEquals(Match.YES, new Criterion.Group(Joiner.ANY, List.of()).evaluate(facts(Map.of())));
	}

	@Test
	void nestedGroupsCombine()
	{
		Criterion flyingOrSwimmingUndead = new Criterion.Group(Joiner.ALL, List.of(
			test(CatalogField.TYPE, Operator.ANY_OF, "Undead"),
			new Criterion.Group(Joiner.ANY, List.of(test(CatalogField.FLY_SPEED, Operator.AT_LEAST, "1"),
				test(CatalogField.SWIM_SPEED, Operator.AT_LEAST, "1")))));
		EntryFacts ghost = facts(Map.of(CatalogField.TYPE, new FieldValue.Items(Set.of("Undead")),
			CatalogField.FLY_SPEED, new FieldValue.Number(30), CatalogField.SWIM_SPEED, new FieldValue.Number(0)));

		assertEquals(Match.YES, flyingOrSwimmingUndead.evaluate(ghost));
	}

	@Test
	void textIgnoresCaseAndAccents()
	{
		Criterion name = test(CatalogField.NAME, Operator.CONTAINS, "elementaire");

		assertEquals(Match.YES, name.evaluate(facts(Map.of(CatalogField.NAME, new FieldValue.Text("Élémentaire de feu")))));
	}

	@Test
	void noneOfExcludesASubtype()
	{
		Criterion notAquatic = test(CatalogField.SUBTYPES, Operator.NONE_OF, "aquatic");

		assertEquals(Match.NO, notAquatic.evaluate(facts(Map.of(CatalogField.SUBTYPES, new FieldValue.Items(Set.of("Aquatic"))))));
	}

	@Test
	void anInvalidPatternMatchesNothing()
	{
		Criterion broken = test(CatalogField.NAME, Operator.MATCHES, "(");

		assertEquals(Match.NO, broken.evaluate(facts(Map.of(CatalogField.NAME, new FieldValue.Text("Aboleth")))));
	}

	@Test
	void aSkillScoreIsComparedByItsName()
	{
		Criterion stealthy = test(CatalogField.SKILLS, Operator.AT_LEAST, "Stealth", "20");
		EntryFacts assassin = facts(Map.of(CatalogField.SKILLS, new FieldValue.Scores(Map.of("Stealth", 22, "Climb", 4))));

		assertEquals(Match.YES, stealthy.evaluate(assassin));
	}

	@Test
	void aScoreTheEntryLacksDoesNotMatch()
	{
		Criterion fireproof = test(CatalogField.RESISTANCES, Operator.AT_LEAST, "Fire", "10");

		assertEquals(Match.NO, fireproof.evaluate(facts(Map.of(CatalogField.RESISTANCES, new FieldValue.Scores(Map.of())))));
	}

	@Test
	void aFlagIsTrueOrFalse()
	{
		Criterion telepathic = test(CatalogField.TELEPATHY, Operator.IS_TRUE);

		assertEquals(Match.NO, telepathic.evaluate(facts(Map.of(CatalogField.TELEPATHY, new FieldValue.Flag(false)))));
	}
}
