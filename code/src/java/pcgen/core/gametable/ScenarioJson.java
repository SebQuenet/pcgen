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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;

import pcgen.core.conjurations.BonusSourceKey;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Writes a scenario as JSON and reads it back, checking every field on the way in.
 */
public final class ScenarioJson
{
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final int FORMAT = 1;

	private ScenarioJson()
	{
	}

	/**
	 * @param scenario a scenario.
	 * @return its file's text.
	 */
	public static String write(Scenario scenario)
	{
		ObjectNode root = MAPPER.createObjectNode();
		root.put("format", FORMAT);
		root.put("gameMode", scenario.gameMode());
		strings(root.putArray("campaigns"), scenario.campaigns());
		ArrayNode party = root.putArray("party");
		for (PartyMember member : scenario.party())
		{
			ObjectNode node = party.addObject();
			node.put("name", member.name());
			member.characterFile().ifPresent(file -> node.put("characterFile", file));
		}
		ArrayNode encounters = root.putArray("encounters");
		for (Encounter encounter : scenario.encounters())
		{
			ObjectNode node = encounters.addObject();
			node.put("name", encounter.name());
			node.put("notes", encounter.notes());
			ArrayNode monsters = node.putArray("monsters");
			for (EncounterMonster monster : encounter.monsters())
			{
				ObjectNode monsterNode = monsters.addObject();
				monsterNode.put("name", monster.name());
				monsterNode.put("count", monster.count());
				writeRecipe(monsterNode.putObject("recipe"), monster.recipe());
			}
		}
		writeCombat(root.putObject("combat"), scenario.combat());
		try
		{
			return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
		}
		catch (JsonProcessingException impossible)
		{
			throw new IllegalStateException("A tree of plain nodes always writes", impossible);
		}
	}

	/**
	 * @param text a scenario file's text.
	 * @return the scenario, or why the text does not hold one.
	 */
	public static ScenarioReading read(String text)
	{
		try
		{
			JsonNode root = MAPPER.readTree(text);
			if (root == null || !root.isObject())
			{
				return new ScenarioReading.Unreadable("The file does not hold a scenario");
			}
			List<PartyMember> party = list(root, "party",
				node -> new PartyMember(text(node, "name"), optionalText(node, "characterFile")));
			List<Encounter> encounters = list(root, "encounters", ScenarioJson::readEncounter);
			Scenario scenario = new Scenario(text(root, "gameMode"), list(root, "campaigns", JsonNode::asText), party,
				encounters, readCombat(required(root, "combat")));
			return new ScenarioReading.Read(scenario);
		}
		catch (JsonProcessingException | IllegalArgumentException | IllegalStateException badContent)
		{
			return new ScenarioReading.Unreadable(badContent.getMessage());
		}
	}

	private static Encounter readEncounter(JsonNode node)
	{
		List<EncounterMonster> monsters = list(node, "monsters", monster -> new EncounterMonster(text(monster, "name"),
			readRecipe(required(monster, "recipe")), required(monster, "count").asInt()));
		return new Encounter(text(node, "name"), monsters, text(node, "notes"));
	}

	private static void writeRecipe(ObjectNode node, MonsterRecipe recipe)
	{
		switch (recipe.source())
		{
			case MonsterSource.BestiaryRace race -> node.put("race", race.raceKey());
			case MonsterSource.CharacterFile file -> node.put("file", file.path());
		}
		strings(node.putArray("addedTemplates"), recipe.addedTemplates());
		strings(node.putArray("removedTemplates"), recipe.removedTemplates());
		node.put("addedHitDice", recipe.addedHitDice());
	}

	private static MonsterRecipe readRecipe(JsonNode node)
	{
		MonsterSource source;
		if (node.has("race"))
		{
			source = new MonsterSource.BestiaryRace(text(node, "race"));
		}
		else if (node.has("file"))
		{
			source = new MonsterSource.CharacterFile(text(node, "file"));
		}
		else
		{
			throw new IllegalArgumentException("A monster comes from a race or a character file");
		}
		return new MonsterRecipe(source, list(node, "addedTemplates", JsonNode::asText),
			list(node, "removedTemplates", JsonNode::asText), required(node, "addedHitDice").asInt());
	}

	private static void writeCombat(ObjectNode node, Combat combat)
	{
		node.put("round", combat.round());
		node.put("experience", combat.experience());
		combat.current().ifPresent(turn -> {
			ObjectNode current = node.putObject("current");
			current.put("combatant", turn.combatantId());
			current.put("second", turn.second());
		});
		ArrayNode combatants = node.putArray("combatants");
		for (Combatant combatant : combat.order())
		{
			ObjectNode fighter = combatants.addObject();
			fighter.put("id", combatant.id());
			fighter.put("name", combatant.name());
			fighter.put("group", combatant.group());
			ObjectNode initiative = fighter.putObject("initiative");
			combatant.initiative().score().ifPresent(score -> initiative.put("score", score));
			initiative.put("modifier", combatant.initiative().modifier());
			initiative.put("dual", combatant.initiative().dual());
			combatant.health().ifPresent(health -> {
				ObjectNode healthNode = fighter.putObject("health");
				healthNode.put("maximum", health.maximum());
				healthNode.put("damage", health.damage());
			});
			ArrayNode conditions = fighter.putArray("conditions");
			for (Condition condition : combatant.conditions())
			{
				ObjectNode conditionNode = conditions.addObject();
				conditionNode.put("name", condition.name());
				condition.bonus().ifPresent(bonus -> conditionNode.put("bonus", bonus.text()));
				condition.roundsLeft().ifPresent(rounds -> conditionNode.put("roundsLeft", rounds));
			}
			combatant.recipe().ifPresent(recipe -> writeRecipe(fighter.putObject("recipe"), recipe));
			combatant.characterFile().ifPresent(file -> fighter.put("characterFile", file));
			combatant.summonerId().ifPresent(summoner -> fighter.put("summoner", summoner));
			combatant.roundsLeft().ifPresent(rounds -> fighter.put("roundsLeft", rounds));
			fighter.put("delaying", combatant.delaying());
		}
	}

	private static Combat readCombat(JsonNode node)
	{
		List<Combatant> combatants = list(node, "combatants", ScenarioJson::readCombatant);
		Optional<Turn> current = Optional.ofNullable(node.get("current"))
			.map(turn -> new Turn(required(turn, "combatant").asInt(), required(turn, "second").asBoolean()));
		return new Combat(required(node, "round").asInt(), combatants, current, required(node, "experience").asLong());
	}

	private static Combatant readCombatant(JsonNode node)
	{
		JsonNode initiative = required(node, "initiative");
		Optional<Health> health = Optional.ofNullable(node.get("health"))
			.map(found -> new Health(required(found, "maximum").asInt(), required(found, "damage").asInt()));
		List<Condition> conditions = list(node, "conditions", condition -> new Condition(text(condition, "name"),
			optionalText(condition, "bonus").map(ScenarioJson::bonusKey), optionalInt(condition, "roundsLeft")));
		return new Combatant(required(node, "id").asInt(), text(node, "name"), text(node, "group"),
			new Initiative(optionalInt(initiative, "score"), required(initiative, "modifier").asInt(),
				required(initiative, "dual").asBoolean()), health, conditions,
			Optional.ofNullable(node.get("recipe")).map(ScenarioJson::readRecipe), optionalText(node, "characterFile"),
			optionalInt(node, "summoner"), optionalInt(node, "roundsLeft"), required(node, "delaying").asBoolean());
	}

	private static BonusSourceKey bonusKey(String text)
	{
		return BonusSourceKey.parse(text)
			.orElseThrow(() -> new IllegalArgumentException("Not the source of a bonus: " + text));
	}

	private static void strings(ArrayNode array, List<String> values)
	{
		values.forEach(array::add);
	}

	private static JsonNode required(JsonNode node, String field)
	{
		JsonNode found = node.get(field);
		if (found == null || found.isNull())
		{
			throw new IllegalArgumentException("The scenario lacks the field " + field);
		}
		return found;
	}

	private static String text(JsonNode node, String field)
	{
		return required(node, field).asText();
	}

	private static Optional<String> optionalText(JsonNode node, String field)
	{
		return Optional.ofNullable(node.get(field)).filter(found -> !found.isNull()).map(JsonNode::asText);
	}

	private static OptionalInt optionalInt(JsonNode node, String field)
	{
		JsonNode found = node.get(field);
		return (found == null || found.isNull()) ? OptionalInt.empty() : OptionalInt.of(found.asInt());
	}

	private static <T> List<T> list(JsonNode node, String field, Function<JsonNode, T> reader)
	{
		JsonNode array = required(node, field);
		if (!array.isArray())
		{
			throw new IllegalArgumentException("The field " + field + " is not a list");
		}
		List<T> values = new ArrayList<>();
		array.forEach(element -> values.add(reader.apply(element)));
		return values;
	}
}
