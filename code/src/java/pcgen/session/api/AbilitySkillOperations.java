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

import pcgen.session.service.AbilityService;
import pcgen.session.service.SkillService;

/**
 * The operations that spend a character's feat slots and skill points.
 */
public final class AbilitySkillOperations
{
	private static final String CHARACTER_ID = "character_id";
	private static final String CATEGORY_KEY = "category_key";
	private static final String ABILITY_KEY = "ability_key";

	private AbilitySkillOperations()
	{
	}

	public static List<Operation> of(AbilityService abilities, SkillService skills)
	{
		return List.of(
			listAbilityCategories(abilities),
			listAbilities(abilities),
			addAbility(abilities),
			removeAbility(abilities),
			batchAddAbilities(abilities),
			investSkillPoints(skills),
			getSkillSummary(skills),
			batchInvestSkills(skills));
	}

	private static Operation listAbilityCategories(AbilityService service)
	{
		return Operation.exclusive("list_ability_categories",
			"List active ability categories for a character (e.g., Feat, Trait, Class Ability)",
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
				() -> service.listAbilityCategories(arguments.requiredString(CHARACTER_ID))));
	}

	private static Operation listAbilities(AbilityService service)
	{
		return Operation.exclusive("list_abilities",
			"List available abilities in a category from the data set",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"category_key": { "type": "string", "description": "Ability category key (from list_ability_categories)" }
					},
					"required": ["character_id", "category_key"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.listAbilities(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString(CATEGORY_KEY))));
	}

	private static Operation addAbility(AbilityService service)
	{
		return Operation.exclusive("add_ability",
			"Add an ability (feat, trait, etc.) to a character. If the ability requires a choice "
				+ "(e.g., Weapon Focus requires choosing a weapon), pass it in the 'choice' parameter.",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"category_key": { "type": "string", "description": "Ability category key" },
						"ability_key": { "type": "string", "description": "Ability key or name" },
						"choice": {
							"type": "array",
							"items": { "type": "string" },
							"description": "Choices for abilities that need one, e.g. ['Longbow'] for Weapon Focus. Left out, the first option is taken."
						}
					},
					"required": ["character_id", "category_key", "ability_key"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.addAbility(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString(CATEGORY_KEY),
				arguments.requiredString(ABILITY_KEY),
				arguments.optionalStringList("choice"))));
	}

	private static Operation removeAbility(AbilityService service)
	{
		return Operation.exclusive("remove_ability",
			"Remove an ability from a character",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"category_key": { "type": "string", "description": "Ability category key" },
						"ability_key": { "type": "string", "description": "Ability key or name" }
					},
					"required": ["character_id", "category_key", "ability_key"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.removeAbility(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString(CATEGORY_KEY),
				arguments.requiredString(ABILITY_KEY))));
	}

	private static Operation batchAddAbilities(AbilityService service)
	{
		return Operation.exclusive("batch_add_abilities",
			"Add multiple abilities (feats, traits, FCB, etc.) in one call.",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"abilities": {
							"type": "array",
							"description": "List of abilities: [{category_key, ability_key, choice?}]",
							"items": {
								"type": "object",
								"properties": {
									"category_key": { "type": "string" },
									"ability_key": { "type": "string" },
									"choice": { "type": "array", "items": { "type": "string" } }
								},
								"required": ["category_key", "ability_key"]
							}
						}
					},
					"required": ["character_id", "abilities"]
				}
				""",
			arguments -> arguments.decodeThen(() -> {
				String characterId = arguments.requiredString(CHARACTER_ID);
				List<AbilityService.AbilityRequest> requests = new ArrayList<>();
				for (Arguments entry : arguments.requiredEntryList("abilities"))
				{
					requests.add(new AbilityService.AbilityRequest(
						entry.requiredString(CATEGORY_KEY),
						entry.requiredString(ABILITY_KEY),
						entry.optionalStringList("choice")));
				}
				return service.batchAddAbilities(characterId, List.copyOf(requests));
			}));
	}

	private static Operation investSkillPoints(SkillService service)
	{
		return Operation.exclusive("invest_skill_points",
			"Invest skill points in a skill at a specific character level. "
				+ "For skills with a CHOOSE token (e.g. Linguistics, Craft, Perform, Profession), "
				+ "provide the 'choices' parameter to select what the skill rank grants.",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"skill_key": { "type": "string", "description": "Skill key or name" },
						"points": { "type": "integer", "description": "Number of skill points to invest" },
						"level_index": { "type": "integer", "description": "Character level index (0-based, defaults to latest level)", "default": -1 },
						"choices": {
							"type": "array",
							"items": { "type": "string" },
							"description": "Choices for CHOOSE-based skills (e.g. language name for Linguistics). Required for skills like Linguistics, Craft, Perform, Profession."
						}
					},
					"required": ["character_id", "skill_key", "points"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.investSkillPoints(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString("skill_key"),
				arguments.requiredInt("points"),
				arguments.optionalInt("level_index", -1),
				arguments.optionalStringList("choices"))));
	}

	private static Operation getSkillSummary(SkillService service)
	{
		return Operation.exclusive("get_skill_summary",
			"Get skill ranks, modifiers, and remaining points for a character",
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
				() -> service.getSkillSummary(arguments.requiredString(CHARACTER_ID))));
	}

	private static Operation batchInvestSkills(SkillService service)
	{
		return Operation.exclusive("batch_invest_skills",
			"Invest skill points across multiple levels in one call. Each entry specifies a skill and points per level. "
				+ "For CHOOSE-based skills (Linguistics, Craft, Perform, Profession), include 'choices' in the entry.",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"investments": {
							"type": "array",
							"description": "List of skill investments: [{skill_key, points, level_index, choices?}]",
							"items": {
								"type": "object",
								"properties": {
									"skill_key": { "type": "string" },
									"points": { "type": "integer" },
									"level_index": { "type": "integer" },
									"choices": {
										"type": "array",
										"items": { "type": "string" },
										"description": "Choices for CHOOSE-based skills (e.g. language name for Linguistics)"
									}
								},
								"required": ["skill_key", "points", "level_index"]
							}
						}
					},
					"required": ["character_id", "investments"]
				}
				""",
			arguments -> arguments.decodeThen(() -> {
				String characterId = arguments.requiredString(CHARACTER_ID);
				List<SkillService.SkillInvestmentRequest> requests = new ArrayList<>();
				for (Arguments entry : arguments.requiredEntryList("investments"))
				{
					requests.add(new SkillService.SkillInvestmentRequest(
						entry.requiredString("skill_key"),
						entry.requiredInt("points"),
						entry.requiredInt("level_index"),
						entry.optionalStringList("choices")));
				}
				return service.batchInvestSkills(characterId, List.copyOf(requests));
			}));
	}
}
