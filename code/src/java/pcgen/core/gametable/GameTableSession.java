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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.IntSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pcgen.cdom.enumeration.ObjectKey;
import pcgen.core.Campaign;
import pcgen.core.GameMode;
import pcgen.core.PCTemplate;
import pcgen.core.PlayerCharacter;
import pcgen.core.conjurations.BonusSourceKey;
import pcgen.core.conjurations.CreatureStats;
import pcgen.core.conjurations.SummonedCreatureBuilder;
import pcgen.io.PCGIOHandler;
import pcgen.system.LanguageBundle;
import pcgen.util.enumeration.Visibility;

/**
 * The scenario the game master has open, and the monsters of its fight as PCGen
 * computes them. The game table's screens act through it.
 */
public final class GameTableSession
{
	private final IntSupplier d20;
	private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
	private final Map<Integer, PlayerCharacter> monsters = new HashMap<>();
	private final Map<Integer, Set<BonusSourceKey>> laid = new HashMap<>();
	private final Map<String, Optional<PlayerCharacter>> playerCharacters = new HashMap<>();
	private final Map<MonsterRecipe, Long> experienceByRecipe = new HashMap<>();

	private Optional<MonsterForge> forge = Optional.empty();
	private Optional<CombatantSheets> sheets = Optional.empty();
	private Scenario scenario = Scenario.empty();
	private Optional<File> file = Optional.empty();
	private boolean dirty;
	private List<String> problems = List.of();

	/**
	 * @param d20 rolls a twenty-sided die, for the monsters' initiative.
	 */
	public GameTableSession(IntSupplier d20)
	{
		this.d20 = d20;
	}

	/**
	 * @param listener called after every change.
	 */
	public void addListener(Runnable listener)
	{
		listeners.add(listener);
	}

	/**
	 * @param newForge  builds monsters on the sources now loaded.
	 * @param gameMode  the game mode of those sources.
	 * @param campaigns the keys of those sources.
	 * @return the monsters of the fight that cannot be built on them.
	 */
	public synchronized List<String> useSources(MonsterForge newForge, String gameMode, List<String> campaigns)
	{
		forge = Optional.of(newForge);
		sheets = Optional.of(new CombatantSheets(newForge.builder()));
		if (scenario.gameMode().isEmpty())
		{
			scenario = scenario.withSources(gameMode, campaigns);
		}
		rebuildAll();
		notifyListeners();
		return problems;
	}

	/**
	 * Forgets the monsters built on sources that are being unloaded.
	 */
	public synchronized void dropSources()
	{
		forge = Optional.empty();
		sheets = Optional.empty();
		forgetBuilt();
		notifyListeners();
	}

	public synchronized boolean hasSources()
	{
		return forge.isPresent();
	}

	public synchronized Scenario scenario()
	{
		return scenario;
	}

	public synchronized Combat combat()
	{
		return scenario.combat();
	}

	public synchronized Optional<File> file()
	{
		return file;
	}

	public synchronized boolean isDirty()
	{
		return dirty;
	}

	/**
	 * @return what could not be built the last time the monsters were rebuilt.
	 */
	public synchronized List<String> problems()
	{
		return problems;
	}

	/**
	 * Starts an empty scenario on the sources loaded now.
	 */
	public synchronized void newScenario(String gameMode, List<String> campaigns)
	{
		scenario = Scenario.empty().withSources(gameMode, campaigns);
		file = Optional.empty();
		forgetBuilt();
		problems = List.of();
		dirty = false;
		notifyListeners();
	}

	/**
	 * @param scenarioFile a scenario file.
	 * @return the scenario it holds, or why it cannot be read. The game table keeps the
	 *         scenario it had when the file cannot be read.
	 */
	public synchronized ScenarioReading read(File scenarioFile)
	{
		try
		{
			return ScenarioJson.read(Files.readString(scenarioFile.toPath(), StandardCharsets.UTF_8));
		}
		catch (IOException unreadable)
		{
			return new ScenarioReading.Unreadable(unreadable.getMessage());
		}
	}

	/**
	 * @param opened       a scenario read from a file.
	 * @param scenarioFile the file it was read from.
	 * @return the monsters of its fight that cannot be built.
	 */
	public synchronized List<String> open(Scenario opened, File scenarioFile)
	{
		scenario = opened;
		file = Optional.of(scenarioFile);
		dirty = false;
		forgetBuilt();
		rebuildAll();
		notifyListeners();
		return problems;
	}

	/**
	 * @param scenarioFile where to save the scenario.
	 * @return why it could not be saved; empty once saved.
	 */
	public synchronized Optional<String> save(File scenarioFile)
	{
		try
		{
			Files.writeString(scenarioFile.toPath(), ScenarioJson.write(scenario), StandardCharsets.UTF_8);
			file = Optional.of(scenarioFile);
			dirty = false;
			notifyListeners();
			return Optional.empty();
		}
		catch (IOException unwritable)
		{
			return Optional.of(unwritable.getMessage());
		}
	}

	public synchronized void setParty(List<PartyMember> party)
	{
		scenario = scenario.withParty(party);
		changed();
	}

	public synchronized void setEncounters(List<Encounter> encounters)
	{
		scenario = scenario.withEncounters(encounters);
		changed();
	}

	/**
	 * @param monster monsters of an encounter.
	 * @return what they are worth together; nothing for one that cannot be built.
	 */
	public synchronized long experienceOf(EncounterMonster monster)
	{
		return experienceOf(monster.recipe()) * monster.count();
	}

	/**
	 * @param monster monsters of an encounter.
	 * @return the challenge rating of one, or why it cannot be built.
	 */
	public synchronized ForgeOutcome<String> challengeRatingOf(EncounterMonster monster)
	{
		ForgedMonster forged = forge(monster.recipe());
		return switch (forged)
		{
			case ForgedMonster.Forged built -> new ForgeOutcome.Built<>(sheets().challengeRating(built.character()));
			case ForgedMonster.Missing missing -> new ForgeOutcome.Failed<>(missing.reason());
		};
	}

	/**
	 * @param recipe how to build a monster.
	 * @return its numbers and challenge rating, or why it cannot be built.
	 */
	public synchronized ForgeOutcome<MonsterPreview> preview(MonsterRecipe recipe)
	{
		return switch (forge(recipe))
		{
			case ForgedMonster.Forged built -> new ForgeOutcome.Built<>(new MonsterPreview(
				sheets().stats(built.character()), sheets().challengeRating(built.character())));
			case ForgedMonster.Missing missing -> new ForgeOutcome.Failed<>(missing.reason());
		};
	}

	/**
	 * @param recipe how to build a monster.
	 * @return the keys of the templates the built monster has, or why it cannot be built.
	 */
	public synchronized ForgeOutcome<List<String>> templateKeysOf(MonsterRecipe recipe)
	{
		return switch (forge(recipe))
		{
			case ForgedMonster.Forged built -> new ForgeOutcome.Built<>(built.character().getDisplay().getTemplateSet()
				.stream().filter(template -> template.getSafe(ObjectKey.VISIBILITY) != Visibility.HIDDEN)
				.map(PCTemplate::getKeyName).sorted().toList());
			case ForgedMonster.Missing missing -> new ForgeOutcome.Failed<>(missing.reason());
		};
	}

	/**
	 * @param encounter a prepared encounter.
	 * @return what could not be built. The party and the monsters enter the fight, the
	 *         monsters with their initiative rolled.
	 */
	public synchronized List<String> launch(Encounter encounter)
	{
		Combat combat = scenario.combat();
		for (PartyMember member : scenario.party())
		{
			boolean present = combat.order().stream().anyMatch(
				combatant -> combatant.kind() == CombatantKind.PLAYER && combatant.name().equals(member.name()));
			if (!present)
			{
				combat = combat.with(Combatant.player(combat.nextId(), member.name(), member.characterFile()));
			}
		}
		scenario = scenario.withCombat(combat);
		List<String> failures = new ArrayList<>();
		for (EncounterMonster monster : encounter.monsters())
		{
			failures.addAll(enter(monster.name(), monster.recipe(), monster.count()));
		}
		scenario = scenario.withCombat(scenario.combat().rollingMissing(d20));
		changed();
		return failures;
	}

	/**
	 * @param name   the name of one.
	 * @param recipe how to build each.
	 * @param count  how many enter.
	 * @return what could not be built.
	 */
	public synchronized List<String> addMonsters(String name, MonsterRecipe recipe, int count)
	{
		List<String> failures = enter(name, recipe, count);
		scenario = scenario.withCombat(scenario.combat().rollingMissing(d20));
		changed();
		return failures;
	}

	/**
	 * @param name          the character's name.
	 * @param characterFile its saved character, if the game master has a copy.
	 */
	public synchronized void addPlayer(String name, Optional<String> characterFile)
	{
		Combat combat = scenario.combat();
		scenario = scenario.withCombat(combat.with(Combatant.player(combat.nextId(), name, characterFile)));
		changed();
	}

	/**
	 * @param summonerId the conjurer's id.
	 * @param name       the name of one creature.
	 * @param recipe     how to build each, for a monster's creatures; empty for a
	 *                   player's, whose player tracks them.
	 * @param count      how many arrive.
	 * @param rounds     rounds before they leave; empty when they stay.
	 * @return what could not be built.
	 */
	public synchronized List<String> summon(int summonerId, String name, Optional<MonsterRecipe> recipe, int count,
		OptionalInt rounds)
	{
		Combat combat = scenario.combat();
		List<String> names = numberedNames(combat, name, count);
		int id = combat.nextId();
		List<Combatant> creatures = new ArrayList<>();
		List<String> failures = new ArrayList<>();
		Optional<MonsterRecipe> augmented = recipe.map(found -> augmentedBy(summonerId, found));
		for (String creatureName : names)
		{
			if (augmented.isEmpty())
			{
				creatures.add(Combatant.player(id++, creatureName, Optional.empty()).withRoundsLeft(rounds));
				continue;
			}
			ForgedMonster forged = forge(augmented.get());
			if (forged instanceof ForgedMonster.Forged built)
			{
				monsters.put(id, built.character());
				creatures.add(monsterOf(id++, creatureName, "", augmented.get(), built.character()).withRoundsLeft(rounds));
			}
			else if (forged instanceof ForgedMonster.Missing missing)
			{
				failures.add(creatureName + " : " + missing.reason());
			}
		}
		scenario = scenario.withCombat(combat.summoned(summonerId, creatures));
		changed();
		return failures;
	}

	/**
	 * @param id     a monster's id.
	 * @param recipe the monster with the game master's touch.
	 * @return why the touch cannot be made; empty once the monster is rebuilt, its damage kept.
	 */
	public synchronized Optional<String> retouch(int id, MonsterRecipe recipe)
	{
		Optional<Combatant> found = scenario.combat().combatant(id);
		if (found.isEmpty() || found.get().recipe().isEmpty())
		{
			return Optional.of(LanguageBundle.getString("in_gametable_notAMonster"));
		}
		ForgedMonster forged = forge(recipe);
		if (forged instanceof ForgedMonster.Missing missing)
		{
			return Optional.of(missing.reason());
		}
		PlayerCharacter rebuilt = ((ForgedMonster.Forged) forged).character();
		long worthBefore = Optional.ofNullable(monsters.get(id)).map(this::worth).orElse(0L);
		monsters.put(id, rebuilt);
		laid.put(id, sheets().lay(rebuilt, Set.of(), found.get().conditions()));
		CreatureStats stats = sheets().stats(rebuilt);
		Combatant touched = found.get().withRecipe(recipe, stats.maximumHitPoints(), sheets().initiativeModifier(rebuilt),
			sheets().hasDualInitiative(rebuilt));
		Combat combat = scenario.combat().replaced(touched);
		if (touched.kind() == CombatantKind.MONSTER)
		{
			// A conjured creature is worth nothing; a touched monster is worth its new challenge rating
			combat = combat.withExperience(worth(rebuilt) - worthBefore);
		}
		scenario = scenario.withCombat(combat);
		changed();
		return Optional.empty();
	}

	private long worth(PlayerCharacter monster)
	{
		return Experience.experienceOf(sheets().challengeRating(monster));
	}

	/**
	 * Rebuilds the monsters built from character files, after the game master changed
	 * those files in the Characters workspace.
	 *
	 * @return what could not be rebuilt.
	 */
	public synchronized List<String> refreshFromFiles()
	{
		List<String> failures = new ArrayList<>();
		for (Combatant combatant : scenario.combat().order())
		{
			if (combatant.recipe().isPresent()
				&& combatant.recipe().get().source() instanceof MonsterSource.CharacterFile)
			{
				retouch(combatant.id(), combatant.recipe().get()).ifPresent(
					reason -> failures.add(combatant.name() + " : " + reason));
			}
		}
		playerCharacters.clear();
		notifyListeners();
		return failures;
	}

	public synchronized void setInitiative(int id, int score)
	{
		scenario = scenario.withCombat(scenario.combat().withInitiative(id, score));
		changed();
	}

	public synchronized void start()
	{
		scenario = scenario.withCombat(scenario.combat().rollingMissing(d20).started());
		changed();
	}

	/**
	 * @return what a new round wore off, if the turn passed into one.
	 */
	public synchronized List<Expiry> next()
	{
		CombatAdvance advance = scenario.combat().next();
		scenario = scenario.withCombat(advance.combat());
		if (!advance.expired().isEmpty())
		{
			monsters.keySet().removeIf(id -> advance.combat().combatant(id).isEmpty());
			laid.keySet().removeIf(id -> advance.combat().combatant(id).isEmpty());
			advance.combat().order().forEach(combatant -> relay(combatant.id()));
		}
		changed();
		return advance.expired();
	}

	public synchronized void damage(int id, int amount)
	{
		scenario = scenario.withCombat(scenario.combat().damaged(id, amount));
		changed();
	}

	public synchronized void heal(int id, int amount)
	{
		scenario = scenario.withCombat(scenario.combat().healed(id, amount));
		changed();
	}

	public synchronized void addCondition(int id, Condition condition)
	{
		scenario = scenario.withCombat(scenario.combat().withCondition(id, condition));
		relay(id);
		changed();
	}

	public synchronized void removeCondition(int id, String conditionName)
	{
		scenario = scenario.withCombat(scenario.combat().withoutCondition(id, conditionName));
		relay(id);
		changed();
	}

	public synchronized void delay(int id)
	{
		scenario = scenario.withCombat(scenario.combat().delaying(id));
		changed();
	}

	public synchronized void resume(int id)
	{
		scenario = scenario.withCombat(scenario.combat().resuming(id));
		changed();
	}

	public synchronized void move(int id, int index)
	{
		scenario = scenario.withCombat(scenario.combat().moved(id, index));
		changed();
	}

	public synchronized void remove(int id)
	{
		scenario = scenario.withCombat(scenario.combat().removed(id));
		monsters.remove(id);
		laid.remove(id);
		changed();
	}

	/**
	 * @return what the fight is worth to the players; the fight is over and cleared.
	 */
	public synchronized CombatReward endCombat()
	{
		Combat combat = scenario.combat();
		long players = scenario.party().isEmpty()
			? combat.order().stream().filter(combatant -> combatant.kind() == CombatantKind.PLAYER).count()
			: scenario.party().size();
		CombatReward reward = CombatReward.of(combat.experience(), (int) players);
		scenario = scenario.withCombat(Combat.empty());
		forgetBuilt();
		changed();
		return reward;
	}

	/**
	 * @param id a combatant's id.
	 * @return the numbers of the monster, when the game master's and built.
	 */
	public synchronized Optional<CreatureStats> sheet(int id)
	{
		return Optional.ofNullable(monsters.get(id)).flatMap(monster -> sheets.map(found -> found.stats(monster)));
	}

	/**
	 * @param id a combatant's id.
	 * @return the monster PCGen computed, when the game master's and built.
	 */
	public synchronized Optional<PlayerCharacter> monster(int id)
	{
		return Optional.ofNullable(monsters.get(id));
	}

	/**
	 * @param id a player's character's id.
	 * @return the few numbers the game master reads of it, when its file is known.
	 */
	public synchronized Optional<PlayerGlance> glance(int id)
	{
		Optional<String> characterFile = scenario.combat().combatant(id).flatMap(Combatant::characterFile);
		if (characterFile.isEmpty() || forge.isEmpty())
		{
			return Optional.empty();
		}
		Optional<PlayerCharacter> character = playerCharacters.computeIfAbsent(characterFile.get(), path -> {
			ForgedMonster read = forge.get().forge(MonsterRecipe.of(new MonsterSource.CharacterFile(path)));
			return (read instanceof ForgedMonster.Forged built) ? Optional.of(built.character()) : Optional.empty();
		});
		return character.map(found -> sheets().glance(found));
	}

	/**
	 * @param id a combatant's id.
	 * @return the conditions and bonuses the data holds, by name, for that combatant.
	 */
	public synchronized Map<String, BonusSourceKey> conditionSources(int id)
	{
		if (forge.isEmpty())
		{
			return Map.of();
		}
		return CombatantSheets.conditionSources(Optional.ofNullable(monsters.get(id)));
	}

	/**
	 * Saves a monster as a character the Characters workspace can open, without the
	 * conditions of the fight, and rebuilds it from that file from now on.
	 *
	 * @param id        a monster's id.
	 * @param target    the character file to write.
	 * @param gameMode  the game mode of the loaded sources.
	 * @param campaigns the loaded sources.
	 * @return why it could not be saved; empty once saved.
	 */
	public synchronized Optional<String> saveMonster(int id, File target, GameMode gameMode, List<Campaign> campaigns)
	{
		PlayerCharacter monster = monsters.get(id);
		if (monster == null || sheets.isEmpty())
		{
			return Optional.of(LanguageBundle.getString("in_gametable_notAMonster"));
		}
		Set<BonusSourceKey> without = sheets.get().lay(monster, laid.getOrDefault(id, Set.of()), List.of());
		try
		{
			new PCGIOHandler().write(monster, gameMode, campaigns, target);
		}
		finally
		{
			laid.put(id, sheets.get().lay(monster, without,
				scenario.combat().combatant(id).map(Combatant::conditions).orElse(List.of())));
		}
		relink(id, target.getAbsolutePath());
		return Optional.empty();
	}

	/**
	 * @param id   a monster's id.
	 * @param path a character file now holding the monster as it is.
	 */
	public synchronized void relink(int id, String path)
	{
		scenario.combat().combatant(id).ifPresent(combatant -> {
			if (combatant.recipe().isPresent())
			{
				Combatant relinked = combatant.withRecipe(MonsterRecipe.of(new MonsterSource.CharacterFile(path)),
					combatant.health().orElseThrow().maximum(), combatant.initiative().modifier(),
					combatant.initiative().dual());
				scenario = scenario.withCombat(scenario.combat().replaced(relinked));
				changed();
			}
		});
	}

	private List<String> enter(String name, MonsterRecipe recipe, int count)
	{
		Combat combat = scenario.combat();
		List<String> failures = new ArrayList<>();
		String group = "troop:" + combat.nextId();
		long experience = 0;
		for (String monsterName : numberedNames(combat, name, count))
		{
			ForgedMonster forged = forge(recipe);
			if (forged instanceof ForgedMonster.Missing missing)
			{
				failures.add(monsterName + " : " + missing.reason());
				continue;
			}
			PlayerCharacter built = ((ForgedMonster.Forged) forged).character();
			int id = combat.nextId();
			monsters.put(id, built);
			laid.put(id, Set.of());
			combat = combat.with(monsterOf(id, monsterName, group, recipe, built));
			experience += Experience.experienceOf(sheets().challengeRating(built));
		}
		scenario = scenario.withCombat(combat.withExperience(experience));
		return failures;
	}

	private Combatant monsterOf(int id, String name, String group, MonsterRecipe recipe, PlayerCharacter built)
	{
		return Combatant.monster(id, name, group, recipe, sheets().stats(built).maximumHitPoints(),
			sheets().initiativeModifier(built), sheets().hasDualInitiative(built));
	}

	private MonsterRecipe augmentedBy(int summonerId, MonsterRecipe recipe)
	{
		PlayerCharacter summoner = monsters.get(summonerId);
		if (summoner != null && SummonedCreatureBuilder.augments(summoner))
		{
			return recipe.withTemplate(SummonedCreatureBuilder.AUGMENT_SUMMONING);
		}
		return recipe;
	}

	private long experienceOf(MonsterRecipe recipe)
	{
		Long known = experienceByRecipe.get(recipe);
		if (known != null)
		{
			return known;
		}
		ForgedMonster forged = forge(recipe);
		long experience = (forged instanceof ForgedMonster.Forged built)
			? Experience.experienceOf(sheets().challengeRating(built.character())) : 0;
		experienceByRecipe.put(recipe, experience);
		return experience;
	}

	private ForgedMonster forge(MonsterRecipe recipe)
	{
		return forge.map(found -> found.forge(recipe))
			.orElseGet(() -> new ForgedMonster.Missing(LanguageBundle.getString("in_gametable_noSources")));
	}

	private CombatantSheets sheets()
	{
		return sheets.orElseThrow(() -> new IllegalStateException("No sources are loaded"));
	}

	private void relay(int id)
	{
		PlayerCharacter monster = monsters.get(id);
		Optional<Combatant> combatant = scenario.combat().combatant(id);
		if (monster != null && combatant.isPresent() && sheets.isPresent())
		{
			laid.put(id, sheets.get().lay(monster, laid.getOrDefault(id, Set.of()), combatant.get().conditions()));
		}
	}

	private void rebuildAll()
	{
		forgetBuilt();
		List<String> failures = new ArrayList<>();
		if (forge.isPresent())
		{
			for (Combatant combatant : scenario.combat().order())
			{
				if (combatant.recipe().isEmpty())
				{
					continue;
				}
				ForgedMonster forged = forge(combatant.recipe().get());
				switch (forged)
				{
					case ForgedMonster.Forged built ->
					{
						monsters.put(combatant.id(), built.character());
						laid.put(combatant.id(), sheets().lay(built.character(), Set.of(), combatant.conditions()));
					}
					case ForgedMonster.Missing missing -> failures.add(combatant.name() + " : " + missing.reason());
				}
			}
		}
		problems = List.copyOf(failures);
	}

	private void forgetBuilt()
	{
		monsters.clear();
		laid.clear();
		playerCharacters.clear();
		experienceByRecipe.clear();
	}

	private void changed()
	{
		dirty = true;
		notifyListeners();
	}

	private void notifyListeners()
	{
		listeners.forEach(Runnable::run);
	}

	/**
	 * @param combat the fight the newcomers enter.
	 * @param name   the name of one.
	 * @param count  how many arrive.
	 * @return their names: the name itself for a single newcomer whose name is free,
	 *         numbered after those already in the fight otherwise.
	 */
	static List<String> numberedNames(Combat combat, String name, int count)
	{
		String base = name.strip();
		Pattern numbered = Pattern.compile(Pattern.quote(base) + " (\\d+)");
		int highest = 0;
		boolean taken = false;
		for (Combatant combatant : combat.order())
		{
			if (combatant.name().equals(base))
			{
				taken = true;
				highest = Math.max(highest, 1);
			}
			Matcher matcher = numbered.matcher(combatant.name());
			if (matcher.matches())
			{
				taken = true;
				highest = Math.max(highest, Integer.parseInt(matcher.group(1)));
			}
		}
		if (count == 1 && !taken)
		{
			return List.of(base);
		}
		List<String> names = new ArrayList<>();
		for (int number = highest + 1; number <= highest + count; number++)
		{
			names.add(base + " " + number);
		}
		return names;
	}
}
