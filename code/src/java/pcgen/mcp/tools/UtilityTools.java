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

import pcgen.facade.core.CharacterFacade;
import pcgen.facade.core.InfoFacade;
import pcgen.facade.core.TodoFacade;
import pcgen.mcp.McpSessionManager;

public final class UtilityTools
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private UtilityTools()
	{
	}

	public static SyncToolSpecification getTodoList(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("get_todo_list",
				"Get the character's TODO list - things that still need to be done (e.g., select feats, assign skill points)",
				"""
					{ "type": "object", "properties": { "character_id": { "type": "string" } }, "required": ["character_id"] }
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					List<String> todos = new ArrayList<>();
					for (TodoFacade todo : character.getTodoList())
					{
						todos.add(todo.getMessageKey());
					}
					return toResult(Map.of("todoList", todos, "count", todos.size()));
				}
				catch (Exception e) { return errorResult(e.getMessage()); }
			}
		);
	}

	public static SyncToolSpecification isQualifiedFor(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("is_qualified_for",
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
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					String itemType = (String) args.get("item_type");
					String itemKey = (String) args.get("item_key");
					var dataSet = character.getDataSet();

					boolean qualified = false;
					String itemName = itemKey;

					switch (itemType.toLowerCase())
					{
						case "race":
							for (var r : dataSet.getRaces())
							{
								if (r.getKeyName().equalsIgnoreCase(itemKey) || r.getDisplayName().equalsIgnoreCase(itemKey))
								{
									qualified = character.isQualifiedFor(r);
									itemName = r.getDisplayName();
									break;
								}
							}
							break;
						case "class":
							for (var c : dataSet.getClasses())
							{
								if (c.getKeyName().equalsIgnoreCase(itemKey) || c.getDisplayName().equalsIgnoreCase(itemKey))
								{
									qualified = character.isQualifiedFor(c);
									itemName = c.getDisplayName();
									break;
								}
							}
							break;
						case "deity":
							for (var d : dataSet.getDeities())
							{
								if (d.getKeyName().equalsIgnoreCase(itemKey) || d.getDisplayName().equalsIgnoreCase(itemKey))
								{
									qualified = character.isQualifiedFor(d);
									itemName = d.getDisplayName();
									break;
								}
							}
							break;
						case "template":
							for (var t : dataSet.getTemplates())
							{
								if (t.getKeyName().equalsIgnoreCase(itemKey) || t.getDisplayName().equalsIgnoreCase(itemKey))
								{
									qualified = character.isQualifiedFor(t);
									itemName = t.getDisplayName();
									break;
								}
							}
							break;
						case "kit":
							for (var k : dataSet.getKits())
							{
								if (k.getKeyName().equalsIgnoreCase(itemKey) || k.getDisplayName().equalsIgnoreCase(itemKey))
								{
									qualified = character.isQualifiedFor(k);
									itemName = k.getDisplayName();
									break;
								}
							}
							break;
						default:
							return errorResult("Unknown item type: " + itemType + ". Use: race, class, deity, template, kit");
					}
					return toResult(Map.of("qualified", qualified, "item", itemName, "type", itemType));
				}
				catch (Exception e) { return errorResult(e.getMessage()); }
			}
		);
	}

	public static SyncToolSpecification rollStats(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("roll_stats",
				"Roll ability scores using the game mode's rolling method",
				"""
					{ "type": "object", "properties": { "character_id": { "type": "string" } }, "required": ["character_id"] }
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					if (!character.isStatRollEnabled())
					{
						return errorResult("Stat rolling is not enabled for this game mode");
					}
					character.rollStats();

					var dataSet = character.getDataSet();
					Map<String, Integer> scores = new LinkedHashMap<>();
					for (var stat : dataSet.getStats())
					{
						scores.put(stat.getKeyName(), character.getScoreBase(stat));
					}
					return toResult(Map.of("status", "ok", "scores", scores));
				}
				catch (Exception e) { return errorResult(e.getMessage()); }
			}
		);
	}

	public static SyncToolSpecification isDirty(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("is_dirty",
				"Check if a character has unsaved changes",
				"""
					{ "type": "object", "properties": { "character_id": { "type": "string" } }, "required": ["character_id"] }
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					return toResult(Map.of("dirty", character.isDirty()));
				}
				catch (Exception e) { return errorResult(e.getMessage()); }
			}
		);
	}

	public static SyncToolSpecification getCharacterDetails(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("get_character_details",
				"Get detailed computed stats: HP, AC-related weight/load, wealth, stat totals/modifiers",
				"""
					{ "type": "object", "properties": { "character_id": { "type": "string" } }, "required": ["character_id"] }
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					var dataSet = character.getDataSet();

					Map<String, Object> result = new LinkedHashMap<>();
					result.put("hp", character.getTotalHPRef().get());
					result.put("carriedWeight", character.getCarriedWeightRef().get());
					result.put("load", character.getLoadRef().get());
					result.put("weightLimit", character.getWeightLimitRef().get());
					result.put("funds", character.getFundsRef().get());
					result.put("wealth", character.getWealthRef().get());

					Map<String, Object> stats = new LinkedHashMap<>();
					for (var stat : dataSet.getStats())
					{
						Map<String, Object> statDetail = new LinkedHashMap<>();
						statDetail.put("base", character.getScoreBase(stat));
						statDetail.put("total", character.getScoreTotalString(stat));
						statDetail.put("modifier", character.getModTotal(stat));
						statDetail.put("raceBonus", character.getScoreRaceBonus(stat));
						statDetail.put("otherBonus", character.getScoreOtherBonus(stat));
						stats.put(stat.getKeyName(), statDetail);
					}
					result.put("stats", stats);

					return toResult(result);
				}
				catch (Exception e) { return errorResult(e.getMessage()); }
			}
		);
	}

	private static CallToolResult toResult(Object data)
	{
		try
		{
			return new CallToolResult(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(data), false);
		}
		catch (JsonProcessingException e) { return new CallToolResult(data.toString(), false); }
	}

	private static CallToolResult errorResult(String message)
	{
		return new CallToolResult(message, true);
	}
}
