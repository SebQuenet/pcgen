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
			.withMonster(new EncounterMonster("Goblin", goblin, 3)).withMonster(new EncounterMonster("Chief", chief, 1));
		Combatant seoni = Combatant.player(1, "Seoni", Optional.of("/players/seoni.pcg"));
		Combatant hurt = Combatant.monster(2, "Goblin 1", "goblins", goblin, 8, 3, true);
		Combatant archon = Combatant.player(3, "Hound Archon 1", Optional.empty()).withRoundsLeft(OptionalInt.of(4));
		Combat combat = Combat.empty().with(seoni).with(hurt).withInitiative(1, 15).withInitiative(2, 12)
			.summoned(1, List.of(archon)).damaged(2, 5)
			.withCondition(2, new Condition("Shaken", Optional.of(new BonusSourceKey(BonusSourceKey.TEMPLATE, "",
				"Shaken")), OptionalInt.of(2)))
			.withCondition(1, Condition.label("Invisible", OptionalInt.empty())).withExperience(1_200).started()
			.next().combat();
		return new Scenario("Pathfinder_RPG", List.of("Core Rulebook", "Bestiary"),
			List.of(new PartyMember("Seoni", Optional.of("/players/seoni.pcg")), new PartyMember("Valeros",
				Optional.empty())), List.of(ambush), combat.delaying(3));
	}

	@Test
	void aScenarioReadsBackAsItWasWritten()
	{
		Scenario scenario = prepared();

		ScenarioReading reading = ScenarioJson.read(ScenarioJson.write(scenario));

		assertEquals(new ScenarioReading.Read(scenario), reading);
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
