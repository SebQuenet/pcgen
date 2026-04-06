package pcgen.mcp.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import pcgen.core.AbilityCategory;
import pcgen.facade.core.AbilityFacade;
import pcgen.facade.core.CharacterFacade;
import pcgen.mcp.McpSessionManager;
import pcgen.mcp.McpUIDelegate;

public final class AbilityTools
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private AbilityTools()
	{
	}

	public static SyncToolSpecification listAbilityCategories(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("list_ability_categories",
				"List active ability categories for a character (e.g., Feat, Trait, Class Ability)",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" }
						},
						"required": ["character_id"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					List<Map<String, Object>> categories = new ArrayList<>();
					java.util.Set<String> seen = new java.util.HashSet<>();
					for (AbilityCategory cat : character.getActiveAbilityCategories())
					{
						Map<String, Object> catMap = new LinkedHashMap<>();
						catMap.put("key", cat.getKeyName());
						catMap.put("name", cat.getDisplayName());
						catMap.put("remaining", character.getRemainingSelections(cat));
						catMap.put("total", character.getTotalSelections(cat));
						categories.add(catMap);
						seen.add(cat.getKeyName());
					}
					// Also include dataset categories with pool > 0 that are not
					// in activeAbilityCategories (e.g. mythic path categories in headless mode)
					for (AbilityCategory cat : character.getDataSet().getAbilities().getKeys())
					{
						if (!seen.contains(cat.getKeyName()))
						{
							int remaining = character.getRemainingSelections(cat);
							int total = character.getTotalSelections(cat);
							if (total > 0)
							{
								Map<String, Object> catMap = new LinkedHashMap<>();
								catMap.put("key", cat.getKeyName());
								catMap.put("name", cat.getDisplayName());
								catMap.put("remaining", remaining);
								catMap.put("total", total);
								categories.add(catMap);
								seen.add(cat.getKeyName());
							}
						}
					}
					return toResult(categories);
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification listAbilities(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("list_abilities",
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
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					String categoryKey = (String) args.get("category_key");

					AbilityCategory found = findCategory(character, categoryKey);
					if (found == null)
					{
						return errorResult("Ability category not found: " + categoryKey);
					}

					var dataSetAbilities = character.getDataSet().getAbilities();
					var abilityList = dataSetAbilities.getValue(found);
					List<Map<String, Object>> result = new ArrayList<>();
					if (abilityList != null)
					{
						for (AbilityFacade ability : abilityList)
						{
							Map<String, Object> map = new LinkedHashMap<>();
							map.put("key", ability.getKeyName());
							map.put("name", ability.toString());
							map.put("qualified", character.isQualifiedFor(ability));
							result.add(map);
						}
					}
					return toResult(result);
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification addAbility(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("add_ability",
				"Add an ability (feat, trait, etc.) to a character. If the ability requires a choice (e.g., Weapon Focus requires choosing a weapon), pass it in the 'choice' parameter.",
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
								"description": "Pre-selected choices for abilities that require selection (e.g., ['Longbow'] for Weapon Focus). If the ability requires a choice and this is not provided, the first available option will be auto-selected."
							}
						},
						"required": ["character_id", "category_key", "ability_key"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					String characterId = (String) args.get("character_id");
					CharacterFacade character = session.getCharacter(characterId);
					String categoryKey = (String) args.get("category_key");
					String abilityKey = (String) args.get("ability_key");
					@SuppressWarnings("unchecked")
					List<String> choice = (List<String>) args.get("choice");

					AbilityCategory category = findCategory(character, categoryKey);
					if (category == null)
					{
						return errorResult("Ability category not found: " + categoryKey);
					}

					var abilityList = character.getDataSet().getAbilities().getValue(category);
					AbilityFacade found = null;
					if (abilityList != null)
					{
						for (AbilityFacade ability : abilityList)
						{
							if (ability.getKeyName().equalsIgnoreCase(abilityKey) || ability.toString().equalsIgnoreCase(abilityKey))
							{
								found = ability;
								break;
							}
						}
					}
					if (found == null)
					{
						return errorResult("Ability not found: " + abilityKey);
					}

					McpUIDelegate delegate = session.getDelegate(characterId);

					// If the ability requires a choice and none was provided,
					// capture the available options and return them without adding the ability.
					if (found.isMult() && (choice == null || choice.isEmpty()))
					{
						if (delegate != null)
						{
							delegate.enableCaptureMode();
						}
						character.addAbility(category, found);
						if (delegate != null)
						{
							delegate.disableCaptureMode();
							List<String> options = delegate.getCapturedOptions();
							if (options != null && !options.isEmpty())
							{
								Map<String, Object> result = new LinkedHashMap<>();
								result.put("status", "choice_required");
								result.put("ability", found.toString());
								result.put("available_choices", options);
								result.put("message", "This ability requires a choice. Call add_ability again with the 'choice' parameter.");
								return toResult(result);
							}
						}
						return toResult(Map.of("status", "ok", "ability", found.toString()));
					}

					// Choice was provided — pre-select it
					if (delegate != null && choice != null && !choice.isEmpty())
					{
						delegate.setPreSelectedChoices(choice);
					}

					// Clear stale errors before the operation
					if (delegate != null) { delegate.consumeLastError(); }

					character.addAbility(category, found);

					if (delegate != null)
					{
						delegate.clearPreSelectedChoices();
						String error = delegate.consumeLastError();
						if (error != null)
						{
							return errorResult("Failed to add ability: " + error);
						}
					}

					return toResult(Map.of("status", "ok", "ability", found.toString(),
						"choice", choice != null ? choice : List.of()));
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification removeAbility(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("remove_ability",
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
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					String categoryKey = (String) args.get("category_key");
					String abilityKey = (String) args.get("ability_key");

					AbilityCategory category = findCategory(character, categoryKey);
					if (category == null)
					{
						return errorResult("Ability category not found: " + categoryKey);
					}

					AbilityFacade found = null;
					for (AbilityFacade ability : character.getAbilities(category))
					{
						if (ability.getKeyName().equalsIgnoreCase(abilityKey) || ability.toString().equalsIgnoreCase(abilityKey))
						{
							found = ability;
							break;
						}
					}
					if (found == null)
					{
						return errorResult("Character does not have ability: " + abilityKey);
					}

					character.removeAbility(category, found);
					return toResult(Map.of("status", "ok", "removed", found.toString()));
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	private static AbilityCategory findCategory(CharacterFacade character, String categoryKey)
	{
		for (AbilityCategory cat : character.getActiveAbilityCategories())
		{
			if (cat.getKeyName().equalsIgnoreCase(categoryKey) || cat.getDisplayName().equalsIgnoreCase(categoryKey))
			{
				return cat;
			}
		}
		// Fall back to all dataset categories (needed for mythic path categories
		// that may not appear in activeAbilityCategories in headless mode)
		for (AbilityCategory cat : character.getDataSet().getAbilities().getKeys())
		{
			if (cat.getKeyName().equalsIgnoreCase(categoryKey) || cat.getDisplayName().equalsIgnoreCase(categoryKey))
			{
				return cat;
			}
		}
		return null;
	}

	public static SyncToolSpecification batchAddAbilities(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("batch_add_abilities",
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
					"""),
			(exchange, args) -> {
				try
				{
					String characterId = (String) args.get("character_id");
					CharacterFacade character = session.getCharacter(characterId);
					@SuppressWarnings("unchecked")
					List<Map<String, Object>> abilities = (List<Map<String, Object>>) args.get("abilities");
					McpUIDelegate delegate = session.getDelegate(characterId);

					int successCount = 0;
					List<String> errors = new ArrayList<>();

					for (Map<String, Object> entry : abilities)
					{
						String categoryKey = (String) entry.get("category_key");
						String abilityKey = (String) entry.get("ability_key");
						@SuppressWarnings("unchecked")
						List<String> choice = (List<String>) entry.get("choice");

						AbilityCategory category = findCategory(character, categoryKey);
						if (category == null)
						{
							errors.add("Category not found: " + categoryKey);
							continue;
						}

						var abilityList = character.getDataSet().getAbilities().getValue(category);
						AbilityFacade found = null;
						if (abilityList != null)
						{
							for (AbilityFacade ability : abilityList)
							{
								if (ability.getKeyName().equalsIgnoreCase(abilityKey)
									|| ability.toString().equalsIgnoreCase(abilityKey))
								{
									found = ability;
									break;
								}
							}
						}
						if (found == null)
						{
							errors.add("Ability not found: " + abilityKey + " in " + categoryKey);
							continue;
						}

						try
						{
							if (delegate != null && choice != null && !choice.isEmpty())
							{
								delegate.setPreSelectedChoices(choice);
							}
							character.addAbility(category, found);
							if (delegate != null)
							{
								delegate.clearPreSelectedChoices();
							}
							successCount++;
						}
						catch (Exception e)
						{
							errors.add("Failed: " + abilityKey + " - " + e.getMessage());
						}
					}

					Map<String, Object> result = new LinkedHashMap<>();
					result.put("status", errors.isEmpty() ? "ok" : "partial");
					result.put("successCount", successCount);
					result.put("totalRequested", abilities.size());
					if (!errors.isEmpty())
					{
						result.put("errors", errors);
					}
					return toResult(result);
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	private static CallToolResult toResult(Object data)
	{
		try
		{
			String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(data);
			return new CallToolResult(json, false);
		}
		catch (JsonProcessingException e)
		{
			return new CallToolResult(data.toString(), false);
		}
	}

	private static CallToolResult errorResult(String message)
	{
		return new CallToolResult(message, true);
	}
}
