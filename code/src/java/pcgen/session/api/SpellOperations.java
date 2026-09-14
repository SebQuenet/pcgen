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

import java.util.ArrayList;
import java.util.List;

import pcgen.session.service.SpellService;

/**
 * The operations over a caster's spells: what can be learned, what is known,
 * what is prepared, and what is written down.
 */
public final class SpellOperations
{
	private static final String CHARACTER_ID = "character_id";
	private static final String SPELL_NAME = "spell_name";
	private static final String SPELL_LIST = "spell_list";
	private static final String CLASS_KEY = "class_key";

	private static final String CHARACTER_ONLY_SCHEMA = """
		{
			"type": "object",
			"properties": {
				"character_id": { "type": "string", "description": "Character ID" }
			},
			"required": ["character_id"]
		}
		""";

	private SpellOperations()
	{
	}

	public static List<Operation> of(SpellService service)
	{
		return List.of(
			getAvailableSpells(service),
			getKnownSpells(service),
			addKnownSpell(service),
			removeKnownSpell(service),
			getPreparedSpells(service),
			addPreparedSpell(service),
			removePreparedSpell(service),
			getSpellbooks(service),
			addToSpellbook(service),
			batchAddPreparedSpells(service));
	}

	private static Operation getAvailableSpells(SpellService service)
	{
		return Operation.exclusive("get_available_spells",
			"List spells available for a character to learn, optionally filtered by class and level",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"class_filter": { "type": "string", "description": "Filter by spellcasting class name (optional)" },
						"level_filter": { "type": "string", "description": "Filter by spell level (optional, e.g. '0', '1', '2')" }
					},
					"required": ["character_id"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.getAvailableSpells(
				arguments.requiredString(CHARACTER_ID),
				arguments.optionalString("class_filter", null),
				arguments.optionalString("level_filter", null))));
	}

	private static Operation getKnownSpells(SpellService service)
	{
		return Operation.exclusive("get_known_spells",
			"List all spells known by a character",
			CHARACTER_ONLY_SCHEMA,
			arguments -> arguments.decodeThen(
				() -> service.getKnownSpells(arguments.requiredString(CHARACTER_ID))));
	}

	private static Operation addKnownSpell(SpellService service)
	{
		return Operation.exclusive("add_known_spell",
			"Add a spell to a character's known spells",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"spell_name": { "type": "string", "description": "Spell name or key" },
						"class_key": { "type": "string", "description": "Spellcasting class (optional, auto-detected if only one)" },
						"spell_level": { "type": "string", "description": "Spell level (optional, auto-detected)" }
					},
					"required": ["character_id", "spell_name"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.addKnownSpell(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString(SPELL_NAME),
				arguments.optionalString(CLASS_KEY, null),
				arguments.optionalString("spell_level", null))));
	}

	private static Operation removeKnownSpell(SpellService service)
	{
		return Operation.exclusive("remove_known_spell",
			"Remove a spell from a character's known spells",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"spell_name": { "type": "string", "description": "Spell name or key" }
					},
					"required": ["character_id", "spell_name"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.removeKnownSpell(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString(SPELL_NAME))));
	}

	private static Operation getPreparedSpells(SpellService service)
	{
		return Operation.exclusive("get_prepared_spells",
			"List all prepared spells for a character",
			CHARACTER_ONLY_SCHEMA,
			arguments -> arguments.decodeThen(
				() -> service.getPreparedSpells(arguments.requiredString(CHARACTER_ID))));
	}

	private static Operation addPreparedSpell(SpellService service)
	{
		return Operation.exclusive("add_prepared_spell",
			"Prepare a known spell (add to prepared spell list)",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"spell_name": { "type": "string", "description": "Spell name" },
						"spell_list": { "type": "string", "description": "Prepared spell list name (optional, uses default)" },
						"class_key": { "type": "string", "description": "Spellcasting class (optional)" }
					},
					"required": ["character_id", "spell_name"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.addPreparedSpell(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString(SPELL_NAME),
				arguments.optionalString(SPELL_LIST, null),
				arguments.optionalString(CLASS_KEY, null))));
	}

	private static Operation removePreparedSpell(SpellService service)
	{
		return Operation.exclusive("remove_prepared_spell",
			"Remove a spell from prepared spell list",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"spell_name": { "type": "string", "description": "Spell name" },
						"spell_list": { "type": "string", "description": "Prepared spell list name (optional, uses default)" }
					},
					"required": ["character_id", "spell_name"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.removePreparedSpell(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString(SPELL_NAME),
				arguments.optionalString(SPELL_LIST, null))));
	}

	private static Operation getSpellbooks(SpellService service)
	{
		return Operation.exclusive("get_spellbooks",
			"List spellbooks and their contents",
			CHARACTER_ONLY_SCHEMA,
			arguments -> arguments.decodeThen(
				() -> service.getSpellbooks(arguments.requiredString(CHARACTER_ID))));
	}

	private static Operation addToSpellbook(SpellService service)
	{
		return Operation.exclusive("add_to_spellbook",
			"Add a known spell to a spellbook",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"spell_name": { "type": "string", "description": "Spell name" },
						"spellbook": { "type": "string", "description": "Spellbook name" }
					},
					"required": ["character_id", "spell_name", "spellbook"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.addToSpellbook(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString(SPELL_NAME),
				arguments.requiredString("spellbook"))));
	}

	private static Operation batchAddPreparedSpells(SpellService service)
	{
		return Operation.exclusive("batch_add_prepared_spells",
			"Prepare multiple spells in one call.",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"spells": {
							"type": "array",
							"description": "List of spells: [{spell_name, class_key?, spell_list?}]",
							"items": {
								"type": "object",
								"properties": {
									"spell_name": { "type": "string" },
									"class_key": { "type": "string" },
									"spell_list": { "type": "string" }
								},
								"required": ["spell_name"]
							}
						}
					},
					"required": ["character_id", "spells"]
				}
				""",
			arguments -> arguments.decodeThen(() -> {
				String characterId = arguments.requiredString(CHARACTER_ID);
				List<SpellService.PreparedSpellRequest> requests = new ArrayList<>();
				for (Arguments entry : arguments.requiredEntryList("spells"))
				{
					requests.add(new SpellService.PreparedSpellRequest(
						entry.requiredString(SPELL_NAME),
						entry.optionalString(CLASS_KEY, null),
						entry.optionalString(SPELL_LIST, null)));
				}
				return service.batchAddPreparedSpells(characterId, List.copyOf(requests));
			}));
	}
}
