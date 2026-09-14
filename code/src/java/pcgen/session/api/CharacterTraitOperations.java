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

import pcgen.session.service.DeityDomainService;
import pcgen.session.service.LanguageCompanionService;
import pcgen.session.service.TemplateService;

/**
 * The operations for what a character worships, what is laid over them, what
 * they speak, and what follows them.
 */
public final class CharacterTraitOperations
{
	private static final String CHARACTER_ID = "character_id";
	private static final String DOMAIN_KEY = "domain_key";
	private static final String TEMPLATE_KEY = "template_key";
	private static final String BONUS_NAME = "bonus_name";
	private static final String LANGUAGE_KEY = "language_key";
	private static final String COMPANION_TYPE = "companion_type";
	private static final String COMPANION_RACE = "companion_race";

	private static final String CHARACTER_ONLY_SCHEMA = """
		{ "type": "object", "properties": { "character_id": { "type": "string" } }, "required": ["character_id"] }
		""";

	private CharacterTraitOperations()
	{
	}

	public static List<Operation> of(DeityDomainService deities, TemplateService templates,
		LanguageCompanionService languages)
	{
		return List.of(
			setDeity(deities),
			addDomain(deities),
			removeDomain(deities),
			addTemplate(templates),
			removeTemplate(templates),
			getTemplates(templates),
			addTempBonus(templates),
			removeTempBonus(templates),
			listTempBonuses(templates),
			addKit(templates),
			getLanguages(languages),
			addLanguage(languages),
			removeLanguage(languages),
			getCompanions(languages),
			addCompanion(languages),
			removeCompanion(languages));
	}

	private static Operation setDeity(DeityDomainService service)
	{
		return Operation.exclusive("set_deity",
			"Set a character's deity",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"deity_key": { "type": "string", "description": "Deity key or name" }
					},
					"required": ["character_id", "deity_key"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.setDeity(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString("deity_key"))));
	}

	private static Operation addDomain(DeityDomainService service)
	{
		return Operation.exclusive("add_domain",
			"Add a domain to a character (must have deity set and domain selections remaining)",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"domain_key": { "type": "string", "description": "Domain key or name" }
					},
					"required": ["character_id", "domain_key"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.addDomain(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString(DOMAIN_KEY))));
	}

	private static Operation removeDomain(DeityDomainService service)
	{
		return Operation.exclusive("remove_domain",
			"Remove a domain from a character",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"domain_key": { "type": "string", "description": "Domain key or name" }
					},
					"required": ["character_id", "domain_key"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.removeDomain(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString(DOMAIN_KEY))));
	}

	private static Operation addTemplate(TemplateService service)
	{
		return Operation.exclusive("add_template",
			"Apply a character template (e.g., Half-Dragon, Lycanthrope)",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string" },
						"template_key": { "type": "string", "description": "Template key or name" }
					},
					"required": ["character_id", "template_key"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.addTemplate(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString(TEMPLATE_KEY))));
	}

	private static Operation removeTemplate(TemplateService service)
	{
		return Operation.exclusive("remove_template",
			"Remove a template from a character",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string" },
						"template_key": { "type": "string" }
					},
					"required": ["character_id", "template_key"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.removeTemplate(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString(TEMPLATE_KEY))));
	}

	private static Operation getTemplates(TemplateService service)
	{
		return Operation.exclusive("get_templates",
			"List templates applied to a character",
			CHARACTER_ONLY_SCHEMA,
			arguments -> arguments.decodeThen(
				() -> service.getTemplates(arguments.requiredString(CHARACTER_ID))));
	}

	private static Operation addTempBonus(TemplateService service)
	{
		return Operation.exclusive("add_temp_bonus",
			"Apply a temporary bonus to a character (e.g., Bull's Strength, Haste)",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string" },
						"bonus_name": { "type": "string", "description": "Temporary bonus name" }
					},
					"required": ["character_id", "bonus_name"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.addTempBonus(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString(BONUS_NAME))));
	}

	private static Operation removeTempBonus(TemplateService service)
	{
		return Operation.exclusive("remove_temp_bonus",
			"Remove a temporary bonus from a character",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string" },
						"bonus_name": { "type": "string" }
					},
					"required": ["character_id", "bonus_name"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.removeTempBonus(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString(BONUS_NAME))));
	}

	private static Operation listTempBonuses(TemplateService service)
	{
		return Operation.exclusive("list_temp_bonuses",
			"List available and active temporary bonuses for a character",
			CHARACTER_ONLY_SCHEMA,
			arguments -> arguments.decodeThen(
				() -> service.listTempBonuses(arguments.requiredString(CHARACTER_ID))));
	}

	private static Operation addKit(TemplateService service)
	{
		return Operation.exclusive("add_kit",
			"Apply a character kit (pre-built equipment/ability package)",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string" },
						"kit_key": { "type": "string", "description": "Kit key or name" }
					},
					"required": ["character_id", "kit_key"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.addKit(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString("kit_key"))));
	}

	private static Operation getLanguages(LanguageCompanionService service)
	{
		return Operation.exclusive("get_languages",
			"List a character's known languages and available language choosers",
			CHARACTER_ONLY_SCHEMA,
			arguments -> arguments.decodeThen(
				() -> service.getLanguages(arguments.requiredString(CHARACTER_ID))));
	}

	private static Operation addLanguage(LanguageCompanionService service)
	{
		return Operation.exclusive("add_language",
			"Add a bonus language to a character by selecting from an available language chooser",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"language_key": { "type": "string", "description": "Language name or key (from get_languages chooser available list)" }
					},
					"required": ["character_id", "language_key"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.addLanguage(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString(LANGUAGE_KEY))));
	}

	private static Operation removeLanguage(LanguageCompanionService service)
	{
		return Operation.exclusive("remove_language",
			"Remove a bonus language from a character",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string" },
						"language_key": { "type": "string", "description": "Language name or key" }
					},
					"required": ["character_id", "language_key"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.removeLanguage(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString(LANGUAGE_KEY))));
	}

	private static Operation getCompanions(LanguageCompanionService service)
	{
		return Operation.exclusive("get_companions",
			"List a character's companions (familiars, animal companions, cohorts) and available companion types",
			CHARACTER_ONLY_SCHEMA,
			arguments -> arguments.decodeThen(
				() -> service.getCompanions(arguments.requiredString(CHARACTER_ID))));
	}

	private static Operation addCompanion(LanguageCompanionService service)
	{
		return Operation.exclusive("add_companion",
			"Add an animal companion, familiar, or mount to a character by selecting from available companion races",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"companion_type": { "type": "string", "description": "Companion type (e.g., 'Animal Companion', 'Familiar')" },
						"companion_race": { "type": "string", "description": "Race of the companion (from get_companions available list)" }
					},
					"required": ["character_id", "companion_type", "companion_race"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.addCompanion(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString(COMPANION_TYPE),
				arguments.requiredString(COMPANION_RACE))));
	}

	private static Operation removeCompanion(LanguageCompanionService service)
	{
		return Operation.exclusive("remove_companion",
			"Remove a companion (animal companion, familiar, mount, follower) from a character",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"companion_type": { "type": "string", "description": "Companion type (e.g., 'Animal Companion', 'Familiar', 'Follower')" },
						"companion_race": { "type": "string", "description": "Race of the companion to remove (optional, uses first match if omitted)" }
					},
					"required": ["character_id", "companion_type"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.removeCompanion(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString(COMPANION_TYPE),
				arguments.optionalString(COMPANION_RACE, null))));
	}
}
