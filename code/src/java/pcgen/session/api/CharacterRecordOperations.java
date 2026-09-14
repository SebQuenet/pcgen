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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pcgen.session.service.BiographyService;
import pcgen.session.service.ChoiceService;
import pcgen.session.service.ExportService;
import pcgen.session.service.TacticalSheetService;
import pcgen.session.service.UtilityService;

/**
 * The operations that describe a character, plan their fights, render their
 * sheet, and answer the questions PCGen stops to ask.
 */
public final class CharacterRecordOperations
{
	private static final String CHARACTER_ID = "character_id";

	private static final String CHARACTER_ONLY_SCHEMA = """
		{ "type": "object", "properties": { "character_id": { "type": "string" } }, "required": ["character_id"] }
		""";

	private static final List<String> BIOGRAPHY_FIELDS = List.of(
		"gender", "age", "weight", "hair_color", "eye_color", "skin_color", "handed", "players_name",
		"bio", "description", "companions", "assets", "magic", "gm_notes");

	private static final String SYNTAX = """
		A plan is line oriented text.

		'## title' opens a section. At column zero, one of these opens a block:
		  step: <when>              then '  do: <what to do>' and optionally '  note: <detail>'
		  note: <title>             then one or more indented lines of prose
		  resource: <label> | <maximum> | <action>      action is optional
		  attack: <@weapon(name)>   then optionally '  target: <kind> | <what changes>' and '  note: <detail>'
		  attack: <name> | <to hit> | <damage> | <crit>  all four fields when written out
		  creature: <name> | <source> | <duration>      then one or more '  row: <heading> | <contents>'
		  spells: @prepared                             then optionally '  tag: <spell> | <tag>, <tag>'

		Fields are separated by '|'. Two spaces of indentation attach a line to the
		block above it. In a single line field, the two characters \\n mean a line break.

		'@weapon(name)' and '@var(name)' read the character's own numbers, so the sheet
		follows a level up without being rewritten. Call list_tactical_references to see
		what those names may be. Anything else is written out as text.
		""";

	private CharacterRecordOperations()
	{
	}

	public static List<Operation> of(BiographyService biography, TacticalSheetService tactics,
		ExportService export, ChoiceService choices, UtilityService utilities)
	{
		return List.of(
			setBiography(biography),
			getBiography(biography),
			setXp(biography),
			setTacticalSheet(tactics),
			getTacticalSheet(tactics),
			clearTacticalSheet(tactics),
			listTacticalReferences(tactics),
			exportCharacter(export),
			getCharacterSheet(export),
			getPendingChoices(choices),
			resolveChoice(choices),
			getTodoList(utilities),
			isQualifiedFor(utilities),
			rollStats(utilities),
			isDirty(utilities),
			getCharacterDetails(utilities));
	}

	private static Operation setBiography(BiographyService service)
	{
		return Operation.exclusive("set_biography",
			"Set biography fields for a character. Supports basic fields (gender, age, etc.) "
				+ "and text note fields (bio, description, companions, assets, magic, gm_notes).",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"gender": { "type": "string", "description": "Gender (Male, Female, etc.)" },
						"age": { "type": "integer", "description": "Age in years" },
						"weight": { "type": "integer", "description": "Weight in pounds" },
						"hair_color": { "type": "string", "description": "Hair color" },
						"eye_color": { "type": "string", "description": "Eye color" },
						"skin_color": { "type": "string", "description": "Skin color" },
						"handed": { "type": "string", "description": "Handedness (Right, Left, Ambidextrous)" },
						"players_name": { "type": "string", "description": "Player's real name" },
						"bio": { "type": "string", "description": "Character biography / backstory text" },
						"description": { "type": "string", "description": "Physical description of the character" },
						"companions": { "type": "string", "description": "Notes about companions and allies" },
						"assets": { "type": "string", "description": "Notes about other assets and possessions" },
						"magic": { "type": "string", "description": "Notes about magic items and effects" },
						"gm_notes": { "type": "string", "description": "GM-only notes" }
					},
					"required": ["character_id"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.setBiography(
				arguments.requiredString(CHARACTER_ID), biographyChanges(arguments))));
	}

	/** Only the biography fields the caller actually sent, so the rest are left alone. */
	private static Map<String, Object> biographyChanges(Arguments arguments)
	{
		Map<String, Object> changes = new LinkedHashMap<>();
		for (String field : BIOGRAPHY_FIELDS)
		{
			if (!arguments.has(field))
			{
				continue;
			}
			if ("age".equals(field) || "weight".equals(field))
			{
				changes.put(field, arguments.requiredInt(field));
			}
			else
			{
				changes.put(field, arguments.requiredString(field));
			}
		}
		return changes;
	}

	private static Operation getBiography(BiographyService service)
	{
		return Operation.exclusive("get_biography",
			"Get biography details for a character, including basic fields and text notes "
				+ "(bio, description, companions, assets, magic, gm_notes)",
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
				() -> service.getBiography(arguments.requiredString(CHARACTER_ID))));
	}

	private static Operation setXp(BiographyService service)
	{
		return Operation.exclusive("set_xp",
			"Set a character's experience points",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"xp": { "type": "integer", "description": "Total XP value" }
					},
					"required": ["character_id", "xp"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.setExperience(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredInt("xp"))));
	}

	private static Operation setTacticalSheet(TacticalSheetService service)
	{
		return Operation.exclusive("set_tactical_sheet",
			"Write the tactical sheet of a character, as source text. Refuses text that does not read, "
				+ "naming the offending line.\n\n" + SYNTAX,
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"source": { "type": "string", "description": "The plan's source text" }
					},
					"required": ["character_id", "source"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.setTacticalSheet(
				arguments.requiredString(CHARACTER_ID),
				arguments.optionalString("source", ""))));
	}

	private static Operation getTacticalSheet(TacticalSheetService service)
	{
		return Operation.exclusive("get_tactical_sheet",
			"Get the tactical sheet of a character as source text, together with what the session has used up.",
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
				() -> service.getTacticalSheet(arguments.requiredString(CHARACTER_ID))));
	}

	private static Operation clearTacticalSheet(TacticalSheetService service)
	{
		return Operation.exclusive("clear_tactical_sheet",
			"Remove the tactical sheet of a character.",
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
				() -> service.clearTacticalSheet(arguments.requiredString(CHARACTER_ID))));
	}

	private static Operation listTacticalReferences(TacticalSheetService service)
	{
		return Operation.exclusive("list_tactical_references",
			"List what a tactical sheet may point at with @weapon(...) and @var(...), plus the spells "
				+ "a spells block would show. Use these names verbatim.",
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
				() -> service.listTacticalReferences(arguments.requiredString(CHARACTER_ID))));
	}

	private static Operation exportCharacter(ExportService service)
	{
		return Operation.exclusive("export_character",
			"Export a character to PDF, HTML, or other formats using a template",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"template_path": { "type": "string", "description": "Path to the export template file (.xslt for PDF, .ftl for HTML/text)" },
						"output_path": { "type": "string", "description": "Path for the output file" }
					},
					"required": ["character_id", "template_path", "output_path"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.exportCharacter(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString("template_path"),
				arguments.requiredString("output_path"))));
	}

	private static Operation getCharacterSheet(ExportService service)
	{
		return Operation.exclusive("get_character_sheet",
			"Export a character sheet as inline content in markdown, toon, json, or xml format. "
				+ "Returns the rendered content directly.",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"format": { "type": "string", "description": "Output format: markdown (default), toon, json, xml", "default": "markdown" }
					},
					"required": ["character_id"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.getCharacterSheet(
				arguments.requiredString(CHARACTER_ID),
				arguments.optionalString("format", ExportService.defaultFormat()))));
	}

	private static Operation getPendingChoices(ChoiceService service)
	{
		return Operation.concurrent("get_pending_choices",
			"Get all pending choices that need to be resolved (e.g., skill selection for Skill Focus feat)",
			"""
				{
					"type": "object",
					"properties": {},
					"required": []
				}
				""",
			arguments -> service.getPendingChoices());
	}

	private static Operation resolveChoice(ChoiceService service)
	{
		return Operation.concurrent("resolve_choice",
			"Resolve a pending choice by providing selections (e.g., choose a skill for Skill Focus)",
			"""
				{
					"type": "object",
					"properties": {
						"choice_id": {
							"type": "string",
							"description": "ID of the pending choice (from get_pending_choices)"
						},
						"selections": {
							"type": "array",
							"items": { "type": "string" },
							"description": "List of selected option names"
						}
					},
					"required": ["choice_id", "selections"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.resolveChoice(
				arguments.requiredString("choice_id"),
				arguments.requiredStringList("selections"))));
	}

	private static Operation getTodoList(UtilityService service)
	{
		return Operation.exclusive("get_todo_list",
			"Get the character's TODO list - things that still need to be done "
				+ "(e.g., select feats, assign skill points)",
			CHARACTER_ONLY_SCHEMA,
			arguments -> arguments.decodeThen(
				() -> service.getTodoList(arguments.requiredString(CHARACTER_ID))));
	}

	private static Operation isQualifiedFor(UtilityService service)
	{
		return Operation.exclusive("is_qualified_for",
			"Check if a character meets the prerequisites for a race, class, feat, deity, template, or kit",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string" },
						"item_type": { "type": "string", "description": "Type: race, class, deity, template, kit" },
						"item_key": { "type": "string", "description": "Item key or name to check" }
					},
					"required": ["character_id", "item_type", "item_key"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.isQualifiedFor(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString("item_type"),
				arguments.requiredString("item_key"))));
	}

	private static Operation rollStats(UtilityService service)
	{
		return Operation.exclusive("roll_stats",
			"Roll ability scores using the game mode's rolling method",
			CHARACTER_ONLY_SCHEMA,
			arguments -> arguments.decodeThen(
				() -> service.rollStats(arguments.requiredString(CHARACTER_ID))));
	}

	private static Operation isDirty(UtilityService service)
	{
		return Operation.exclusive("is_dirty",
			"Check if a character has unsaved changes",
			CHARACTER_ONLY_SCHEMA,
			arguments -> arguments.decodeThen(
				() -> service.isDirty(arguments.requiredString(CHARACTER_ID))));
	}

	private static Operation getCharacterDetails(UtilityService service)
	{
		return Operation.exclusive("get_character_details",
			"Get detailed computed stats: HP, AC-related weight/load, wealth, stat totals/modifiers",
			CHARACTER_ONLY_SCHEMA,
			arguments -> arguments.decodeThen(
				() -> service.getCharacterDetails(arguments.requiredString(CHARACTER_ID))));
	}
}
