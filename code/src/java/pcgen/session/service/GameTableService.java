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
package pcgen.session.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;
import java.util.stream.Stream;

import pcgen.core.Campaign;
import pcgen.core.bonus.IntRange;
import pcgen.core.conjurations.BonusSourceKey;
import pcgen.core.gametable.Combat;
import pcgen.core.gametable.CombatReward;
import pcgen.core.gametable.Combatant;
import pcgen.core.gametable.Condition;
import pcgen.core.gametable.Encounter;
import pcgen.core.gametable.EncounterMonster;
import pcgen.core.gametable.Experience;
import pcgen.core.gametable.Expiry;
import pcgen.core.gametable.ForgeOutcome;
import pcgen.core.gametable.GameTableSession;
import pcgen.core.gametable.HttpTabletopTransfer;
import pcgen.core.gametable.ImageCategory;
import pcgen.core.gametable.Initiative;
import pcgen.core.gametable.LootBlock;
import pcgen.core.gametable.MonsterForge;
import pcgen.core.gametable.MonsterPreview;
import pcgen.core.gametable.MonsterRecipe;
import pcgen.core.gametable.MonsterSource;
import pcgen.core.gametable.PartyMember;
import pcgen.core.gametable.Scenario;
import pcgen.core.gametable.ScenarioImage;
import pcgen.core.gametable.ScenarioReading;
import pcgen.core.gametable.TabletopOutcome;
import pcgen.core.gametable.TabletopTransfer;
import pcgen.core.gametable.Turn;
import pcgen.facade.core.BonusChoice;
import pcgen.facade.core.DataSetFacade;
import pcgen.facade.util.ListFacades;
import pcgen.session.HeadlessUIDelegate;
import pcgen.session.PcgenSession;
import pcgen.session.service.model.CastSpellEntry;
import pcgen.session.service.model.CombatEnded;
import pcgen.session.service.model.CombatEntry;
import pcgen.session.service.model.CombatantEntry;
import pcgen.session.service.model.CombatantSheet;
import pcgen.session.service.model.ConditionEntry;
import pcgen.session.service.model.EncounterEntry;
import pcgen.session.service.model.EncounterMonsterEntry;
import pcgen.session.service.model.GameTableState;
import pcgen.session.service.model.ImageContent;
import pcgen.session.service.model.ImageEntry;
import pcgen.session.service.model.LootBlockEntry;
import pcgen.session.service.model.LootLineEntry;
import pcgen.session.service.model.PartyMemberEntry;
import pcgen.session.service.model.RecipeEntry;
import pcgen.session.service.model.TabletopSent;

/**
 * The game table without its screens: the scenario a game master runs, its party,
 * its prepared encounters and the fight under way, with monsters built on the sources
 * the session has loaded.
 */
public final class GameTableService
{
	private static final String CARRIED = "carried";
	private static final String CATEGORY = "category";

	/** How a caller describes a monster: a race, a character file or an NPC kit, then the game master's touch. */
	public record RecipeRequest(String race, String characterFile, String kit, List<String> addedTemplates,
		List<String> removedTemplates, int addedHitDice)
	{
	}

	/** Monsters of an encounter, as a caller describes them. */
	public record EncounterMonsterRequest(String name, RecipeRequest recipe, int count)
	{
	}

	private final PcgenSession session;
	private final TabletopTransfer tabletop;
	private final GameTableSession table;
	private String sourcesBuiltOn = "";

	public GameTableService(PcgenSession session, GameTableSession table)
	{
		this(session, table, HttpTabletopTransfer.fromEnvironment());
	}

	/**
	 * @param tabletop sends the scenario's images to Tabletop Simulator.
	 */
	public GameTableService(PcgenSession session, GameTableSession table, TabletopTransfer tabletop)
	{
		this.session = session;
		this.table = table;
		this.tabletop = tabletop;
	}

	public ServiceResult<GameTableState> newScenario()
	{
		return withSources(() -> {
			DataSetFacade dataSet = session.getCurrentDataSet();
			table.newScenario(dataSet.getGameMode().getName(), campaignKeys(dataSet));
			return ServiceResult.success(state(List.of()));
		});
	}

	public ServiceResult<GameTableState> openScenario(String path)
	{
		File file = new File(path);
		ScenarioReading reading = table.read(file);
		if (reading instanceof ScenarioReading.Unreadable unreadable)
		{
			return ServiceResult.failure(new ServiceError.InvalidArgument("path", unreadable.reason()));
		}
		Scenario scenario = ((ScenarioReading.Read) reading).scenario();
		if (!loaded(scenario))
		{
			try
			{
				session.loadSources(scenario.gameMode(), scenario.campaigns());
			}
			catch (IllegalArgumentException missing)
			{
				return ServiceResult.failure(new ServiceError.DataFailure(missing.getMessage()));
			}
		}
		return withSources(() -> ServiceResult.success(state(table.open(scenario, file))));
	}

	public ServiceResult<GameTableState> saveScenario(String path)
	{
		Optional<File> target = path.isBlank() ? table.file() : Optional.of(new File(path));
		if (target.isEmpty())
		{
			return ServiceResult.failure(new ServiceError.InvalidArgument("path",
				"the scenario has never been saved, so it needs a path"));
		}
		return table.save(target.get()).<ServiceResult<GameTableState>>map(
			reason -> ServiceResult.failure(new ServiceError.DataFailure(reason)))
			.orElseGet(() -> ServiceResult.success(state(List.of())));
	}

	public ServiceResult<GameTableState> getGameTable()
	{
		return ServiceResult.success(state(table.problems()));
	}

	public ServiceResult<GameTableState> setParty(List<PartyMemberEntry> members)
	{
		table.setParty(members.stream()
			.map(member -> new PartyMember(member.name(), Optional.ofNullable(member.characterFile()))).toList());
		return ServiceResult.success(state(List.of()));
	}

	/**
	 * Writes an encounter, replacing the one of the same name.
	 */
	public ServiceResult<GameTableState> setEncounter(String name, String notes, List<EncounterMonsterRequest> monsters)
	{
		List<EncounterMonster> read = new ArrayList<>();
		for (EncounterMonsterRequest monster : monsters)
		{
			ServiceResult<MonsterRecipe> recipe = recipeOf(monster.recipe());
			if (recipe instanceof ServiceResult.Failure<MonsterRecipe> failure)
			{
				return ServiceResult.failure(failure.error());
			}
			if (monster.count() < 1)
			{
				return ServiceResult.failure(new ServiceError.InvalidArgument("count", "at least one monster enters"));
			}
			read.add(new EncounterMonster(monster.name(), ((ServiceResult.Success<MonsterRecipe>) recipe).value(),
				monster.count()));
		}
		Encounter written = Encounter.named(name).withNotes(notes).withMonsters(read);
		List<Encounter> encounters = new ArrayList<>(table.scenario().encounters());
		int at = indexOfEncounter(name);
		if (at < 0)
		{
			encounters.add(written);
		}
		else
		{
			encounters.set(at, written);
		}
		table.setEncounters(encounters);
		return ServiceResult.success(state(List.of()));
	}

	public ServiceResult<GameTableState> removeEncounter(String name)
	{
		int at = indexOfEncounter(name);
		if (at < 0)
		{
			return ServiceResult.failure(new ServiceError.EntryNotFound("Encounter", name));
		}
		List<Encounter> encounters = new ArrayList<>(table.scenario().encounters());
		encounters.remove(at);
		table.setEncounters(encounters);
		return ServiceResult.success(state(List.of()));
	}

	public ServiceResult<MonsterPreview> previewMonster(RecipeRequest request)
	{
		return withSources(() -> recipeOf(request).andThen(recipe -> switch (table.preview(recipe))
		{
			case ForgeOutcome.Built<MonsterPreview> built -> ServiceResult.success(built.value());
			case ForgeOutcome.Failed<MonsterPreview> failed ->
				ServiceResult.failure(new ServiceError.InvalidArgument("recipe", failed.reason()));
		}));
	}

	public ServiceResult<GameTableState> launchEncounter(String name)
	{
		int at = indexOfEncounter(name);
		if (at < 0)
		{
			return ServiceResult.failure(new ServiceError.EntryNotFound("Encounter", name));
		}
		return withSources(() -> ServiceResult.success(state(table.launch(table.scenario().encounters().get(at)))));
	}

	public ServiceResult<GameTableState> addMonsters(String name, RecipeRequest request, int count)
	{
		if (count < 1)
		{
			return ServiceResult.failure(new ServiceError.InvalidArgument("count", "at least one monster enters"));
		}
		return withSources(() -> recipeOf(request).map(recipe -> state(table.addMonsters(name, recipe, count))));
	}

	public ServiceResult<GameTableState> addPlayer(String name, String characterFile)
	{
		table.addPlayer(name, Optional.ofNullable(characterFile));
		return ServiceResult.success(state(List.of()));
	}

	/**
	 * @param request how to build each creature; null for a player's creatures, whose player tracks them.
	 * @param rounds  rounds before they leave; 0 when they stay.
	 */
	public ServiceResult<GameTableState> summon(int summonerId, String name, RecipeRequest request, int count,
		int rounds)
	{
		if (count < 1)
		{
			return ServiceResult.failure(new ServiceError.InvalidArgument("count", "at least one creature arrives"));
		}
		OptionalInt lasting = (rounds > 0) ? OptionalInt.of(rounds) : OptionalInt.empty();
		return combatant(summonerId).andThen(summoner -> {
			if (request == null)
			{
				return ServiceResult.success(state(table.summon(summonerId, name, Optional.empty(), count, lasting)));
			}
			return withSources(() -> recipeOf(request)
				.map(recipe -> state(table.summon(summonerId, name, Optional.of(recipe), count, lasting))));
		});
	}

	public ServiceResult<GameTableState> retouchMonster(int id, RecipeRequest request)
	{
		return monster(id).andThen(monster -> withSources(() -> recipeOf(request).andThen(
			recipe -> table.retouch(id, recipe).<ServiceResult<GameTableState>>map(
				reason -> ServiceResult.failure(new ServiceError.InvalidArgument("recipe", reason)))
				.orElseGet(() -> ServiceResult.success(state(List.of()))))));
	}

	public ServiceResult<GameTableState> setInitiative(int id, int score)
	{
		return combatant(id).map(found -> {
			table.setInitiative(id, score);
			return state(List.of());
		});
	}

	public ServiceResult<GameTableState> startCombat()
	{
		if (table.combat().order().isEmpty())
		{
			return ServiceResult.failure(new ServiceError.NotAllowed("No one is in the fight yet."));
		}
		table.start();
		return ServiceResult.success(state(List.of()));
	}

	public ServiceResult<GameTableState> nextTurn()
	{
		if (table.combat().order().isEmpty())
		{
			return ServiceResult.failure(new ServiceError.NotAllowed("No one is in the fight yet."));
		}
		return ServiceResult.success(state(table.next().stream().map(GameTableService::described).toList()));
	}

	public ServiceResult<GameTableState> damage(int id, int amount)
	{
		return trackedHitPoints(id, amount).map(found -> {
			table.damage(id, amount);
			return state(List.of());
		});
	}

	public ServiceResult<GameTableState> heal(int id, int amount)
	{
		return trackedHitPoints(id, amount).map(found -> {
			table.heal(id, amount);
			return state(List.of());
		});
	}

	/**
	 * @param ids    the combatants it goes on at once, such as a band of monsters.
	 * @param name   a condition, temporary bonus or negative property the data holds, by its name or
	 *               key, which changes a monster's numbers; any other name, and anything on a player, is a label.
	 * @param rounds rounds before it ends; 0 when it lasts.
	 * @param value  the value a bonus asking one takes, such as the +2 of an Inspire Courage; its
	 *               highest when omitted.
	 */
	public ServiceResult<GameTableState> addCondition(List<Integer> ids, String name, int rounds, OptionalInt value)
	{
		if (ids.isEmpty())
		{
			return ServiceResult.failure(new ServiceError.InvalidArgument("combatant_ids", "names no combatant"));
		}
		return everyCombatant(ids).andThen(found -> conditionNamed(ids, name, rounds, value)).map(condition -> {
			table.addCondition(ids, condition);
			return state(List.of());
		});
	}

	/**
	 * @param ids  combatants of the fight.
	 * @param name the condition's name as get_game_table shows it, or the key of the data's; it comes off
	 *             those bearing it.
	 */
	public ServiceResult<GameTableState> removeCondition(List<Integer> ids, String name)
	{
		return everyCombatant(ids).andThen(found -> {
			String shown = shownName(ids, name);
			List<Integer> bearing = found.stream().filter(combatant -> combatant.conditions().stream()
				.anyMatch(condition -> condition.name().equalsIgnoreCase(shown))).map(Combatant::id).toList();
			if (bearing.isEmpty())
			{
				return ServiceResult.failure(new ServiceError.EntryNotFound("Condition on the combatants", name));
			}
			table.removeCondition(bearing, shown);
			return ServiceResult.success(state(List.of()));
		});
	}

	/**
	 * @param casterId  the caster's id.
	 * @param name      the spell; when the data holds a temporary bonus of that name or key, its effect
	 *                  changes the numbers of the monsters it targets, a label otherwise.
	 * @param rounds    turns of its caster before it ends, counted down as each begins; 0 until dropped.
	 * @param targetIds the combatants bearing its effect until it ends; none to only keep track of it.
	 * @param value     the value its bonus takes when it asks one; its highest when omitted.
	 */
	public ServiceResult<GameTableState> castSpell(int casterId, String name, int rounds, List<Integer> targetIds,
		OptionalInt value)
	{
		return combatant(casterId).andThen(caster -> everyCombatant(targetIds))
			.andThen(targets -> conditionNamed(targetIds, name, rounds, value)).map(effect -> {
				table.castSpell(casterId, effect, targetIds);
				return state(List.of());
			});
	}

	/**
	 * @param casterId the caster's id.
	 * @param name     a spell it keeps up, as get_game_table shows it or by the key of the data's bonus;
	 *                 its effect leaves its targets.
	 */
	public ServiceResult<GameTableState> dropSpell(int casterId, String name)
	{
		return combatant(casterId).andThen(caster -> {
			List<Integer> targets = caster.spells().stream().flatMap(spell -> spell.targetIds().stream()).toList();
			String shown = shownName(targets, name);
			boolean kept = caster.spells().stream().anyMatch(spell -> spell.name().equalsIgnoreCase(shown));
			if (!kept)
			{
				return ServiceResult.failure(new ServiceError.EntryNotFound("Spell cast by " + caster.name(), name));
			}
			table.dropSpell(casterId, shown);
			return ServiceResult.success(state(List.of()));
		});
	}

	private ServiceResult<List<Combatant>> everyCombatant(List<Integer> ids)
	{
		List<Combatant> found = new ArrayList<>();
		for (int id : ids)
		{
			Optional<Combatant> combatant = table.combat().combatant(id);
			if (combatant.isEmpty())
			{
				return ServiceResult.failure(new ServiceError.EntryNotFound("Combatant", String.valueOf(id)));
			}
			found.add(combatant.get());
		}
		return ServiceResult.success(found);
	}

	/** The name a condition or spell is shown under: the label of the data's bonus of that name or key, else the name. */
	private String shownName(List<Integer> ids, String name)
	{
		return table.conditionChoices(ids).stream().filter(choice -> answersTo(choice, name)).map(BonusChoice::label)
			.findFirst().orElse(name.strip());
	}

	/** The condition a name stands for on those combatants: one the data holds, with its value, else a label. */
	private ServiceResult<Condition> conditionNamed(List<Integer> ids, String name, int rounds, OptionalInt value)
	{
		if (name.isBlank())
		{
			return ServiceResult.failure(new ServiceError.InvalidArgument("name", "is blank"));
		}
		OptionalInt lasting = (rounds > 0) ? OptionalInt.of(rounds) : OptionalInt.empty();
		Optional<BonusChoice> known = table.conditionChoices(ids).stream().filter(choice -> answersTo(choice, name))
			.findFirst();
		if (known.isEmpty())
		{
			return value.isPresent()
				? ServiceResult.failure(new ServiceError.InvalidArgument("value", name + " is a label, which takes no value"))
				: ServiceResult.success(Condition.label(name.strip(), lasting));
		}
		Optional<IntRange> range = known.get().range();
		if (range.isEmpty() && value.isPresent())
		{
			return ServiceResult.failure(new ServiceError.InvalidArgument("value", known.get().label() + " takes no value"));
		}
		if (range.isPresent() && value.isPresent()
			&& (value.getAsInt() < range.get().minimum() || value.getAsInt() > range.get().maximum()))
		{
			return ServiceResult.failure(new ServiceError.InvalidArgument("value", known.get().label() + " takes from "
				+ range.get().minimum() + " to " + range.get().maximum()));
		}
		OptionalInt chosen = range.isPresent() ? OptionalInt.of(value.orElse(range.get().maximum())) : OptionalInt.empty();
		return ServiceResult.success(new Condition(known.get().label(), BonusSourceKey.parse(known.get().key()), lasting,
			chosen));
	}

	public ServiceResult<List<String>> listConditions(int id)
	{
		return withSources(() -> combatant(id)
			.map(found -> table.conditionSources(id).keySet().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList()));
	}

	public ServiceResult<GameTableState> delay(int id)
	{
		return combatant(id).map(found -> {
			table.delay(id);
			return state(List.of());
		});
	}

	public ServiceResult<GameTableState> resume(int id)
	{
		return combatant(id).andThen(found -> {
			if (!found.delaying())
			{
				return ServiceResult.failure(new ServiceError.NotAllowed(found.name() + " is not delaying."));
			}
			table.resume(id);
			return ServiceResult.success(state(List.of()));
		});
	}

	public ServiceResult<GameTableState> move(int id, int index)
	{
		return combatant(id).map(found -> {
			table.move(id, index);
			return state(List.of());
		});
	}

	public ServiceResult<GameTableState> remove(int id)
	{
		return combatant(id).map(found -> {
			table.remove(id);
			return state(List.of());
		});
	}

	public ServiceResult<CombatEnded> endCombat()
	{
		int blocksBefore = table.scenario().loot().size();
		CombatReward reward = table.endCombat();
		List<LootBlock> loot = table.scenario().loot();
		LootBlockEntry ended = loot.size() > blocksBefore ? lootEntry(loot.getLast()) : null;
		return ServiceResult.success(new CombatEnded(reward.experience(), reward.challengeRating().orElse(null),
			reward.players(), reward.perPlayer(), ended, state(List.of())));
	}

	public ServiceResult<GameTableState> addImage(String encounter, String name, String category, String path,
		String contentBase64)
	{
		Optional<ImageCategory> kind = categoryNamed(category);
		if (kind.isEmpty())
		{
			return ServiceResult.failure(new ServiceError.InvalidArgument(CATEGORY, "one of "
				+ Arrays.stream(ImageCategory.values()).map(GameTableService::categoryName).toList()));
		}
		byte[] bytes;
		String imageName = name;
		try
		{
			if (path != null && !path.isBlank())
			{
				Path file = Path.of(path);
				bytes = Files.readAllBytes(file);
				imageName = (name == null || name.isBlank()) ? file.getFileName().toString() : name;
			}
			else if (contentBase64 != null && !contentBase64.isBlank())
			{
				bytes = Base64.getDecoder().decode(contentBase64.strip());
			}
			else
			{
				return ServiceResult.failure(new ServiceError.InvalidArgument("path", "a file path or content_base64 is needed"));
			}
		}
		catch (IOException | IllegalArgumentException unreadable)
		{
			return ServiceResult.failure(new ServiceError.InvalidArgument("path", String.valueOf(unreadable.getMessage())));
		}
		if (imageName == null || imageName.isBlank())
		{
			return ServiceResult.failure(new ServiceError.InvalidArgument("name", "an image has a name"));
		}
		try
		{
			table.addImage(Optional.ofNullable(encounter).filter(found -> !found.isBlank()), imageName, kind.get(), bytes);
		}
		catch (IllegalArgumentException refused)
		{
			return ServiceResult.failure(new ServiceError.NotAllowed(refused.getMessage()));
		}
		return ServiceResult.success(state(List.of()));
	}

	public ServiceResult<ImageContent> getImage(int id)
	{
		return image(id).map(image -> new ImageContent(image.id(), image.name(), categoryName(image.category()),
			image.format().mediaType(), image.content()));
	}

	public ServiceResult<GameTableState> removeImage(int id)
	{
		return image(id).map(image -> {
			table.removeImage(id);
			return state(List.of());
		});
	}

	public ServiceResult<TabletopSent> sendImageToTabletop(int id, boolean placeOnTable)
	{
		return image(id).andThen(image -> switch (tabletop.send(image.forTabletop(), placeOnTable))
		{
			case TabletopOutcome.Sent sent -> ServiceResult.success(new TabletopSent(sent.key(), sent.url().orElse(null),
				sent.placed()));
			case TabletopOutcome.Failed failed -> ServiceResult.failure(new ServiceError.DataFailure(failed.reason()));
		});
	}

	private ServiceResult<ScenarioImage> image(int id)
	{
		return table.image(id).<ServiceResult<ScenarioImage>>map(ServiceResult::success)
			.orElseGet(() -> ServiceResult.failure(new ServiceError.EntryNotFound("Image", String.valueOf(id))));
	}

	private static List<ImageEntry> imageEntries(List<ScenarioImage> images)
	{
		return images.stream().map(image -> new ImageEntry(image.id(), image.name(), categoryName(image.category()),
			image.format().mediaType(), image.sizeInBytes())).toList();
	}

	private static String categoryName(ImageCategory category)
	{
		return category.name().toLowerCase(Locale.ROOT);
	}

	private static Optional<ImageCategory> categoryNamed(String category)
	{
		return Arrays.stream(ImageCategory.values())
			.filter(found -> categoryName(found).equals(category == null ? "" : category.strip().toLowerCase(Locale.ROOT)))
			.findFirst();
	}

	public ServiceResult<GameTableState> selectLoot(int blockId, int line, boolean selected)
	{
		return lootBlock(blockId).andThen(block -> {
			if (line < 0 || line >= block.lines().size())
			{
				return ServiceResult.failure(new ServiceError.EntryNotFound("Loot line", String.valueOf(line)));
			}
			table.selectLoot(blockId, line, selected);
			return ServiceResult.success(state(List.of()));
		});
	}

	public ServiceResult<GameTableState> rerollLoot(int blockId)
	{
		return lootBlock(blockId).map(block -> {
			table.rerollLoot(blockId);
			return state(List.of());
		});
	}

	public ServiceResult<GameTableState> removeLoot(int blockId)
	{
		return lootBlock(blockId).map(block -> {
			table.removeLoot(blockId);
			return state(List.of());
		});
	}

	private ServiceResult<LootBlock> lootBlock(int blockId)
	{
		return table.scenario().loot().stream().filter(block -> block.id() == blockId).findFirst()
			.<ServiceResult<LootBlock>>map(ServiceResult::success)
			.orElseGet(() -> ServiceResult.failure(new ServiceError.EntryNotFound("Loot", String.valueOf(blockId))));
	}

	private LootBlockEntry lootEntry(LootBlock block)
	{
		List<LootLineEntry> lines = block.lines().stream().map(line -> new LootLineEntry(line.name(), line.quantity(),
			line.carrier(), line.drawnAs().map(type -> type.name().toLowerCase(Locale.ROOT)).orElse(CARRIED), line.value(),
			line.resale(), line.selected())).toList();
		int players = table.scenario().party().size();
		return new LootBlockEntry(block.id(), block.title(), block.challengeRating(), block.experience(),
			block.experiencePerPlayer(players), block.budget(), lines,
			block.selectedValue(), block.selectedResale(), block.sharePerPlayer(players));
	}

	public ServiceResult<CombatantSheet> getCombatant(int id)
	{
		return withSources(() -> combatant(id).map(found -> {
			boolean monster = found.kind().isGameMasters();
			List<String> templates = found.recipe().filter(recipe -> monster)
				.map(recipe -> table.templateKeysOf(recipe) instanceof ForgeOutcome.Built<List<String>> built
					? built.value() : List.<String>of())
				.orElse(List.of());
			String challengeRating = found.recipe().filter(recipe -> monster)
				.map(recipe -> table.preview(recipe) instanceof ForgeOutcome.Built<MonsterPreview> preview
					? preview.value().challengeRating() : null)
				.orElse(null);
			return new CombatantSheet(id, found.name(), found.kind().name(), table.sheet(id).orElse(null),
				challengeRating, templates, monster ? null : table.glance(id).orElse(null), conditionsOf(found),
				spellsOf(found));
		}));
	}

	public ServiceResult<GameTableState> saveMonsterAsCharacter(int id, String path)
	{
		return monster(id).andThen(found -> withSources(() -> {
			DataSetFacade dataSet = session.getCurrentDataSet();
			List<Campaign> campaigns = ListFacades.wrap(dataSet.getCampaigns());
			return table.saveMonster(id, new File(path), dataSet.getGameMode(), campaigns)
				.<ServiceResult<GameTableState>>map(reason -> ServiceResult.failure(new ServiceError.DataFailure(reason)))
				.orElseGet(() -> ServiceResult.success(state(List.of())));
		}));
	}

	/**
	 * Builds the game table's monsters on the sources the session has loaded, once
	 * per set of sources, then makes the call.
	 */
	private <T> ServiceResult<T> withSources(Supplier<ServiceResult<T>> call)
	{
		DataSetFacade dataSet = session.getCurrentDataSet();
		if (dataSet == null)
		{
			return ServiceResult.failure(new ServiceError.NoSourcesLoaded());
		}
		String loaded = session.getCurrentSourceSetId();
		if (!loaded.equals(sourcesBuiltOn))
		{
			// Building a monster may ask the data's questions: the first answers are taken
			HeadlessUIDelegate choices = new HeadlessUIDelegate();
			choices.setAutoChoose(true);
			table.useSources(new MonsterForge(ListFacades.wrap(dataSet.getCampaigns()), choices),
				dataSet.getGameMode().getName(), campaignKeys(dataSet));
			sourcesBuiltOn = loaded;
		}
		return call.get();
	}

	private boolean loaded(Scenario scenario)
	{
		DataSetFacade dataSet = session.getCurrentDataSet();
		return dataSet != null && dataSet.getGameMode().getName().equals(scenario.gameMode())
			&& new HashSet<>(campaignKeys(dataSet)).containsAll(scenario.campaigns());
	}

	private static List<String> campaignKeys(DataSetFacade dataSet)
	{
		return ListFacades.wrap(dataSet.getCampaigns()).stream().map(Campaign::getKeyName).toList();
	}

	private int indexOfEncounter(String name)
	{
		List<Encounter> encounters = table.scenario().encounters();
		for (int at = 0; at < encounters.size(); at++)
		{
			if (encounters.get(at).name().equals(name))
			{
				return at;
			}
		}
		return -1;
	}

	private ServiceResult<Combatant> combatant(int id)
	{
		return table.combat().combatant(id).<ServiceResult<Combatant>>map(ServiceResult::success)
			.orElseGet(() -> ServiceResult.failure(new ServiceError.EntryNotFound("Combatant", String.valueOf(id))));
	}

	private ServiceResult<Combatant> monster(int id)
	{
		return combatant(id).andThen(found -> found.recipe().isPresent() ? ServiceResult.success(found)
			: ServiceResult.failure(new ServiceError.NotAllowed(found.name() + " is not a monster.")));
	}

	private ServiceResult<Combatant> trackedHitPoints(int id, int amount)
	{
		if (amount < 1)
		{
			return ServiceResult.failure(new ServiceError.InvalidArgument("amount", "must be at least 1"));
		}
		return combatant(id).andThen(found -> found.health().isPresent() ? ServiceResult.success(found)
			: ServiceResult.failure(new ServiceError.NotAllowed(
				found.name() + "'s hit points are tracked by the player.")));
	}

	private static ServiceResult<MonsterRecipe> recipeOf(RecipeRequest request)
	{
		boolean hasRace = request.race() != null && !request.race().isBlank();
		boolean hasFile = request.characterFile() != null && !request.characterFile().isBlank();
		boolean hasKit = request.kit() != null && !request.kit().isBlank();
		if (Stream.of(hasRace, hasFile, hasKit).filter(Boolean::booleanValue).count() != 1)
		{
			return ServiceResult.failure(new ServiceError.InvalidArgument("recipe",
				"names one of a race, a character file or a kit"));
		}
		if (request.addedHitDice() < 0)
		{
			return ServiceResult.failure(new ServiceError.InvalidArgument("added_hit_dice", "cannot be negative"));
		}
		MonsterSource source;
		if (hasRace)
		{
			source = new MonsterSource.BestiaryRace(request.race().strip());
		}
		else if (hasFile)
		{
			source = new MonsterSource.CharacterFile(request.characterFile().strip());
		}
		else
		{
			source = new MonsterSource.NpcKit(request.kit().strip());
		}
		MonsterRecipe recipe = MonsterRecipe.of(source);
		for (String template : request.addedTemplates())
		{
			recipe = recipe.withTemplate(template);
		}
		for (String template : request.removedTemplates())
		{
			recipe = recipe.withoutTemplate(template);
		}
		return ServiceResult.success(recipe.withHitDice(request.addedHitDice()));
	}

	private static boolean answersTo(BonusChoice choice, String wanted)
	{
		String name = wanted.strip().toLowerCase(Locale.ROOT);
		return choice.label().toLowerCase(Locale.ROOT).equals(name) || BonusSourceKey.parse(choice.key())
			.map(source -> source.key().toLowerCase(Locale.ROOT).equals(name)).orElse(false);
	}

	private static String described(Expiry expiry)
	{
		return switch (expiry)
		{
			case Expiry.CreatureLeft left -> left.combatant() + " leaves the fight";
			case Expiry.ConditionEnded ended -> ended.combatant() + ": " + ended.condition() + " ends";
			case Expiry.SpellEnded ended -> ended.combatant() + ": spell " + ended.spell() + " ends";
		};
	}

	private GameTableState state(List<String> notices)
	{
		Scenario scenario = table.scenario();
		return new GameTableState(table.file().map(File::getAbsolutePath).orElse(null), table.isDirty(),
			scenario.gameMode(), scenario.campaigns(),
			scenario.party().stream().map(member -> new PartyMemberEntry(member.name(),
				member.characterFile().orElse(null))).toList(),
			scenario.encounters().stream().map(this::encounterEntry).toList(), combatEntry(scenario.combat()),
			scenario.loot().stream().map(this::lootEntry).toList(), imageEntries(scenario.images()), List.copyOf(notices));
	}

	private EncounterEntry encounterEntry(Encounter encounter)
	{
		List<EncounterMonsterEntry> monsters = new ArrayList<>();
		long total = 0;
		for (EncounterMonster monster : encounter.monsters())
		{
			long experience = table.hasSources() ? table.experienceOf(monster) : 0;
			String challengeRating = table.hasSources()
				&& table.challengeRatingOf(monster) instanceof ForgeOutcome.Built<String> built ? built.value() : null;
			monsters.add(new EncounterMonsterEntry(monster.name(), monster.count(), recipeEntry(monster.recipe()),
				experience, challengeRating));
			total += experience;
		}
		return new EncounterEntry(encounter.name(), encounter.notes(), monsters, total,
			Experience.challengeRatingOf(total).orElse(null), imageEntries(encounter.images()));
	}

	private static CombatEntry combatEntry(Combat combat)
	{
		List<CombatantEntry> order = new ArrayList<>();
		for (Turn turn : combat.turns())
		{
			combat.combatant(turn.combatantId())
				.ifPresent(found -> order.add(combatantEntry(found, Optional.of(turn), combat.current())));
		}
		combat.order().stream().filter(Combatant::delaying)
			.forEach(found -> order.add(combatantEntry(found, Optional.empty(), combat.current())));
		return new CombatEntry(combat.isStarted() ? combat.round() : 0, combat.experience(), order);
	}

	private static CombatantEntry combatantEntry(Combatant combatant, Optional<Turn> turn, Optional<Turn> current)
	{
		boolean second = turn.map(Turn::second).orElse(false);
		OptionalInt score = combatant.initiative().score();
		Integer initiative = score.isPresent()
			? Integer.valueOf(second ? score.getAsInt() - Initiative.SECOND_TURN_DROP
				: score.getAsInt())
			: null;
		return new CombatantEntry(combatant.id(), combatant.name(), combatant.kind().name(), initiative,
			combatant.initiative().modifier(), second, turn.isPresent() && turn.equals(current), combatant.delaying(),
			combatant.health().map(health -> health.remaining()).orElse(null),
			combatant.health().map(health -> health.maximum()).orElse(null), conditionsOf(combatant),
			spellsOf(combatant),
			combatant.roundsLeft().isPresent() ? combatant.roundsLeft().getAsInt() : null,
			combatant.summonerId().isPresent() ? combatant.summonerId().getAsInt() : null,
			combatant.recipe().map(GameTableService::recipeEntry).orElse(null));
	}

	private static List<ConditionEntry> conditionsOf(Combatant combatant)
	{
		return combatant.conditions().stream().map(condition -> new ConditionEntry(condition.name(),
			condition.roundsLeft().isPresent() ? condition.roundsLeft().getAsInt() : null,
			condition.bonus().isPresent())).toList();
	}

	private static List<CastSpellEntry> spellsOf(Combatant combatant)
	{
		return combatant.spells().stream().map(spell -> new CastSpellEntry(spell.name(),
			spell.roundsLeft().isPresent() ? spell.roundsLeft().getAsInt() : null, spell.targetIds())).toList();
	}

	private static RecipeEntry recipeEntry(MonsterRecipe recipe)
	{
		return switch (recipe.source())
		{
			case MonsterSource.BestiaryRace race -> new RecipeEntry(race.raceKey(), null, null, recipe.addedTemplates(),
				recipe.removedTemplates(), recipe.addedHitDice());
			case MonsterSource.CharacterFile file -> new RecipeEntry(null, file.path(), null, recipe.addedTemplates(),
				recipe.removedTemplates(), recipe.addedHitDice());
			case MonsterSource.NpcKit kit -> new RecipeEntry(null, null, kit.kitKey(), recipe.addedTemplates(),
				recipe.removedTemplates(), recipe.addedHitDice());
		};
	}
}
