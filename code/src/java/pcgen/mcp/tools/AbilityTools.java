package pcgen.mcp.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import pcgen.core.AbilityCategory;
import pcgen.facade.core.AbilityFacade;
import pcgen.facade.core.CharacterFacade;
import pcgen.mcp.McpSessionManager;
import pcgen.mcp.PendingChoice;
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
					for (AbilityCategory cat : character.getActiveAbilityCategories())
					{
						Map<String, Object> catMap = new LinkedHashMap<>();
						catMap.put("key", cat.getKeyName());
						catMap.put("name", cat.getDisplayName());
						catMap.put("remaining", character.getRemainingSelections(cat));
						catMap.put("total", character.getTotalSelections(cat));
						categories.add(catMap);
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
				"Add an ability (feat, trait, etc.) to a character. May trigger a chooser if the ability requires selections.",
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
					String characterId = (String) args.get("character_id");
					CharacterFacade character = session.getCharacter(characterId);
					String categoryKey = (String) args.get("category_key");
					String abilityKey = (String) args.get("ability_key");

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

					character.addAbility(category, found);

					McpUIDelegate delegate = session.getDelegate(characterId);
					if (delegate != null)
					{
						PendingChoice pending = delegate.getLatestPendingChoice();
						if (pending != null)
						{
							Map<String, Object> result = new LinkedHashMap<>();
							result.put("status", "pending_choice");
							result.put("choice_id", pending.choiceId());
							result.put("title", pending.title());
							result.put("available_options", pending.availableOptions());
							result.put("remaining_selections", pending.remainingSelections());
							result.put("message", "This ability requires a choice. Use resolve_choice tool.");
							return toResult(result);
						}
					}

					return toResult(Map.of("status", "ok", "ability", found.toString()));
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
		return null;
	}

	private static CallToolResult toResult(Object data)
	{
		try
		{
			String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(data);
			return new CallToolResult(List.of(new TextContent(json)), false);
		}
		catch (JsonProcessingException e)
		{
			return errorResult("JSON serialization error: " + e.getMessage());
		}
	}

	private static CallToolResult errorResult(String message)
	{
		return new CallToolResult(List.of(new TextContent(message)), true);
	}
}
