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
package pcgen.session.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import pcgen.core.conjurations.BestiarySources;
import pcgen.core.gametable.MonsterPreview;
import pcgen.session.service.ServiceError;
import pcgen.session.service.ServiceResult;
import pcgen.session.service.model.CastSpellEntry;
import pcgen.session.service.model.CombatEnded;
import pcgen.session.service.model.CombatantEntry;
import pcgen.session.service.model.GameTableState;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The game table as a transport calls it, on the Pathfinder Bestiary: raw arguments in,
 * the game table out.
 */
class GameTableOperationsTest
{
	private static final Map<String, Object> WOLF = Map.of("race", "Wolf");

	private OperationRegistry registry;

	@BeforeEach
	void prepareTheWolvesInTheWoods()
	{
		registry = OperationRegistry.forSession(BestiarySources.session());
		call("new_scenario", Map.of());
		call("set_party", Map.of("members", List.of(Map.of("name", "Seoni"), Map.of("name", "Valeros"))));
		call("set_encounter", Map.of("name", "Wolves in the woods",
			"monsters", List.of(Map.of("name", "Wolf", "count", 2, "recipe", WOLF))));
	}

	private ServiceResult<?> call(String operation, Map<String, Object> arguments)
	{
		return registry.find(operation).orElseThrow().call(Arguments.of(arguments));
	}

	private static <T> T value(ServiceResult<?> result, Class<T> type)
	{
		assertInstanceOf(ServiceResult.Success.class, result, result::toString);
		return type.cast(((ServiceResult.Success<?>) result).value());
	}

	private static ServiceError error(ServiceResult<?> result)
	{
		assertInstanceOf(ServiceResult.Failure.class, result, result::toString);
		return ((ServiceResult.Failure<?>) result).error();
	}

	private GameTableState launched()
	{
		return value(call("launch_encounter", Map.of("name", "Wolves in the woods")), GameTableState.class);
	}

	private static CombatantEntry named(GameTableState table, String name)
	{
		return table.combat().order().stream().filter(combatant -> combatant.name().equals(name)).findFirst()
			.orElseThrow();
	}

	@Test
	void launchingAnEncounterBringsThePartyAndItsMonsters()
	{
		GameTableState table = launched();

		assertEquals(Set.of("Seoni", "Valeros", "Wolf 1", "Wolf 2"),
			table.combat().order().stream().map(CombatantEntry::name).collect(Collectors.toSet()));
	}

	@Test
	void anEncounterIsWorthItsMonsters()
	{
		GameTableState table = value(call("get_game_table", Map.of()), GameTableState.class);

		assertEquals(800, table.encounters().getFirst().experience());
	}

	@Test
	void shakenChangesAWolfsNumbers()
	{
		int wolf = named(launched(), "Wolf 1").id();

		GameTableState table = value(call("add_condition", Map.of("combatant_id", wolf, "name", "Shaken")),
			GameTableState.class);

		assertTrue(named(table, "Wolf 1").conditions().getFirst().changesNumbers());
	}

	@Test
	void aConditionOnAPlayerIsALabel()
	{
		int seoni = named(launched(), "Seoni").id();

		GameTableState table = value(call("add_condition", Map.of("combatant_id", seoni, "name", "Shaken")),
			GameTableState.class);

		assertEquals(false, named(table, "Seoni").conditions().getFirst().changesNumbers());
	}

	@Test
	void aConditionGoesOnABandAtOnceWithItsValue()
	{
		GameTableState before = launched();
		List<Integer> wolves = List.of(named(before, "Wolf 1").id(), named(before, "Wolf 2").id());

		GameTableState table = value(call("add_condition", Map.of("combatant_ids", wolves,
			"name", "Ability Damaged (Strength)", "value", 4)), GameTableState.class);

		assertEquals(List.of(true, true), List.of(named(table, "Wolf 1").conditions().getFirst().changesNumbers(),
			named(table, "Wolf 2").conditions().getFirst().changesNumbers()));
	}

	@Test
	void aValueOutsideItsBonusRangeIsRefused()
	{
		int wolf = named(launched(), "Wolf 1").id();

		ServiceError refused = error(call("add_condition", Map.of("combatant_id", wolf,
			"name", "Ability Damaged (Strength)", "value", 400)));

		assertInstanceOf(ServiceError.InvalidArgument.class, refused);
	}

	@Test
	void aConditionComesOffABandAtOnce()
	{
		GameTableState before = launched();
		List<Integer> wolves = List.of(named(before, "Wolf 1").id(), named(before, "Wolf 2").id());
		call("add_condition", Map.of("combatant_ids", wolves, "name", "Shaken"));

		GameTableState table = value(call("remove_condition", Map.of("combatant_ids", wolves, "name", "Shaken")),
			GameTableState.class);

		assertEquals(List.of(List.of(), List.of()), List.of(named(table, "Wolf 1").conditions(),
			named(table, "Wolf 2").conditions()));
	}

	@Test
	void aSpellCastIsKeptByItsCasterWithItsTargets()
	{
		GameTableState before = launched();
		int seoni = named(before, "Seoni").id();
		int valeros = named(before, "Valeros").id();

		GameTableState table = value(call("cast_spell", Map.of("caster_id", seoni, "name", "Ward of Sandpoint",
			"rounds", 3, "target_ids", List.of(seoni, valeros))), GameTableState.class);

		assertEquals(new CastSpellEntry("Ward of Sandpoint", 3, List.of(seoni, valeros)), named(table, "Seoni").spells().getFirst());
	}

	@Test
	void aSpellFromTheDataChangesTheNumbersOfTheMonstersItTargets()
	{
		GameTableState before = launched();

		GameTableState table = value(call("cast_spell", Map.of("caster_id", named(before, "Seoni").id(),
			"name", "Shaken", "rounds", 2, "target_ids", List.of(named(before, "Wolf 1").id()))), GameTableState.class);

		assertTrue(named(table, "Wolf 1").conditions().getFirst().changesNumbers());
	}

	@Test
	void aDroppedSpellLeavesItsTargets()
	{
		GameTableState before = launched();
		int seoni = named(before, "Seoni").id();
		int wolf = named(before, "Wolf 1").id();
		call("cast_spell", Map.of("caster_id", seoni, "name", "Shaken", "target_ids", List.of(wolf)));

		GameTableState table = value(call("drop_spell", Map.of("caster_id", seoni, "name", "Shaken")),
			GameTableState.class);

		assertEquals(List.of(), named(table, "Wolf 1").conditions());
	}

	@Test
	void droppingASpellNotCastIsReported()
	{
		int seoni = named(launched(), "Seoni").id();

		ServiceError refused = error(call("drop_spell", Map.of("caster_id", seoni, "name", "Haste")));

		assertInstanceOf(ServiceError.EntryNotFound.class, refused);
	}

	@Test
	void aPlayersHitPointsAreLeftToThePlayer()
	{
		int seoni = named(launched(), "Seoni").id();

		ServiceError refused = error(call("damage_combatant", Map.of("combatant_id", seoni, "amount", 5)));

		assertInstanceOf(ServiceError.NotAllowed.class, refused);
	}

	@Test
	void damageComesOffAWolf()
	{
		int wolf = named(launched(), "Wolf 1").id();

		GameTableState table = value(call("damage_combatant", Map.of("combatant_id", wolf, "amount", 5)),
			GameTableState.class);

		assertEquals(named(table, "Wolf 1").maximumHitPoints() - 5, named(table, "Wolf 1").hitPoints());
	}

	@Test
	void anAnnouncedInitiativePlacesThePlayer()
	{
		int seoni = named(launched(), "Seoni").id();

		GameTableState table = value(call("set_initiative", Map.of("combatant_id", seoni, "score", 40)),
			GameTableState.class);

		assertEquals("Seoni", table.combat().order().getFirst().name());
	}

	@Test
	void anUnknownCombatantIsReported()
	{
		ServiceError refused = error(call("set_initiative", Map.of("combatant_id", 999, "score", 12)));

		assertInstanceOf(ServiceError.EntryNotFound.class, refused);
	}

	@Test
	void aRecipeNamingBothARaceAndAFileIsRefused()
	{
		ServiceError refused = error(call("preview_monster",
			Map.of("recipe", Map.of("race", "Wolf", "character_file", "/somewhere/wolf.pcg"))));

		assertInstanceOf(ServiceError.InvalidArgument.class, refused);
	}

	@Test
	void anAdvancedWolfHasMoreHitPoints()
	{
		MonsterPreview plain = value(call("preview_monster", Map.of("recipe", WOLF)), MonsterPreview.class);

		MonsterPreview advanced = value(call("preview_monster",
			Map.of("recipe", Map.of("race", "Wolf", "added_templates", List.of("Advanced Creature")))),
			MonsterPreview.class);

		assertTrue(advanced.stats().maximumHitPoints() > plain.stats().maximumHitPoints());
	}

	@Test
	void endingTheFightSharesItsExperience()
	{
		launched();

		CombatEnded ended = value(call("end_combat", Map.of()), CombatEnded.class);

		assertEquals(400, ended.perPlayer());
	}

	@Test
	void aSavedScenarioOpensAgain() throws IOException
	{
		File file = Files.createTempFile("wolves", ".pcgtable").toFile();
		file.deleteOnExit();
		call("save_scenario", Map.of("path", file.getPath()));
		call("new_scenario", Map.of());

		GameTableState table = value(call("open_scenario", Map.of("path", file.getPath())), GameTableState.class);

		assertEquals("Wolves in the woods", table.encounters().getFirst().name());
	}
}
