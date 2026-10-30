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
package pcgen.core.gametable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import pcgen.core.conjurations.BonusSourceKey;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

/**
 * A scenario file keeps everything the game master prepared and the fight in progress.
 */
class ScenarioJsonTest
{
	private static Scenario prepared()
	{
		MonsterRecipe goblin = MonsterRecipe.of(new MonsterSource.BestiaryRace("Goblin")).withTemplate(
			"Advanced Creature").withHitDice(2);
		MonsterRecipe chief = MonsterRecipe.of(new MonsterSource.CharacterFile("/campaign/goblin-chief.pcg"))
			.withoutTemplate("Fiendish Creature");
		Encounter ambush = Encounter.named("Goblin ambush").withNotes("On the road to Sandpoint")
			.withMonster(new EncounterMonster("Goblin", goblin, 3)).withMonster(new EncounterMonster("Chief", chief, 1))
			.withImage(ScenarioImageTest.image(2, "Thistletop bridge", ImageCategory.BATTLEMAP));
		Combatant seoni = Combatant.player(1, "Seoni", Optional.of("/players/seoni.pcg"));
		Combatant hurt = Combatant.monster(2, "Goblin 1", "goblins", goblin, 8, 3, true);
		Combatant archon = Combatant.player(3, "Hound Archon 1", Optional.empty()).withRoundsLeft(OptionalInt.of(4));
		Combat combat = Combat.empty().with(seoni).with(hurt).withInitiative(1, 15).withInitiative(2, 12)
			.summoned(1, List.of(archon)).damaged(2, 5)
			.withCondition(2, new Condition("Shaken", Optional.of(new BonusSourceKey(BonusSourceKey.TEMPLATE, "",
				"Shaken")), OptionalInt.of(2)))
			.withCondition(2, new Condition("Strength damage", Optional.of(new BonusSourceKey(BonusSourceKey.TEMPLATE, "",
				"Ability Damaged (Strength)")), OptionalInt.empty(), OptionalInt.of(4)))
			.withCondition(1, Condition.label("Invisible", OptionalInt.empty()))
			.cast(1, Condition.label("Mage Armor", OptionalInt.of(40)), List.of(1, 2)).withSpell(1, new CastSpell("Shield", OptionalInt.empty()))
			.withExperience(1_200).withEncounter("Goblin ambush").started()
			.next().combat();
		LootBlock loot = new LootBlock(1, "Goblin ambush", "4", 1_200,
			List.of(LootLine.carried("Dogslicer", 2, "Goblin 1", 16, false, false),
				new LootLine("Coins", 1, "Goblin 2", Optional.of(TreasureType.STANDARD), 300, true, true)),
			List.of(new LootDraw("Goblin 2", "humanoid", TreasureType.STANDARD, 300)));
		return new Scenario("Pathfinder_RPG", List.of("Core Rulebook", "Bestiary"),
			List.of(new PartyMember("Seoni", Optional.of("/players/seoni.pcg")), new PartyMember("Valeros",
				Optional.empty())), List.of(ambush), combat.delaying(3), List.of(loot), List.of())
			.withImage(ScenarioImageTest.image(1, "Sandpoint", ImageCategory.GENERAL_MAP));
	}

	@Test
	void aScenarioSavedBeforeImagesReadsWithNone() throws JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode saved = (ObjectNode) mapper.readTree(ScenarioJson.write(prepared()));
		saved.remove("images");
		((ObjectNode) saved.get("encounters").get(0)).remove("images");

		Scenario read = ((ScenarioReading.Read) ScenarioJson.read(mapper.writeValueAsString(saved))).scenario();

		assertEquals(List.of(List.of(), List.of()), List.of(read.images(), read.encounters().getFirst().images()));
	}

	@Test
	void aScenarioSavedBeforeLootReadsWithNone() throws JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode saved = (ObjectNode) mapper.readTree(ScenarioJson.write(prepared()));
		saved.remove("loot");
		((ObjectNode) saved.get("combat")).remove("encounters");

		Scenario read = ((ScenarioReading.Read) ScenarioJson.read(mapper.writeValueAsString(saved))).scenario();

		assertEquals(List.of(List.of(), List.of()), List.of(read.loot(), read.combat().encounters()));
	}

	@Test
	void aScenarioReadsBackAsItWasWritten()
	{
		Scenario scenario = prepared();

		ScenarioReading reading = ScenarioJson.read(ScenarioJson.write(scenario));

		assertEquals(new ScenarioReading.Read(scenario), reading);
	}

	@Test
	void aScenarioSavedBeforeSpellsWereKeptReadsWithNone() throws JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode saved = mapper.readTree(ScenarioJson.write(prepared()));
		saved.get("combat").get("combatants").forEach(fighter -> ((ObjectNode) fighter).remove("spells"));
		String json = mapper.writeValueAsString(saved);

		ScenarioReading reading = ScenarioJson.read(json);

		assertEquals(List.of(), ((ScenarioReading.Read) reading).scenario().combat().combatant(1).orElseThrow().spells());
	}

	@Test
	void anEncounterMonsterBuiltFromAKitComesBack()
	{
		MonsterRecipe captain = MonsterRecipe.of(new MonsterSource.NpcKit("Black Claw Elite")).withHitDice(1);
		Scenario scenario = Scenario.empty().withEncounters(List.of(Encounter.named("Kobold warren")
			.withMonster(new EncounterMonster("Black Claw Elite", captain, 2))));

		ScenarioReading reading = ScenarioJson.read(ScenarioJson.write(scenario));

		assertEquals(captain, ((ScenarioReading.Read) reading).scenario().encounters().getFirst().monsters().getFirst().recipe());
	}

	@Test
	void aUniqueCreatureComesBack()
	{
		UniqueCreature sombreCroc = new UniqueCreature("Sombre-croc", MonsterRecipe.of(new MonsterSource.BestiaryRace("Wolf"))
			.withTemplate("Advanced Creature").withHitDice(2));
		Scenario scenario = Scenario.empty().withUniques(List.of(sombreCroc));

		ScenarioReading reading = ScenarioJson.read(ScenarioJson.write(scenario));

		assertEquals(List.of(sombreCroc), ((ScenarioReading.Read) reading).scenario().uniques());
	}

	@Test
	void aScenarioWithoutUniquesOpensWithNone() throws JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode saved = (ObjectNode) mapper.readTree(ScenarioJson.write(prepared()));
		saved.remove("uniques");

		Scenario read = ((ScenarioReading.Read) ScenarioJson.read(mapper.writeValueAsString(saved))).scenario();

		assertEquals(List.of(), read.uniques());
	}

	@Test
	void textThatIsNotJsonIsUnreadable()
	{
		assertInstanceOf(ScenarioReading.Unreadable.class, ScenarioJson.read("not a scenario"));
	}

	@Test
	void anEncounterWithoutANameIsUnreadable()
	{
		String json = ScenarioJson.write(prepared()).replace("\"Goblin ambush\"", "\"\"");

		assertInstanceOf(ScenarioReading.Unreadable.class, ScenarioJson.read(json));
	}

	@Test
	void aMonsterWithoutASourceIsUnreadable()
	{
		String json = ScenarioJson.write(prepared()).replace("\"race\"", "\"breed\"");

		assertInstanceOf(ScenarioReading.Unreadable.class, ScenarioJson.read(json));
	}
}
