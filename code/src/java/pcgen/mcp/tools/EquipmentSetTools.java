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
import pcgen.facade.core.EquipmentFacade;
import pcgen.facade.core.EquipmentSetFacade;
import pcgen.gui2.facade.EquipNode;
import pcgen.facade.util.ListFacade;
import pcgen.mcp.McpSessionManager;

public final class EquipmentSetTools
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private EquipmentSetTools()
	{
	}

	public static SyncToolSpecification listEquipmentSets(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("list_equipment_sets",
				"List all equipment sets (configurations) for a character",
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
					ListFacade<EquipmentSetFacade> sets = character.getEquipmentSets();
					EquipmentSetFacade current = character.getEquipmentSetRef().get();

					List<Map<String, Object>> result = new ArrayList<>();
					for (EquipmentSetFacade set : sets)
					{
						Map<String, Object> setMap = new LinkedHashMap<>();
						setMap.put("name", set.getNameRef().get());
						setMap.put("active", set == current);
						result.add(setMap);
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

	public static SyncToolSpecification createEquipmentSet(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("create_equipment_set",
				"Create a new equipment set (equipment configuration)",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"name": { "type": "string", "description": "Name for the new equipment set" }
						},
						"required": ["character_id", "name"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					String name = (String) args.get("name");
					EquipmentSetFacade newSet = character.createEquipmentSet(name);
					character.setEquipmentSet(newSet);
					return toResult(Map.of("status", "ok", "name", name));
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification getEquippedItems(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("get_equipped_items",
				"List all body slots and currently equipped items in the active equipment set",
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
					EquipmentSetFacade eqSet = character.getEquipmentSetRef().get();
					if (eqSet == null)
					{
						return errorResult("No active equipment set");
					}

					ListFacade<EquipNode> nodes = eqSet.getNodes();
					List<Map<String, Object>> slots = new ArrayList<>();
					for (EquipNode node : nodes)
					{
						Map<String, Object> slotMap = new LinkedHashMap<>();
						slotMap.put("slot", node.toString());
						slotMap.put("type", node.getNodeType().name());
						EquipmentFacade equip = node.getEquipment();
						if (equip != null)
						{
							slotMap.put("equipment", equip.toString());
							slotMap.put("equipmentKey", equip.getKeyName());
							slotMap.put("quantity", eqSet.getQuantity(node));
						}
						slots.add(slotMap);
					}
					return toResult(slots);
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification equipItem(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("equip_item",
				"Equip an item from inventory into a body slot. Use get_equipped_items to see available slots.",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"equipment_key": { "type": "string", "description": "Equipment name or key from inventory" },
							"slot": { "type": "string", "description": "Target slot name (from get_equipped_items, e.g., 'Primary Hand', 'Armor', 'Ring')" },
							"quantity": { "type": "integer", "description": "Quantity to equip (default: 1)", "default": 1 }
						},
						"required": ["character_id", "equipment_key", "slot"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					String equipKey = (String) args.get("equipment_key");
					String slotName = (String) args.get("slot");
					int quantity = args.containsKey("quantity") ? ((Number) args.get("quantity")).intValue() : 1;

					EquipmentSetFacade eqSet = character.getEquipmentSetRef().get();
					if (eqSet == null)
					{
						return errorResult("No active equipment set");
					}

					EquipmentFacade equipToWear = findInInventory(character, equipKey);
					if (equipToWear == null)
					{
						return errorResult("Item not found in inventory: " + equipKey);
					}

					EquipNode targetNode = findSlot(eqSet, slotName);
					if (targetNode == null)
					{
						return errorResult("Slot not found: " + slotName + ". Use get_equipped_items to see available slots.");
					}

					if (!eqSet.canEquip(targetNode, equipToWear))
					{
						return errorResult("Cannot equip " + equipToWear.toString() + " in slot " + slotName);
					}

					EquipmentFacade equipped = eqSet.addEquipment(targetNode, equipToWear, quantity);
					if (equipped == null)
					{
						return errorResult("Failed to equip item");
					}

					return toResult(Map.of("status", "ok", "equipped", equipped.toString(), "slot", slotName));
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification unequipItem(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("unequip_item",
				"Remove an equipped item from a body slot",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"slot": { "type": "string", "description": "Slot name containing the item to remove" },
							"quantity": { "type": "integer", "description": "Quantity to remove (default: 1)", "default": 1 }
						},
						"required": ["character_id", "slot"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					String slotName = (String) args.get("slot");
					int quantity = args.containsKey("quantity") ? ((Number) args.get("quantity")).intValue() : 1;

					EquipmentSetFacade eqSet = character.getEquipmentSetRef().get();
					if (eqSet == null)
					{
						return errorResult("No active equipment set");
					}

					EquipNode targetNode = findEquippedNode(eqSet, slotName);
					if (targetNode == null)
					{
						return errorResult("No equipped item found at: " + slotName);
					}

					EquipmentFacade removed = eqSet.removeEquipment(targetNode, quantity);
					return toResult(Map.of("status", "ok", "removed", removed != null ? removed.toString() : "item"));
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	private static EquipmentFacade findInInventory(CharacterFacade character, String key)
	{
		for (EquipmentFacade equip : character.getPurchasedEquipment())
		{
			if (equip.toString().equalsIgnoreCase(key) || equip.getKeyName().equalsIgnoreCase(key))
			{
				return equip;
			}
		}
		return null;
	}

	private static EquipNode findSlot(EquipmentSetFacade eqSet, String slotName)
	{
		for (EquipNode node : eqSet.getNodes())
		{
			if (node.toString().equalsIgnoreCase(slotName))
			{
				return node;
			}
		}
		// Partial match
		for (EquipNode node : eqSet.getNodes())
		{
			if (node.toString().toLowerCase().contains(slotName.toLowerCase()))
			{
				return node;
			}
		}
		return null;
	}

	private static EquipNode findEquippedNode(EquipmentSetFacade eqSet, String slotOrItemName)
	{
		for (EquipNode node : eqSet.getNodes())
		{
			if (node.getEquipment() != null)
			{
				if (node.toString().equalsIgnoreCase(slotOrItemName)
					|| node.getEquipment().toString().equalsIgnoreCase(slotOrItemName)
					|| node.getEquipment().getKeyName().equalsIgnoreCase(slotOrItemName))
				{
					return node;
				}
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
			return new CallToolResult(List.of(new TextContent(data.toString())), false);
		}
	}

	private static CallToolResult errorResult(String message)
	{
		return new CallToolResult(List.of(new TextContent(message)), true);
	}
}
