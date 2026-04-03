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

import pcgen.cdom.enumeration.ObjectKey;
import pcgen.cdom.reference.CDOMDirectSingleRef;
import pcgen.core.Equipment;
import pcgen.core.EquipmentModifier;
import pcgen.core.Globals;
import pcgen.core.PlayerCharacter;
import pcgen.facade.core.CharacterFacade;
import pcgen.facade.core.DataSetFacade;
import pcgen.facade.core.EquipmentBuilderFacade;
import pcgen.facade.core.EquipmentBuilderFacade.EquipmentHead;
import pcgen.facade.core.EquipmentFacade;
import pcgen.facade.core.InfoFactory;
import pcgen.gui2.facade.EquipmentBuilderFacadeImpl;
import pcgen.mcp.McpSessionManager;
import pcgen.mcp.McpUIDelegate;

public final class CustomEquipmentTools
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private CustomEquipmentTools()
	{
	}

	public static SyncToolSpecification listEquipmentModifiers(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("list_equipment_modifiers",
				"List available equipment modifiers (enchantments) for a base equipment item",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"equipment_key": { "type": "string", "description": "Base equipment name or key (e.g., Longsword, Chain Shirt)" },
							"head": { "type": "string", "description": "Equipment head: PRIMARY or SECONDARY (default: PRIMARY)", "default": "PRIMARY" }
						},
						"required": ["character_id", "equipment_key"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					String characterId = (String) args.get("character_id");
					CharacterFacade character = session.getCharacter(characterId);
					PlayerCharacter pc = session.getPlayerCharacter(characterId);
					String equipKey = (String) args.get("equipment_key");
					String headStr = args.containsKey("head") ? (String) args.get("head") : "PRIMARY";
					EquipmentHead head = "SECONDARY".equalsIgnoreCase(headStr) ? EquipmentHead.SECONDARY : EquipmentHead.PRIMARY;
					McpUIDelegate delegate = session.getDelegate(characterId);

					Equipment baseEquip = findEquipment(character.getDataSet(), equipKey);
					if (baseEquip == null)
					{
						return errorResult("Equipment not found: " + equipKey);
					}

					Equipment clone = baseEquip.clone();
					clone.put(ObjectKey.BASE_ITEM, CDOMDirectSingleRef.getRef(baseEquip));

					EquipmentBuilderFacadeImpl builder = new EquipmentBuilderFacadeImpl(clone, pc, delegate);
					var availList = builder.getAvailList(head);

					List<Map<String, Object>> mods = new ArrayList<>();
					for (EquipmentModifier mod : availList)
					{
						Map<String, Object> modMap = new LinkedHashMap<>();
						modMap.put("key", mod.getKeyName());
						modMap.put("name", mod.getDisplayName());
						mods.add(modMap);
					}
					return toResult(mods);
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification customizeEquipment(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("customize_equipment",
				"Create a custom/magic equipment item by applying modifiers (enchantments) to a base item. The item is added to the dataset and becomes available for purchase via buy_equipment. Example: customize 'Longsword' with '+1 Enhancement' then buy '+1 Longsword'.",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"equipment_key": { "type": "string", "description": "Base equipment name or key (e.g., Longsword)" },
							"modifier_keys": {
								"type": "array",
								"items": { "type": "string" },
								"description": "List of modifier keys to apply (from list_equipment_modifiers)"
							},
							"head": { "type": "string", "description": "Equipment head: PRIMARY or SECONDARY (default: PRIMARY)", "default": "PRIMARY" },
							"custom_name": { "type": "string", "description": "Custom name override (optional)" }
						},
						"required": ["character_id", "equipment_key", "modifier_keys"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					String characterId = (String) args.get("character_id");
					CharacterFacade character = session.getCharacter(characterId);
					PlayerCharacter pc = session.getPlayerCharacter(characterId);
					String equipKey = (String) args.get("equipment_key");
					@SuppressWarnings("unchecked")
					List<String> modifierKeys = (List<String>) args.get("modifier_keys");
					String headStr = args.containsKey("head") ? (String) args.get("head") : "PRIMARY";
					EquipmentHead head = "SECONDARY".equalsIgnoreCase(headStr) ? EquipmentHead.SECONDARY : EquipmentHead.PRIMARY;
					String customName = (String) args.get("custom_name");
					McpUIDelegate delegate = session.getDelegate(characterId);

					Equipment baseEquip = findEquipment(character.getDataSet(), equipKey);
					if (baseEquip == null)
					{
						return errorResult("Equipment not found: " + equipKey);
					}

					Equipment clone = baseEquip.clone();
					clone.put(ObjectKey.BASE_ITEM, CDOMDirectSingleRef.getRef(baseEquip));

					EquipmentBuilderFacadeImpl builder = new EquipmentBuilderFacadeImpl(clone, pc, delegate);

					List<String> applied = new ArrayList<>();
					List<String> failed = new ArrayList<>();

					for (String modKey : modifierKeys)
					{
						EquipmentModifier mod = findModifier(builder, head, modKey);
						if (mod == null)
						{
							failed.add(modKey + " (not found/not available)");
							continue;
						}
						boolean success = builder.addModToEquipment(mod, head);
						if (success)
						{
							applied.add(mod.getDisplayName());
						}
						else
						{
							failed.add(modKey + " (add failed)");
						}
					}

					if (customName != null && !customName.isBlank())
					{
						builder.setName(customName);
					}

					Equipment finalEquip = (Equipment) builder.getEquipment();

					character.getDataSet().addEquipment(finalEquip);

					InfoFactory info = character.getInfoFactory();
					Map<String, Object> result = new LinkedHashMap<>();
					result.put("status", "ok");
					result.put("name", finalEquip.toString());
					result.put("appliedModifiers", applied);
					if (!failed.isEmpty())
					{
						result.put("failedModifiers", failed);
					}
					result.put("cost", info.getCost(finalEquip));
					result.put("message", "Item created in dataset. Use buy_equipment with name '" + finalEquip.toString() + "' to purchase it.");
					return toResult(result);
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	private static Equipment findEquipment(DataSetFacade dataSet, String key)
	{
		for (EquipmentFacade equip : dataSet.getEquipment())
		{
			if (equip instanceof Equipment e)
			{
				if (e.getKeyName().equalsIgnoreCase(key) || e.getDisplayName().equalsIgnoreCase(key)
					|| e.toString().equalsIgnoreCase(key))
				{
					return e;
				}
			}
		}
		return null;
	}

	private static EquipmentModifier findModifier(EquipmentBuilderFacadeImpl builder, EquipmentHead head, String key)
	{
		for (EquipmentModifier mod : builder.getAvailList(head))
		{
			if (mod.getKeyName().equalsIgnoreCase(key) || mod.getDisplayName().equalsIgnoreCase(key))
			{
				return mod;
			}
		}
		return null;
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
