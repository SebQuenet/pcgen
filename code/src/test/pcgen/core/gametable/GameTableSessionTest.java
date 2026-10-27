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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import pcgen.core.SettingsHandler;
import pcgen.core.conjurations.BestiarySources;
import pcgen.core.conjurations.BonusSourceKey;
import pcgen.facade.core.BonusChoice;
import pcgen.io.PCGIOHandler;
import pcgen.session.HeadlessUIDelegate;
import pcgen.system.LanguageBundle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The game table running a prepared encounter on the Pathfinder Bestiary.
 */
class GameTableSessionTest
{
	private static final MonsterRecipe WOLF = MonsterRecipe.of(new MonsterSource.BestiaryRace("Wolf"));
	private static final Encounter WOLVES = Encounter.named("Wolves in the woods")
		.withMonster(new EncounterMonster("Wolf", WOLF, 2));
	private static final MonsterRecipe TREANT = MonsterRecipe.of(new MonsterSource.BestiaryRace("Treant"));
	private static final MonsterRecipe CONTRACT_KILLER = MonsterRecipe.of(new MonsterSource.CharacterFile(
		"characters/combat-manager/NPC Codex/Rogue/Contract Killer.pcg"));

	private GameTableSession session;

	@BeforeEach
	void openAScenario()
	{
		session = new GameTableSession(() -> 10, new Random(42));
		session.useSources(new MonsterForge(BestiarySources.campaigns(), new HeadlessUIDelegate()), "Pathfinder_RPG",
			List.of("Core Rulebook", "Bestiary"));
		session.setParty(List.of(new PartyMember("Seoni", Optional.empty()), new PartyMember("Valeros",
			Optional.empty())));
	}

	private Combatant named(String name)
	{
		return session.combat().order().stream().filter(combatant -> combatant.name().equals(name)).findFirst()
			.orElseThrow();
	}

	@Test
	void aPreviewedWolfCarriesItsTacticalPlan()
	{
		Optional<String> plan = switch (session.previewSheet(WOLF))
		{
			case ForgeOutcome.Built<SheetPreview> built -> built.value().monster().getDisplay().getTacticalPlan();
			case ForgeOutcome.Failed<SheetPreview> failed -> Optional.of(failed.reason());
		};

		assertEquals(BestiaryTactics.of("Wolf"), plan);
	}

	@Test
	void launchingAnEncounterBringsThePartyAndTheMonsters()
	{
		session.launch(WOLVES);

		assertEquals(Set.of("Seoni", "Valeros", "Wolf 1", "Wolf 2"),
			session.combat().order().stream().map(Combatant::name).collect(Collectors.toSet()));
	}

	@Test
	void theWolvesShareOneRolledInitiative()
	{
		session.launch(WOLVES);

		assertEquals(named("Wolf 1").initiative().score(), named("Wolf 2").initiative().score());
	}

	@Test
	void aTouchKeepsTheDamageTaken()
	{
		session.launch(WOLVES);
		int wolf = named("Wolf 1").id();
		session.damage(wolf, 5);

		session.retouch(wolf, WOLF.withTemplate("Advanced Creature"));

		assertEquals(5, named("Wolf 1").health().orElseThrow().damage());
	}

	@Test
	void aTouchRaisesTheMaximum()
	{
		session.launch(WOLVES);
		int wolf = named("Wolf 1").id();
		int before = named("Wolf 1").health().orElseThrow().maximum();

		session.retouch(wolf, WOLF.withTemplate("Advanced Creature"));

		assertTrue(named("Wolf 1").health().orElseThrow().maximum() > before);
	}

	@Test
	void anAdvancedWolfIsWorthMoreExperience()
	{
		session.launch(WOLVES);

		session.retouch(named("Wolf 1").id(), WOLF.withTemplate("Advanced Creature"));

		assertEquals(1_000, session.combat().experience());
	}

	@Test
	void shakenLowersAMonstersSheet()
	{
		session.launch(WOLVES);
		int wolf = named("Wolf 1").id();
		String bite = session.sheet(wolf).orElseThrow().attacks().getFirst().toHit();

		session.addCondition(wolf, new Condition("Shaken", Optional.of(new BonusSourceKey(BonusSourceKey.TEMPLATE, "",
			"Shaken")), OptionalInt.of(1)));

		assertEquals(Integer.parseInt(bite.replace("+", "")) - 2,
			Integer.parseInt(session.sheet(wolf).orElseThrow().attacks().getFirst().toHit().replace("+", "")));
	}

	@Test
	void aPlayersConjuredCreatureCarriesNoHitPoints()
	{
		session.launch(WOLVES);

		session.summon(named("Seoni").id(), "Hound Archon", Optional.empty(), 1, OptionalInt.of(3));

		assertEquals(CombatantKind.PLAYER_SUMMON, named("Hound Archon").kind());
	}

	@Test
	void aMonstersConjuredCreatureIsBuilt()
	{
		session.launch(WOLVES);

		session.summon(named("Wolf 1").id(), "Lantern Archon", Optional.of(MonsterRecipe.of(
			new MonsterSource.BestiaryRace("Archon (Lantern)"))), 1, OptionalInt.of(3));

		assertTrue(session.sheet(named("Lantern Archon").id()).isPresent());
	}

	@Test
	void endingTheFightSharesTheWolvesExperience()
	{
		session.launch(WOLVES);

		CombatReward reward = session.endCombat();

		assertEquals(new CombatReward(800, Optional.of("3"), 2, 400), reward);
	}

	@Test
	void aMonsterSavedAsACharacterIsRebuiltFromItsFile() throws IOException
	{
		session.launch(WOLVES);
		int wolf = named("Wolf 1").id();
		session.retouch(wolf, WOLF.withTemplate("Advanced Creature"));
		int maximum = named("Wolf 1").health().orElseThrow().maximum();
		File saved = Files.createTempFile("wolf", ".pcg").toFile();
		saved.deleteOnExit();

		session.saveMonster(wolf, saved, SettingsHandler.getGameAsProperty().get(),
			List.copyOf(BestiarySources.campaigns()));
		session.refreshFromFiles();

		assertEquals(List.of(new MonsterSource.CharacterFile(saved.getAbsolutePath()), maximum),
			List.of(named("Wolf 1").recipe().orElseThrow().source(), named("Wolf 1").health().orElseThrow().maximum()));
	}

	@Test
	void aPlayersFileGivesAGlance() throws IOException
	{
		File seoni = Files.createTempFile("seoni", ".pcg").toFile();
		seoni.deleteOnExit();
		new PCGIOHandler().write(BestiarySources.newCharacter("Seoni"), SettingsHandler.getGameAsProperty().get(),
			List.copyOf(BestiarySources.campaigns()), seoni);
		session.setParty(List.of(new PartyMember("Seoni", Optional.of(seoni.getAbsolutePath()))));
		session.launch(WOLVES);

		Optional<PlayerGlance> glance = session.glance(named("Seoni").id());

		assertTrue(glance.isPresent());
	}

	@Test
	void aSavedFightReopensWithItsMonstersBuiltAndHurt() throws IOException
	{
		session.launch(WOLVES);
		int wolf = named("Wolf 1").id();
		session.damage(wolf, 4);
		File saved = Files.createTempFile("wolves", "." + Scenario.FILE_EXTENSION).toFile();
		saved.deleteOnExit();
		session.save(saved);
		GameTableSession reopened = new GameTableSession(() -> 10);
		reopened.useSources(new MonsterForge(BestiarySources.campaigns(), new HeadlessUIDelegate()), "Pathfinder_RPG",
			List.of());
		ScenarioReading reading = reopened.read(saved);
		assertInstanceOf(ScenarioReading.Read.class, reading);

		reopened.open(((ScenarioReading.Read) reading).scenario(), saved);

		assertEquals(List.of(true, 4), List.of(reopened.sheet(wolf).isPresent(),
			reopened.combat().combatant(wolf).orElseThrow().health().orElseThrow().damage()));
	}

	private static final Condition FOUR_STRENGTH_DAMAGE = new Condition("Strength damage", Optional.of(
		new BonusSourceKey(BonusSourceKey.TEMPLATE, "", "Ability Damaged (Strength)")), OptionalInt.empty(),
		OptionalInt.of(4));

	private int biteOf(String name)
	{
		return Integer.parseInt(session.sheet(named(name).id()).orElseThrow().attacks().getFirst().toHit()
			.replace("+", ""));
	}

	@Test
	void aConditionLaidOnAGroupLowersEveryWolfByItsChosenValue()
	{
		session.launch(WOLVES);
		List<Integer> before = List.of(biteOf("Wolf 1"), biteOf("Wolf 2"));

		session.addCondition(List.of(named("Wolf 1").id(), named("Wolf 2").id()), FOUR_STRENGTH_DAMAGE);

		assertEquals(List.of(before.get(0) - 2, before.get(1) - 2), List.of(biteOf("Wolf 1"), biteOf("Wolf 2")));
	}

	@Test
	void aConditionLaidOnAGroupIsOnlyALabelOnAPlayer()
	{
		session.launch(WOLVES);

		session.addCondition(List.of(named("Wolf 1").id(), named("Seoni").id()), FOUR_STRENGTH_DAMAGE);

		assertEquals(List.of(FOUR_STRENGTH_DAMAGE.asLabel()), named("Seoni").conditions());
	}

	@Test
	void severalCombatantsAreOfferedTheBonusesCastOnOthersWithTheirValues()
	{
		session.launch(WOLVES);

		List<BonusChoice> choices = session.conditionChoices(List.of(named("Wolf 1").id(), named("Wolf 2").id()));

		assertTrue(choices.stream().anyMatch(choice -> choice.range().isPresent()), choices.toString());
	}

	/** Wolf 2 lays four points of Strength damage on Wolf 1 for one round, the fight started. */
	private int wolfCursingItsBrother()
	{
		session.launch(WOLVES);
		session.start();
		int before = biteOf("Wolf 1");
		session.castSpell(named("Wolf 2").id(), new Condition("Strength damage", FOUR_STRENGTH_DAMAGE.bonus(),
			OptionalInt.of(1), OptionalInt.of(4)), List.of(named("Wolf 1").id()));
		return before;
	}

	@Test
	void aSpellsEffectChangesTheNumbersOfTheMonsterItTargets()
	{
		int before = wolfCursingItsBrother();

		assertEquals(before - 2, biteOf("Wolf 1"));
	}

	@Test
	void aMonsterGetsItsNumbersBackWhenTheSpellOnItEnds()
	{
		int before = wolfCursingItsBrother();
		for (int turn = 0; turn < 10 && !named("Wolf 2").spells().isEmpty(); turn++)
		{
			session.next();
		}

		assertEquals(before, biteOf("Wolf 1"));
	}

	private void slay(String name)
	{
		session.damage(named(name).id(), 1_000);
	}

	private LootBlock lastLoot()
	{
		return session.scenario().loot().getLast();
	}

	@Test
	void endingAFightKeepsItsLoot()
	{
		session.launch(WOLVES);

		session.endCombat();

		assertEquals(List.of("Wolves in the woods"), session.scenario().loot().stream().map(LootBlock::title).toList());
	}

	@Test
	void aSlainMonstersLinesAreChecked()
	{
		session.addMonsters("Contract Killer", CONTRACT_KILLER, 1);
		slay("Contract Killer");

		session.endCombat();

		assertTrue(!lastLoot().lines().isEmpty() && lastLoot().lines().stream().allMatch(LootLine::selected));
	}

	@Test
	void aLivingMonstersLinesAreUnchecked()
	{
		session.addMonsters("Contract Killer", CONTRACT_KILLER, 1);

		session.endCombat();

		assertTrue(!lastLoot().lines().isEmpty() && lastLoot().lines().stream().noneMatch(LootLine::selected));
	}

	@Test
	void aSlainTreantLeavesDrawnTreasure()
	{
		session.addMonsters("Treant", TREANT, 1);
		slay("Treant");

		session.endCombat();

		assertTrue(lastLoot().lines().stream().allMatch(line -> line.drawnAs().equals(Optional.of(TreasureType.STANDARD))
			&& line.carrier().equals("Treant")) && !lastLoot().lines().isEmpty());
	}

	@Test
	void aConjuredCreatureLeavesNoLoot()
	{
		session.launch(WOLVES);
		session.summon(named("Wolf 1").id(), "Treant", Optional.of(TREANT), 1, OptionalInt.of(3));

		session.endCombat();

		assertEquals(List.of(), lastLoot().lines());
	}

	@Test
	void aFightWithoutEncounterHasADefaultTitle()
	{
		session.addMonsters("Wolf", WOLF, 1);

		session.endCombat();

		assertEquals(LanguageBundle.getString("in_gametable_lootNoEncounter"), lastLoot().title());
	}

	@Test
	void twoFightsMakeTwoBlocks()
	{
		session.launch(WOLVES);
		session.endCombat();
		session.launch(WOLVES);

		session.endCombat();

		assertEquals(List.of(1, 2), session.scenario().loot().stream().map(LootBlock::id).toList());
	}

	@Test
	void aRerollKeepsTheCarriedLinesBoxes()
	{
		session.addMonsters("Contract Killer", CONTRACT_KILLER, 1);
		session.addMonsters("Treant", TREANT, 1);
		slay("Contract Killer");
		slay("Treant");
		session.endCombat();
		session.selectLoot(lastLoot().id(), 0, false);

		session.rerollLoot(lastLoot().id());

		assertEquals(false, lastLoot().lines().getFirst().selected());
	}

	@Test
	void removingABlockForgetsIt()
	{
		session.launch(WOLVES);
		session.endCombat();

		session.removeLoot(lastLoot().id());

		assertEquals(List.of(), session.scenario().loot());
	}

	@Test
	void theLootSurvivesSavingAndReopening() throws IOException
	{
		session.addMonsters("Treant", TREANT, 1);
		slay("Treant");
		session.endCombat();
		File saved = Files.createTempFile("loot", "." + Scenario.FILE_EXTENSION).toFile();
		saved.deleteOnExit();
		session.save(saved);

		ScenarioReading reading = session.read(saved);

		assertEquals(session.scenario().loot(), ((ScenarioReading.Read) reading).scenario().loot());
	}
}
