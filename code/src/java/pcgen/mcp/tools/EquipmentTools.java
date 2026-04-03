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
import pcgen.facade.core.DataSetFacade;
import pcgen.facade.core.EquipmentFacade;
import pcgen.facade.core.EquipmentListFacade;
import pcgen.facade.core.InfoFactory;
import pcgen.mcp.McpSessionManager;
import pcgen.mcp.McpUIDelegate;

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
				"Buy/add a BASE equipment item to inventory. For magic/masterwork items, buy the base item first then use customize_equipment to add enchantments. Example: buy 'Longsword' then customize with '+1 Enhancement'.",
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
					java.math.BigDecimal fundsBefore = character.getFundsRef().get();

					McpUIDelegate delegate = session.getDelegate((String) args.get("character_id"));
					if (delegate != null) { delegate.consumeLastError(); delegate.consumeLastInfo(); }

					character.addPurchasedEquipment(sized, quantity, false, free);

					// Check for errors reported by the delegate
					if (delegate != null)
					{
						String error = delegate.consumeLastError();
						if (error != null)
						{
							return errorResult(error);
						}
						String info = delegate.consumeLastInfo();
						if (info != null)
						{
							return errorResult(info);
						}
					}

					InfoFactory infoFactory = character.getInfoFactory();
					Map<String, Object> result = new LinkedHashMap<>();
					result.put("status", "ok");
					result.put("equipment", sized.toString());
					result.put("quantity", quantity);
					result.put("cost", infoFactory.getCost(sized));
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

	public static SyncToolSpecification batchBuyEquipment(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("batch_buy_equipment",
				"Buy multiple equipment items in one call, with optional free flag and equip slot.",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"items": {
								"type": "array",
								"description": "List of items: [{equipment_key, quantity?, free?, slot?}]",
								"items": {
									"type": "object",
									"properties": {
										"equipment_key": { "type": "string" },
										"quantity": { "type": "integer", "default": 1 },
										"free": { "type": "boolean", "default": false },
										"slot": { "type": "string", "description": "Equip slot (optional, e.g. 'Primary Hand', 'Armor')" }
									},
									"required": ["equipment_key"]
								}
							}
						},
						"required": ["character_id", "items"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					String characterId = (String) args.get("character_id");
					CharacterFacade character = session.getCharacter(characterId);
					@SuppressWarnings("unchecked")
					List<Map<String, Object>> items = (List<Map<String, Object>>) args.get("items");
					DataSetFacade dataSet = character.getDataSet();
					McpUIDelegate delegate = session.getDelegate(characterId);

					int successCount = 0;
					List<String> errors = new ArrayList<>();

					for (Map<String, Object> entry : items)
					{
						String equipKey = (String) entry.get("equipment_key");
						int quantity = entry.containsKey("quantity") ? ((Number) entry.get("quantity")).intValue() : 1;
						boolean free = entry.containsKey("free") && Boolean.TRUE.equals(entry.get("free"));
						String slot = (String) entry.get("slot");

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
							errors.add("Equipment not found: " + equipKey);
							continue;
						}

						try
						{
							EquipmentFacade sized = character.getEquipmentSizedForCharacter(found);
							if (delegate != null) { delegate.consumeLastError(); delegate.consumeLastInfo(); }

							character.addPurchasedEquipment(sized, quantity, false, free);

							if (delegate != null)
							{
								String error = delegate.consumeLastError();
								if (error != null)
								{
									errors.add("Buy failed: " + equipKey + " - " + error);
									continue;
								}
								String info = delegate.consumeLastInfo();
								if (info != null)
								{
									errors.add("Buy failed: " + equipKey + " - " + info);
									continue;
								}
							}

							if (slot != null && !slot.isBlank())
							{
								try
								{
									var eqSet = character.getEquipmentSetRef().get();
									if (eqSet != null)
									{
										EquipmentFacade invItem = findInInventory(character, sized.toString());
										if (invItem != null)
										{
											var nodes = eqSet.getNodes();
											pcgen.gui2.facade.EquipNode targetNode = null;
											for (var node : nodes)
											{
												if (node instanceof pcgen.gui2.facade.EquipNode en
													&& en.getNodeType() == pcgen.gui2.facade.EquipNode.NodeType.PHANTOM_SLOT
													&& en.toString().equalsIgnoreCase(slot))
												{
													if (eqSet.canEquip(en, invItem))
													{
														targetNode = en;
														break;
													}
												}
											}
											if (targetNode != null)
											{
												eqSet.addEquipment(targetNode, invItem, quantity);
											}
										}
									}
								}
								catch (Exception ignored)
								{
									// Equipping is best-effort; purchase already succeeded
								}
							}

							successCount++;
						}
						catch (Exception e)
						{
							errors.add("Failed: " + equipKey + " - " + e.getMessage());
						}
					}

					Map<String, Object> result = new LinkedHashMap<>();
					result.put("status", errors.isEmpty() ? "ok" : "partial");
					result.put("successCount", successCount);
					result.put("totalRequested", items.size());
					result.put("funds", character.getFundsRef().get());
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

	private static EquipmentFacade findInInventory(CharacterFacade character, String key)
	{
		EquipmentListFacade purchased = character.getPurchasedEquipment();
		for (EquipmentFacade equip : purchased)
		{
			if (equip.toString().equalsIgnoreCase(key) || equip.getKeyName().equalsIgnoreCase(key))
			{
				return equip;
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
