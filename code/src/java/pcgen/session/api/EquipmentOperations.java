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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pcgen.session.service.CustomEquipmentService;
import pcgen.session.service.EquipmentService;
import pcgen.session.service.EquipmentSetService;
import pcgen.session.service.EquipmentTokenSupport;

/**
 * The operations over what a character owns, wears, and has made for them.
 */
public final class EquipmentOperations
{
	private static final String CHARACTER_ID = "character_id";
	private static final String EQUIPMENT_KEY = "equipment_key";
	private static final String QUANTITY = "quantity";
	private static final String SLOT = "slot";
	private static final String HEAD = "head";

	private static final String CHARACTER_ONLY_SCHEMA = """
		{
			"type": "object",
			"properties": {
				"character_id": { "type": "string", "description": "Character ID" }
			},
			"required": ["character_id"]
		}
		""";

	private EquipmentOperations()
	{
	}

	public static List<Operation> of(EquipmentService equipment, EquipmentSetService sets,
		CustomEquipmentService custom)
	{
		return List.of(
			buyEquipment(equipment),
			sellEquipment(equipment),
			getInventory(equipment),
			setFunds(equipment),
			batchBuyEquipment(equipment),
			listEquipmentSets(sets),
			createEquipmentSet(sets),
			getEquippedItems(sets),
			equipItem(sets),
			unequipItem(sets),
			equipItems(sets),
			listEquipmentModifiers(custom),
			customizeEquipment(custom));
	}

	private static Operation buyEquipment(EquipmentService service)
	{
		return Operation.exclusive("buy_equipment",
			"Buy/add a BASE equipment item to inventory. For magic/masterwork items, buy the base item "
				+ "first then use customize_equipment to add enchantments. "
				+ "Example: buy 'Longsword' then customize with '+1 Enhancement'.",
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
				""",
			arguments -> arguments.decodeThen(() -> service.buyEquipment(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString(EQUIPMENT_KEY),
				arguments.optionalInt(QUANTITY, 1),
				arguments.optionalBoolean("free", false))));
	}

	private static Operation sellEquipment(EquipmentService service)
	{
		return Operation.exclusive("sell_equipment",
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
				""",
			arguments -> arguments.decodeThen(() -> service.sellEquipment(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString(EQUIPMENT_KEY),
				arguments.optionalInt(QUANTITY, 1),
				arguments.optionalBoolean("free", false))));
	}

	private static Operation getInventory(EquipmentService service)
	{
		return Operation.exclusive("get_inventory",
			"Get a character's purchased equipment inventory",
			CHARACTER_ONLY_SCHEMA,
			arguments -> arguments.decodeThen(
				() -> service.getInventory(arguments.requiredString(CHARACTER_ID))));
	}

	private static Operation setFunds(EquipmentService service)
	{
		return Operation.exclusive("set_funds",
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
				""",
			arguments -> arguments.decodeThen(() -> service.setFunds(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredDouble("amount"))));
	}

	private static Operation batchBuyEquipment(EquipmentService service)
	{
		return Operation.exclusive("batch_buy_equipment",
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
				""",
			arguments -> arguments.decodeThen(() -> {
				String characterId = arguments.requiredString(CHARACTER_ID);
				List<EquipmentService.PurchaseRequest> requests = new ArrayList<>();
				for (Arguments entry : arguments.requiredEntryList("items"))
				{
					requests.add(new EquipmentService.PurchaseRequest(
						entry.requiredString(EQUIPMENT_KEY),
						entry.optionalInt(QUANTITY, 1),
						entry.optionalBoolean("free", false),
						entry.optionalString(SLOT, null)));
				}
				return service.batchBuyEquipment(characterId, List.copyOf(requests));
			}));
	}

	private static Operation listEquipmentSets(EquipmentSetService service)
	{
		return Operation.exclusive("list_equipment_sets",
			"List all equipment sets (configurations) for a character",
			CHARACTER_ONLY_SCHEMA,
			arguments -> arguments.decodeThen(
				() -> service.listEquipmentSets(arguments.requiredString(CHARACTER_ID))));
	}

	private static Operation createEquipmentSet(EquipmentSetService service)
	{
		return Operation.exclusive("create_equipment_set",
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
				""",
			arguments -> arguments.decodeThen(() -> service.createEquipmentSet(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString("name"))));
	}

	private static Operation getEquippedItems(EquipmentSetService service)
	{
		return Operation.exclusive("get_equipped_items",
			"List all body slots and currently equipped items in the active equipment set",
			CHARACTER_ONLY_SCHEMA,
			arguments -> arguments.decodeThen(
				() -> service.getEquippedItems(arguments.requiredString(CHARACTER_ID))));
	}

	private static Operation equipItem(EquipmentSetService service)
	{
		return Operation.exclusive("equip_item",
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
				""",
			arguments -> arguments.decodeThen(() -> service.equipItem(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString(EQUIPMENT_KEY),
				arguments.requiredString(SLOT),
				arguments.optionalInt(QUANTITY, 1))));
	}

	private static Operation unequipItem(EquipmentSetService service)
	{
		return Operation.exclusive("unequip_item",
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
				""",
			arguments -> arguments.decodeThen(() -> service.unequipItem(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString(SLOT),
				arguments.optionalInt(QUANTITY, 1))));
	}

	private static Operation equipItems(EquipmentSetService service)
	{
		return Operation.exclusive("equip_items",
			"Equip multiple items from inventory into body slots in one call. "
				+ "Returns which items succeeded and which failed.",
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"items": {
							"type": "array",
							"description": "List of items to equip: [{equipment_key, slot, quantity?}]",
							"items": {
								"type": "object",
								"properties": {
									"equipment_key": { "type": "string", "description": "Equipment name or key from inventory" },
									"slot": { "type": "string", "description": "Target slot name (e.g. Primary Hand, Armor, Equipped)" },
									"quantity": { "type": "integer", "description": "Quantity to equip (default: 1)", "default": 1 }
								},
								"required": ["equipment_key", "slot"]
							}
						}
					},
					"required": ["character_id", "items"]
				}
				""",
			arguments -> arguments.decodeThen(() -> {
				String characterId = arguments.requiredString(CHARACTER_ID);
				List<EquipmentSetService.EquipRequest> requests = new ArrayList<>();
				for (Arguments entry : arguments.requiredEntryList("items"))
				{
					requests.add(new EquipmentSetService.EquipRequest(
						entry.requiredString(EQUIPMENT_KEY),
						entry.requiredString(SLOT),
						entry.optionalInt(QUANTITY, 1)));
				}
				return service.equipItems(characterId, List.copyOf(requests));
			}));
	}

	private static Operation listEquipmentModifiers(CustomEquipmentService service)
	{
		return Operation.exclusive("list_equipment_modifiers",
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
				""",
			arguments -> arguments.decodeThen(() -> service.listEquipmentModifiers(
				arguments.requiredString(CHARACTER_ID),
				arguments.requiredString(EQUIPMENT_KEY),
				arguments.optionalString(HEAD, "PRIMARY"))));
	}

	private static Operation customizeEquipment(CustomEquipmentService service)
	{
		return Operation.exclusive("customize_equipment",
			"Create a custom/magic equipment item by applying modifiers (enchantments) to a base item. "
				+ "The item is added to the dataset and becomes available for purchase via buy_equipment. "
				+ "Example: customize 'Longsword' with '+1 Enhancement' then buy '+1 Longsword'.",
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
						"custom_name": { "type": "string", "description": "Custom name override (optional)" },
						"choices": {
							"type": "object",
							"description": "Map of modifier key to list of choice strings for CHOOSE-based modifiers (e.g. Bane key mapped to Evil Outsider Bane)",
							"additionalProperties": {
								"type": "array",
								"items": { "type": "string" }
							}
						},
						"spell_abilities": {
							"type": "array",
							"description": "Spell-like abilities the item grants. Each needs spell (key), times_per_day (-1=at will) and caster_level.",
							"items": {
								"type": "object",
								"properties": {
									"spell": { "type": "string" },
									"times_per_day": { "type": "string" },
									"caster_level": { "type": "string" }
								},
								"required": ["spell", "times_per_day", "caster_level"]
							}
						},
						"extra_tokens": {
							"type": "array",
							"items": { "type": "string" },
							"description": "Raw LST token strings applied to the item (e.g. 'DEFINE:SceptreCL|10', 'BONUS:VAR|SceptreCL|5|PREEQUIP:1,Diademe de Lyra', 'SPROP|...')."
						}
					},
					"required": ["character_id", "equipment_key", "modifier_keys"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.customizeEquipment(
				arguments.requiredString(CHARACTER_ID),
				new CustomEquipmentService.CustomisationRequest(
					arguments.requiredString(EQUIPMENT_KEY),
					arguments.requiredStringList("modifier_keys"),
					arguments.optionalString(HEAD, "PRIMARY"),
					arguments.optionalString("custom_name", null),
					choicesPerModifier(arguments),
					spellAbilities(arguments),
					arguments.optionalStringList("extra_tokens")))));
	}

	private static Map<String, List<String>> choicesPerModifier(Arguments arguments)
	{
		if (!arguments.has("choices"))
		{
			return Map.of();
		}
		Map<String, List<String>> perModifier = new LinkedHashMap<>();
		Arguments choices = arguments.requiredNestedObject("choices");
		for (String modifierKey : choices.fieldNames())
		{
			perModifier.put(modifierKey, choices.optionalStringList(modifierKey));
		}
		return Map.copyOf(perModifier);
	}

	private static List<EquipmentTokenSupport.SpellAbility> spellAbilities(Arguments arguments)
	{
		if (!arguments.has("spell_abilities"))
		{
			return List.of();
		}
		List<EquipmentTokenSupport.SpellAbility> abilities = new ArrayList<>();
		for (Arguments entry : arguments.requiredEntryList("spell_abilities"))
		{
			abilities.add(new EquipmentTokenSupport.SpellAbility(
				entry.optionalString("spell", null),
				entry.optionalString("times_per_day", null),
				entry.optionalString("caster_level", null)));
		}
		return List.copyOf(abilities);
	}
}
