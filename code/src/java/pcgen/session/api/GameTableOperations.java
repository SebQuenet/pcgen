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

import java.util.List;
import java.util.OptionalInt;

import pcgen.session.service.GameTableService;
import pcgen.session.service.GameTableService.EncounterMonsterRequest;
import pcgen.session.service.GameTableService.RecipeRequest;
import pcgen.session.service.model.PartyMemberEntry;

/**
 * The game table: a game master's scenario, its party, its prepared encounters and the
 * fight under way. The server holds one scenario at a time.
 */
public final class GameTableOperations
{
	private static final String COMBATANT_ID = "combatant_id";
	private static final String CASTER_ID = "caster_id";
	private static final String RECIPE = "recipe";
	private static final String NAME = "name";
	private static final String COUNT = "count";

	private static final String RECIPE_SCHEMA = """
		{
			"type": "object",
			"description": "How to build a monster: a race of the loaded sources or a character file, then the game master's touch",
			"properties": {
				"race": { "type": "string", "description": "Key of a race of the loaded sources, such as 'Wolf'" },
				"character_file": { "type": "string", "description": "Path of a .pcg file holding the monster" },
				"added_templates": { "type": "array", "items": { "type": "string" },
					"description": "Keys of templates to lay on it, such as 'Advanced Creature' or 'Mythic Simple Template ~ Agile'" },
				"removed_templates": { "type": "array", "items": { "type": "string" },
					"description": "Keys of templates it has that are taken off" },
				"added_hit_dice": { "type": "integer", "description": "Hit dice added to its race's; 0 by default" }
			}
		}
		""";

	private static final String COMBATANT_ONLY_SCHEMA = """
		{
			"type": "object",
			"properties": {
				"combatant_id": { "type": "integer", "description": "Id of a combatant of the fight, from get_game_table" }
			},
			"required": ["combatant_id"]
		}
		""";

	private static final String NO_ARGUMENT_SCHEMA = """
		{ "type": "object", "properties": {} }
		""";

	private static final String STATE = " Answers the whole game table: the scenario's file, party, encounters with "
		+ "their experience and challenge rating, and the fight in order of initiative, each combatant with its id.";

	private GameTableOperations()
	{
	}

	public static List<Operation> of(GameTableService table)
	{
		return List.of(newScenario(table), openScenario(table), saveScenario(table), getGameTable(table),
			setParty(table), setEncounter(table), removeEncounter(table), previewMonster(table),
			launchEncounter(table), addMonsters(table), addPlayer(table), summonCreatures(table),
			retouchMonster(table), setInitiative(table), startCombat(table), nextTurn(table), damage(table),
			heal(table), addCondition(table), removeCondition(table), castSpell(table), dropSpell(table), listConditions(table), delayTurn(table),
			resumeTurn(table), moveCombatant(table), removeCombatant(table), endCombat(table), getCombatant(table),
			saveMonsterAsCharacter(table));
	}

	private static RecipeRequest recipe(Arguments recipe)
	{
		return new RecipeRequest(recipe.optionalString("race", null), recipe.optionalString("character_file", null),
			recipe.optionalStringList("added_templates"), recipe.optionalStringList("removed_templates"),
			recipe.optionalInt("added_hit_dice", 0));
	}

	private static Operation newScenario(GameTableService service)
	{
		return Operation.exclusive("new_scenario",
			"Start an empty scenario on the sources loaded now, leaving the one open unsaved." + STATE,
			NO_ARGUMENT_SCHEMA, arguments -> service.newScenario());
	}

	private static Operation openScenario(GameTableService service)
	{
		return Operation.exclusive("open_scenario",
			"Open a scenario file (.pcgtable). When the loaded sources lack the scenario's, its sources are "
				+ "loaded first, as load_sources would. Monsters that cannot be built are listed in notices." + STATE,
			"""
				{
					"type": "object",
					"properties": {
						"path": { "type": "string", "description": "Path of the .pcgtable file" }
					},
					"required": ["path"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.openScenario(arguments.requiredString("path"))));
	}

	private static Operation saveScenario(GameTableService service)
	{
		return Operation.exclusive("save_scenario",
			"Save the scenario, to the given path or to the file it was opened from or last saved to." + STATE,
			"""
				{
					"type": "object",
					"properties": {
						"path": { "type": "string", "description": "Path of the .pcgtable file; optional once saved" }
					}
				}
				""",
			arguments -> arguments.decodeThen(() -> service.saveScenario(arguments.optionalString("path", ""))));
	}

	private static Operation getGameTable(GameTableService service)
	{
		return Operation.exclusive("get_game_table", "Read the game table." + STATE, NO_ARGUMENT_SCHEMA,
			arguments -> service.getGameTable());
	}

	private static Operation setParty(GameTableService service)
	{
		return Operation.exclusive("set_party",
			"Replace the party of players' characters. Their players track their hit points; a character file, "
				+ "when the game master has a copy, lets get_combatant read its perception, sense motive and saves."
				+ STATE,
			"""
				{
					"type": "object",
					"properties": {
						"members": { "type": "array", "items": { "type": "object", "properties": {
							"name": { "type": "string" },
							"character_file": { "type": "string", "description": "Path of its .pcg file, optional" }
						}, "required": ["name"] } }
					},
					"required": ["members"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.setParty(arguments.requiredEntryList("members").stream()
				.map(member -> new PartyMemberEntry(member.requiredString(NAME),
					member.optionalString("character_file", null)))
				.toList())));
	}

	private static Operation setEncounter(GameTableService service)
	{
		return Operation.exclusive("set_encounter",
			"Write a prepared encounter, replacing the one of the same name." + STATE,
			"""
				{
					"type": "object",
					"properties": {
						"name": { "type": "string", "description": "The encounter's name, unique in the scenario" },
						"notes": { "type": "string" },
						"monsters": { "type": "array", "items": { "type": "object", "properties": {
							"name": { "type": "string", "description": "The name of one, numbered when several enter" },
							"count": { "type": "integer" },
							"recipe": %s
						}, "required": ["name", "recipe"] } }
					},
					"required": ["name", "monsters"]
				}
				""".formatted(RECIPE_SCHEMA),
			arguments -> arguments.decodeThen(() -> service.setEncounter(arguments.requiredString(NAME),
				arguments.optionalString("notes", ""), arguments.requiredEntryList("monsters").stream()
					.map(monster -> new EncounterMonsterRequest(monster.requiredString(NAME),
						recipe(monster.requiredNestedObject(RECIPE)), monster.optionalInt(COUNT, 1)))
					.toList())));
	}

	private static Operation removeEncounter(GameTableService service)
	{
		return Operation.exclusive("remove_encounter", "Remove a prepared encounter." + STATE,
			"""
				{
					"type": "object",
					"properties": { "name": { "type": "string" } },
					"required": ["name"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.removeEncounter(arguments.requiredString(NAME))));
	}

	private static Operation previewMonster(GameTableService service)
	{
		return Operation.exclusive("preview_monster",
			"Build a monster without adding it anywhere, and answer its numbers (hit dice, hit points, the three "
				+ "armor classes, attacks, saves, special abilities) and challenge rating.",
			"""
				{
					"type": "object",
					"properties": { "recipe": %s },
					"required": ["recipe"]
				}
				""".formatted(RECIPE_SCHEMA),
			arguments -> arguments.decodeThen(
				() -> service.previewMonster(recipe(arguments.requiredNestedObject(RECIPE)))));
	}

	private static Operation launchEncounter(GameTableService service)
	{
		return Operation.exclusive("launch_encounter",
			"Bring a prepared encounter into the fight: the party members not already in it, then its monsters, "
				+ "identical monsters sharing one rolled initiative. Players announce theirs with set_initiative."
				+ STATE,
			"""
				{
					"type": "object",
					"properties": { "name": { "type": "string", "description": "The encounter's name" } },
					"required": ["name"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.launchEncounter(arguments.requiredString(NAME))));
	}

	private static Operation addMonsters(GameTableService service)
	{
		return Operation.exclusive("add_monsters",
			"Bring unprepared monsters into the fight, sharing one rolled initiative. Monsters played by the game "
				+ "master as allies are added this way too." + STATE,
			"""
				{
					"type": "object",
					"properties": {
						"name": { "type": "string", "description": "The name of one, numbered when several enter" },
						"count": { "type": "integer", "description": "How many enter; 1 by default" },
						"recipe": %s
					},
					"required": ["name", "recipe"]
				}
				""".formatted(RECIPE_SCHEMA),
			arguments -> arguments.decodeThen(() -> service.addMonsters(arguments.requiredString(NAME),
				recipe(arguments.requiredNestedObject(RECIPE)), arguments.optionalInt(COUNT, 1))));
	}

	private static Operation addPlayer(GameTableService service)
	{
		return Operation.exclusive("add_player_to_combat",
			"Bring a player's character into the fight without initiative." + STATE,
			"""
				{
					"type": "object",
					"properties": {
						"name": { "type": "string" },
						"character_file": { "type": "string", "description": "Path of its .pcg file, optional" }
					},
					"required": ["name"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.addPlayer(arguments.requiredString(NAME),
				arguments.optionalString("character_file", null))));
	}

	private static Operation summonCreatures(GameTableService service)
	{
		return Operation.exclusive("summon_creatures",
			"Conjure creatures into the fight, right after their conjurer and on its initiative. A player's "
				+ "creatures need no recipe: their player tracks them. A monster's are built from the recipe, "
				+ "with Augment Summoning when the conjurer has the feat." + STATE,
			"""
				{
					"type": "object",
					"properties": {
						"summoner_id": { "type": "integer", "description": "Id of the conjurer" },
						"name": { "type": "string", "description": "The name of one, numbered when several arrive" },
						"count": { "type": "integer", "description": "How many arrive; 1 by default" },
						"rounds": { "type": "integer", "description": "Rounds before they leave; omit when they stay" },
						"recipe": %s
					},
					"required": ["summoner_id", "name"]
				}
				""".formatted(RECIPE_SCHEMA),
			arguments -> arguments.decodeThen(() -> service.summon(arguments.requiredInt("summoner_id"),
				arguments.requiredString(NAME),
				arguments.has(RECIPE) ? recipe(arguments.requiredNestedObject(RECIPE)) : null,
				arguments.optionalInt(COUNT, 1), arguments.optionalInt("rounds", 0))));
	}

	private static Operation retouchMonster(GameTableService service)
	{
		return Operation.exclusive("retouch_monster",
			"Rebuild one monster of the fight from a new recipe, such as with hit dice or templates added. "
				+ "It keeps the damage it took and its conditions; the fight's experience follows its new "
				+ "challenge rating." + STATE,
			"""
				{
					"type": "object",
					"properties": {
						"combatant_id": { "type": "integer" },
						"recipe": %s
					},
					"required": ["combatant_id", "recipe"]
				}
				""".formatted(RECIPE_SCHEMA),
			arguments -> arguments.decodeThen(() -> service.retouchMonster(arguments.requiredInt(COMBATANT_ID),
				recipe(arguments.requiredNestedObject(RECIPE)))));
	}

	private static Operation setInitiative(GameTableService service)
	{
		return Operation.exclusive("set_initiative",
			"Give a combatant its initiative. Its troop of identical monsters and the creatures it conjured take "
				+ "the same score and move to their place." + STATE,
			"""
				{
					"type": "object",
					"properties": {
						"combatant_id": { "type": "integer" },
						"score": { "type": "integer" }
					},
					"required": ["combatant_id", "score"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.setInitiative(arguments.requiredInt(COMBATANT_ID),
				arguments.requiredInt("score"))));
	}

	private static Operation startCombat(GameTableService service)
	{
		return Operation.exclusive("start_combat",
			"Start the fight at round 1, rolling the initiative of monsters still without one." + STATE,
			NO_ARGUMENT_SCHEMA, arguments -> service.startCombat());
	}

	private static Operation nextTurn(GameTableService service)
	{
		return Operation.exclusive("next_turn",
			"Pass the turn on; past the last, a new round begins, wears conditions down and sends conjured "
				+ "creatures away when their rounds run out. What ended is listed in notices." + STATE,
			NO_ARGUMENT_SCHEMA, arguments -> service.nextTurn());
	}

	private static Operation damage(GameTableService service)
	{
		return Operation.exclusive("damage_combatant", "Take hit points off a monster." + STATE,
			amountSchema("Hit points lost"),
			arguments -> arguments.decodeThen(() -> service.damage(arguments.requiredInt(COMBATANT_ID),
				arguments.requiredInt("amount"))));
	}

	private static Operation heal(GameTableService service)
	{
		return Operation.exclusive("heal_combatant", "Give a monster hit points back, up to its full." + STATE,
			amountSchema("Hit points healed"),
			arguments -> arguments.decodeThen(() -> service.heal(arguments.requiredInt(COMBATANT_ID),
				arguments.requiredInt("amount"))));
	}

	private static String amountSchema(String meaning)
	{
		return """
			{
				"type": "object",
				"properties": {
					"combatant_id": { "type": "integer" },
					"amount": { "type": "integer", "description": "%s" }
				},
				"required": ["combatant_id", "amount"]
			}
			""".formatted(meaning);
	}

	private static Operation addCondition(GameTableService service)
	{
		return Operation.exclusive("add_condition",
			"Lay a condition, temporary bonus or negative property on one combatant or several at once, such as "
				+ "a band of monsters. On a monster, one the data holds (see list_conditions) changes its numbers; "
				+ "anything else, and anything on a player, is a label. A condition of the same name is replaced."
				+ STATE,
			"""
				{
					"type": "object",
					"properties": {
						"combatant_id": { "type": "integer", "description": "The combatant, when it goes on one" },
						"combatant_ids": { "type": "array", "items": { "type": "integer" },
							"description": "The combatants, when it goes on several" },
						"name": { "type": "string", "description": "Its name or key, such as 'Shaken'" },
						"rounds": { "type": "integer", "description": "Rounds before it ends; omit when it lasts" },
						"value": { "type": "integer",
							"description": "The value a bonus asking one takes, such as 4 for 'Ability Damaged (Strength)'; its highest when omitted" }
					},
					"required": ["name"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.addCondition(combatantIds(arguments),
				arguments.requiredString(NAME), arguments.optionalInt("rounds", 0), optionalValue(arguments))));
	}

	private static Operation removeCondition(GameTableService service)
	{
		return Operation.exclusive("remove_condition",
			"Take a condition off one combatant or several at once; it comes off those bearing it." + STATE,
			"""
				{
					"type": "object",
					"properties": {
						"combatant_id": { "type": "integer", "description": "The combatant, when it comes off one" },
						"combatant_ids": { "type": "array", "items": { "type": "integer" },
							"description": "The combatants, when it comes off several" },
						"name": { "type": "string", "description": "The condition's name as get_game_table shows it" }
					},
					"required": ["name"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.removeCondition(combatantIds(arguments),
				arguments.requiredString(NAME))));
	}

	private static Operation castSpell(GameTableService service)
	{
		return Operation.exclusive("cast_spell",
			"Keep track of a spell a combatant cast. Its rounds count down as each turn of its caster begins; "
				+ "its effect, a condition of its name, stays on its targets until it ends or is dropped. When "
				+ "the data holds a temporary bonus of that name (see list_conditions), the effect changes the "
				+ "numbers of the monsters it targets. Casting it again replaces it." + STATE,
			"""
				{
					"type": "object",
					"properties": {
						"caster_id": { "type": "integer", "description": "Id of the combatant who cast it" },
						"name": { "type": "string", "description": "The spell, such as 'Bless'" },
						"rounds": { "type": "integer", "description": "Its duration in rounds; omit when it lasts until dropped" },
						"target_ids": { "type": "array", "items": { "type": "integer" },
							"description": "Ids of the combatants it affects; omit to only keep track of it" },
						"value": { "type": "integer", "description": "The value its bonus takes when it asks one; its highest when omitted" }
					},
					"required": ["caster_id", "name"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.castSpell(arguments.requiredInt(CASTER_ID),
				arguments.requiredString(NAME), arguments.optionalInt("rounds", 0),
				arguments.optionalIntList("target_ids"), optionalValue(arguments))));
	}

	private static Operation dropSpell(GameTableService service)
	{
		return Operation.exclusive("drop_spell",
			"End a spell a combatant keeps up before its time; its effect leaves its targets." + STATE,
			"""
				{
					"type": "object",
					"properties": {
						"caster_id": { "type": "integer" },
						"name": { "type": "string", "description": "The spell's name as get_game_table shows it" }
					},
					"required": ["caster_id", "name"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.dropSpell(arguments.requiredInt(CASTER_ID),
				arguments.requiredString(NAME))));
	}

	/** The combatants an operation names, by one id or by a list of them. */
	private static List<Integer> combatantIds(Arguments arguments)
	{
		return arguments.has(COMBATANT_ID) ? List.of(arguments.requiredInt(COMBATANT_ID))
			: arguments.optionalIntList("combatant_ids");
	}

	private static OptionalInt optionalValue(Arguments arguments)
	{
		return arguments.has("value") ? OptionalInt.of(arguments.requiredInt("value")) : OptionalInt.empty();
	}

	private static Operation listConditions(GameTableService service)
	{
		return Operation.exclusive("list_conditions",
			"List the conditions, temporary bonuses and negative properties the data holds that can be laid on "
				+ "a combatant, by the name add_condition takes.",
			COMBATANT_ONLY_SCHEMA,
			arguments -> arguments.decodeThen(() -> service.listConditions(arguments.requiredInt(COMBATANT_ID))));
	}

	private static Operation delayTurn(GameTableService service)
	{
		return Operation.exclusive("delay_turn",
			"Step a combatant out of the order; its turn, when it was acting, passes on." + STATE,
			COMBATANT_ONLY_SCHEMA,
			arguments -> arguments.decodeThen(() -> service.delay(arguments.requiredInt(COMBATANT_ID))));
	}

	private static Operation resumeTurn(GameTableService service)
	{
		return Operation.exclusive("resume_turn",
			"Let a delaying combatant act now, just before the turn it interrupts, with the initiative of "
				+ "whoever acted last." + STATE,
			COMBATANT_ONLY_SCHEMA,
			arguments -> arguments.decodeThen(() -> service.resume(arguments.requiredInt(COMBATANT_ID))));
	}

	private static Operation moveCombatant(GameTableService service)
	{
		return Operation.exclusive("move_combatant",
			"Move a combatant to another place in the order, as the game master settles a tie; it takes the "
				+ "initiative of its new neighbour." + STATE,
			"""
				{
					"type": "object",
					"properties": {
						"combatant_id": { "type": "integer" },
						"index": { "type": "integer", "description": "Its new place, 0 for first" }
					},
					"required": ["combatant_id", "index"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.move(arguments.requiredInt(COMBATANT_ID),
				arguments.requiredInt("index"))));
	}

	private static Operation removeCombatant(GameTableService service)
	{
		return Operation.exclusive("remove_combatant",
			"Take a combatant out of the fight; its turn, when it was acting, passes on." + STATE,
			COMBATANT_ONLY_SCHEMA,
			arguments -> arguments.decodeThen(() -> service.remove(arguments.requiredInt(COMBATANT_ID))));
	}

	private static Operation endCombat(GameTableService service)
	{
		return Operation.exclusive("end_combat",
			"End the fight and clear it. Answers what its monsters were worth, the challenge rating that amounts "
				+ "to, and each player's share; character sheets are left untouched.",
			NO_ARGUMENT_SCHEMA, arguments -> service.endCombat());
	}

	private static Operation getCombatant(GameTableService service)
	{
		return Operation.exclusive("get_combatant",
			"Read a combatant: for a monster, its numbers under its conditions (the three armor classes, attacks, "
				+ "saves, special abilities), challenge rating and templates; for a player's character with a "
				+ "file, its perception, sense motive and saves.",
			COMBATANT_ONLY_SCHEMA,
			arguments -> arguments.decodeThen(() -> service.getCombatant(arguments.requiredInt(COMBATANT_ID))));
	}

	private static Operation saveMonsterAsCharacter(GameTableService service)
	{
		return Operation.exclusive("save_monster_as_character",
			"Save a monster of the fight as a character file, without the fight's conditions, so that it can be "
				+ "opened with open_character and edited; the monster is rebuilt from that file from now on."
				+ STATE,
			"""
				{
					"type": "object",
					"properties": {
						"combatant_id": { "type": "integer" },
						"path": { "type": "string", "description": "Path of the .pcg file to write" }
					},
					"required": ["combatant_id", "path"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.saveMonsterAsCharacter(
				arguments.requiredInt(COMBATANT_ID), arguments.requiredString("path"))));
	}
}
