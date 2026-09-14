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

import pcgen.session.service.CharacterBuildService;
import pcgen.session.service.CharacterLifecycleService;

/**
 * The operations that open a character and shape them, each pairing the schema a
 * caller sends with the service call it stands for.
 */
public final class CharacterOperations
{
	private static final String CHARACTER_ID = "character_id";

	private CharacterOperations()
	{
	}

	public static List<Operation> of(CharacterLifecycleService lifecycle, CharacterBuildService build)
	{
		return List.of(
			createCharacter(lifecycle),
			getCharacter(lifecycle),
			openCharacter(lifecycle),
			saveCharacter(lifecycle),
			closeCharacter(lifecycle),
			setName(build),
			setRace(build),
			addClassLevel(build),
			setAbilityScore(build),
			setAlignment(build),
			setAllAbilityScores(build));
	}

	private static Operation createCharacter(CharacterLifecycleService service)
	{
		return Operation.exclusive("create_character",
			"Create a new character. Sources must be loaded first via load_sources.",
			"""
				{
					"type": "object",
					"properties": {
						"name": {
							"type": "string",
							"description": "Character name (optional)"
						}
					},
					"required": []
				}
				""",
			arguments -> service.createCharacter(arguments.optionalString("name", null)));
	}

	private static Operation getCharacter(CharacterLifecycleService service)
	{
		return Operation.exclusive("get_character",
			"Get a full summary of a character including stats, classes, race, alignment, HP, etc.",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": {
							"type": "string",
							"description": "Character ID (from create_character or open_character)"
						}
					},
					"required": ["character_id"]
				}
				""",
			arguments -> arguments.decodeThen(
				() -> service.getCharacter(arguments.requiredString(CHARACTER_ID))));
	}

	private static Operation openCharacter(CharacterLifecycleService service)
	{
		return Operation.exclusive("open_character",
			"Open an existing character from a .pcg file. Sources must be loaded first. "
				+ "If the character requires different sources (same game mode), sources are "
				+ "automatically merged and all open characters are reopened with new IDs.",
			"""
				{
					"type": "object",
					"properties": {
						"file_path": {
							"type": "string",
							"description": "Path to the .pcg character file"
						}
					},
					"required": ["file_path"]
				}
				""",
			arguments -> arguments.decodeThen(
				() -> service.openCharacter(arguments.requiredString("file_path"))));
	}

	private static Operation saveCharacter(CharacterLifecycleService service)
	{
		return Operation.exclusive("save_character",
			"Save a character to its .pcg file",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"file_path": {
							"type": "string",
							"description": "Path to save the file (optional, uses default if not set)"
						}
					},
					"required": ["character_id"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.saveCharacter(
				arguments.requiredString(CHARACTER_ID),
				arguments.optionalString("file_path", null))));
	}

	private static Operation closeCharacter(CharacterLifecycleService service)
	{
		return Operation.exclusive("close_character",
			"Close a character and release its resources",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" }
					},
					"required": ["character_id"]
				}
				""",
			arguments -> arguments.decodeThen(
				() -> service.closeCharacter(arguments.requiredString(CHARACTER_ID))));
	}

	private static Operation setName(CharacterBuildService service)
	{
		return Operation.exclusive("set_name",
			"Set a character's name",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"name": { "type": "string", "description": "New character name" }
					},
					"required": ["character_id", "name"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.setName(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString("name"))));
	}

	private static Operation setRace(CharacterBuildService service)
	{
		return Operation.exclusive("set_race",
			"Set a character's race",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"race_key": { "type": "string", "description": "Race key (from resources or list)" }
					},
					"required": ["character_id", "race_key"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.setRace(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString("race_key"))));
	}

	private static Operation addClassLevel(CharacterBuildService service)
	{
		return Operation.exclusive("add_class_level",
			"Add one or more levels of a class to a character",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"class_key": { "type": "string", "description": "Class key (from resources or list)" },
						"levels": { "type": "integer", "description": "Number of levels to add (default: 1)", "default": 1 }
					},
					"required": ["character_id", "class_key"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.addClassLevel(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString("class_key"),
				arguments.optionalInt("levels", 1))));
	}

	private static Operation setAbilityScore(CharacterBuildService service)
	{
		return Operation.exclusive("set_ability_score",
			"Set a base ability score (e.g., STR, DEX, CON, INT, WIS, CHA)",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"stat_key": { "type": "string", "description": "Stat key (e.g., STR, DEX)" },
						"score": { "type": "integer", "description": "Base score value" }
					},
					"required": ["character_id", "stat_key", "score"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.setAbilityScore(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString("stat_key"),
				arguments.requiredInt("score"))));
	}

	private static Operation setAlignment(CharacterBuildService service)
	{
		return Operation.exclusive("set_alignment",
			"Set a character's alignment (e.g., Lawful Good, Chaotic Neutral)",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"alignment_key": { "type": "string", "description": "Alignment key or name" }
					},
					"required": ["character_id", "alignment_key"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.setAlignment(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString("alignment_key"))));
	}

	private static Operation setAllAbilityScores(CharacterBuildService service)
	{
		return Operation.exclusive("set_all_ability_scores",
			"Set all 6 ability scores in one call.",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"scores": {
							"type": "object",
							"description": "Ability scores: {STR: int, DEX: int, CON: int, INT: int, WIS: int, CHA: int}",
							"properties": {
								"STR": { "type": "integer" },
								"DEX": { "type": "integer" },
								"CON": { "type": "integer" },
								"INT": { "type": "integer" },
								"WIS": { "type": "integer" },
								"CHA": { "type": "integer" }
							}
						}
					},
					"required": ["character_id", "scores"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.setAllAbilityScores(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredIntMap("scores"))));
	}
}
