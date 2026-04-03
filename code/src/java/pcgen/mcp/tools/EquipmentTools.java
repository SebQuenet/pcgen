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

import pcgen.facade.core.CharacterFacade;
import pcgen.facade.core.DataSetFacade;
import pcgen.facade.core.EquipmentFacade;
import pcgen.facade.core.EquipmentListFacade;
import pcgen.facade.core.InfoFactory;
import pcgen.mcp.McpSessionManager;

public final class EquipmentTools
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private EquipmentTools()
	{
	}

	public static SyncToolSpecification buyEquipment(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("buy_equipment",
				"Buy/add equipment to a character's inventory",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"equipment_key": { "type": "string", "description": "Equipment name or key" },
							"quantity": { "type": "integer", "description": "Quantity to buy (default: 1)", "default": 1 },
							"free": { "type": "boolean", "description": "Add for free without deducting gold (default: false)", "default": false }
						},
						"required": ["character_id", "equipment_key"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					String equipKey = (String) args.get("equipment_key");
					int quantity = args.containsKey("quantity") ? ((Number) args.get("quantity")).intValue() : 1;
					boolean free = args.containsKey("free") && Boolean.TRUE.equals(args.get("free"));
					DataSetFacade dataSet = character.getDataSet();

					EquipmentFacade found = null;
					for (EquipmentFacade equip : dataSet.getEquipment())
					{
						if (equip.toString().equalsIgnoreCase(equipKey) || equip.getKeyName().equalsIgnoreCase(equipKey))
						{
							found = equip;
							break;
						}
					}
					if (found == null)
					{
						return errorResult("Equipment not found: " + equipKey);
					}

					EquipmentFacade sized = character.getEquipmentSizedForCharacter(found);
					character.addPurchasedEquipment(sized, quantity, false, free);

					InfoFactory info = character.getInfoFactory();
					Map<String, Object> result = new LinkedHashMap<>();
					result.put("status", "ok");
					result.put("equipment", sized.toString());
					result.put("quantity", quantity);
					result.put("cost", info.getCost(sized));
					result.put("funds", character.getFundsRef().get());
					return toResult(result);
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification sellEquipment(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("sell_equipment",
				"Sell/remove equipment from a character's inventory",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"equipment_key": { "type": "string", "description": "Equipment name or key" },
							"quantity": { "type": "integer", "description": "Quantity to sell (default: 1)", "default": 1 },
							"free": { "type": "boolean", "description": "Remove without adding gold (default: false)", "default": false }
						},
						"required": ["character_id", "equipment_key"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					String equipKey = (String) args.get("equipment_key");
					int quantity = args.containsKey("quantity") ? ((Number) args.get("quantity")).intValue() : 1;
					boolean free = args.containsKey("free") && Boolean.TRUE.equals(args.get("free"));

					EquipmentListFacade purchased = character.getPurchasedEquipment();
					EquipmentFacade found = null;
					for (EquipmentFacade equip : purchased)
					{
						if (equip.toString().equalsIgnoreCase(equipKey) || equip.getKeyName().equalsIgnoreCase(equipKey))
						{
							found = equip;
							break;
						}
					}
					if (found == null)
					{
						return errorResult("Character does not own: " + equipKey);
					}

					character.removePurchasedEquipment(found, quantity, free);
					return toResult(Map.of("status", "ok", "removed", found.toString(), "quantity", quantity,
						"funds", character.getFundsRef().get()));
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification getInventory(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("get_inventory",
				"Get a character's purchased equipment inventory",
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
					EquipmentListFacade purchased = character.getPurchasedEquipment();
					InfoFactory info = character.getInfoFactory();

					List<Map<String, Object>> items = new ArrayList<>();
					for (EquipmentFacade equip : purchased)
					{
						Map<String, Object> item = new LinkedHashMap<>();
						item.put("name", equip.toString());
						item.put("key", equip.getKeyName());
						item.put("quantity", purchased.getQuantity(equip));
						item.put("cost", info.getCost(equip));
						item.put("weight", info.getWeight(equip));
						items.add(item);
					}

					Map<String, Object> result = new LinkedHashMap<>();
					result.put("items", items);
					result.put("funds", character.getFundsRef().get());
					result.put("carriedWeight", character.getCarriedWeightRef().get());
					result.put("load", character.getLoadRef().get());
					return toResult(result);
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification setFunds(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("set_funds",
				"Set a character's available funds (gold pieces)",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"amount": { "type": "number", "description": "Amount of gold pieces" }
						},
						"required": ["character_id", "amount"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					double amount = ((Number) args.get("amount")).doubleValue();
					character.setFunds(java.math.BigDecimal.valueOf(amount));
					return toResult(Map.of("status", "ok", "funds", character.getFundsRef().get()));
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
