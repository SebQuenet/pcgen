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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import java.util.Map;
import java.util.Set;

import pcgen.core.Globals;
import pcgen.core.PlayerCharacter;
import pcgen.core.Race;
import pcgen.core.conjurations.BestiarySources;
import pcgen.core.gametable.CombatantSheets;
import pcgen.core.gametable.ForgedMonster;
import pcgen.core.gametable.MonsterForge;
import pcgen.core.gametable.MonsterRecipe;
import pcgen.core.gametable.MonsterSource;
import pcgen.session.HeadlessUIDelegate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The values the bestiary filters on, read from a race or computed on the built creature, on the Bestiaries.
 */
class FactsTest
{
	private static MonsterForge forge;
	private static CombatantSheets sheets;

	@BeforeAll
	static void loadTheBestiary()
	{
		forge = new MonsterForge(BestiarySources.campaigns(), new HeadlessUIDelegate());
		sheets = new CombatantSheets(forge.builder());
	}

	private static PlayerCharacter built(String race)
	{
		ForgedMonster monster = forge.forge(MonsterRecipe.of(new MonsterSource.BestiaryRace(race)));
		return assertInstanceOf(ForgedMonster.Forged.class, monster, monster.toString()).character();
	}

	private static Race race(String key)
	{
		return Globals.getContext().getReferenceContext().silentlyGetConstructedCDOMObject(Race.class, key);
	}

	@Test
	void theAbolethsDirectFactsComeFromItsRaceLine()
	{
		Map<CatalogField, FieldValue> aboleth = DirectFacts.ofRace(race("Aboleth"));

		assertEquals(List.of(new FieldValue.Items(Set.of("Aberration")), new FieldValue.Items(Set.of("Aquatic")),
			new FieldValue.Number(7), new FieldValue.Number(8)), List.of(aboleth.get(CatalogField.TYPE),
			aboleth.get(CatalogField.SUBTYPES), aboleth.get(CatalogField.CHALLENGE_RATING), aboleth.get(CatalogField.HIT_DICE)));
	}

	@Test
	void theBuiltAbolethSwimsAndReaches()
	{
		Map<CatalogField, FieldValue> aboleth = ComputedFacts.of(built("Aboleth"), sheets);

		assertEquals(List.of(new FieldValue.Number(60), new FieldValue.Number(15), new FieldValue.Number(10)),
			List.of(aboleth.get(CatalogField.SWIM_SPEED), aboleth.get(CatalogField.REACH), aboleth.get(CatalogField.LAND_SPEED)));
	}

	@Test
	void theBuiltWolfsArmorClassIsItsSheets()
	{
		PlayerCharacter wolf = built("Wolf");

		assertEquals(new FieldValue.Number(sheets.stats(wolf).armorClass()), ComputedFacts.of(wolf, sheets).get(CatalogField.ARMOR_CLASS));
	}

	@Test
	void aSkeletonHasNoConstitution()
	{
		assertEquals(new FieldValue.Flag(true), ComputedFacts.of(built("Skeleton (Human)"), sheets).get(CatalogField.NO_CONSTITUTION));
	}

	@Test
	void aDretchResistsFireAndIsImmuneToElectricity()
	{
		Map<CatalogField, FieldValue> dretch = ComputedFacts.of(built("Demon (Dretch)"), sheets);

		assertEquals(List.of(10, true, true), List.of(((FieldValue.Scores) dretch.get(CatalogField.RESISTANCES)).values().get("Fire"),
			((FieldValue.Items) dretch.get(CatalogField.IMMUNITIES)).values().contains("Electricity"),
			((FieldValue.Flag) dretch.get(CatalogField.TELEPATHY)).value()));
	}

	@Test
	void aDretchsDamageReductionAndWhatOvercomesIt()
	{
		Map<CatalogField, FieldValue> dretch = ComputedFacts.of(built("Demon (Dretch)"), sheets);

		assertEquals(List.of(new FieldValue.Number(5), new FieldValue.Items(Set.of("Cold Iron", "Good"))),
			List.of(dretch.get(CatalogField.DAMAGE_REDUCTION), dretch.get(CatalogField.DAMAGE_REDUCTION_BYPASS)));
	}
}
