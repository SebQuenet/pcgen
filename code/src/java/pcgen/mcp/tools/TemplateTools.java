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

import pcgen.core.Kit;
import pcgen.core.PCTemplate;
import pcgen.facade.core.CharacterFacade;
import pcgen.facade.core.DataSetFacade;
import pcgen.facade.core.TempBonusFacade;
import pcgen.mcp.McpSessionManager;

public final class TemplateTools
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private TemplateTools()
	{
	}

	public static SyncToolSpecification addTemplate(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("add_template",
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
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					String key = (String) args.get("template_key");
					DataSetFacade ds = character.getDataSet();
					PCTemplate found = null;
					for (PCTemplate t : ds.getTemplates())
					{
						if (t.getKeyName().equalsIgnoreCase(key) || t.getDisplayName().equalsIgnoreCase(key))
						{
							found = t;
							break;
						}
					}
					if (found == null) return errorResult("Template not found: " + key);
					character.addTemplate(found);
					return toResult(Map.of("status", "ok", "template", found.getDisplayName()));
				}
				catch (Exception e) { return errorResult(e.getMessage()); }
			}
		);
	}

	public static SyncToolSpecification removeTemplate(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("remove_template",
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
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					String key = (String) args.get("template_key");
					PCTemplate found = null;
					for (PCTemplate t : character.getTemplates())
					{
						if (t.getKeyName().equalsIgnoreCase(key) || t.getDisplayName().equalsIgnoreCase(key))
						{
							found = t;
							break;
						}
					}
					if (found == null) return errorResult("Character does not have template: " + key);
					character.removeTemplate(found);
					return toResult(Map.of("status", "ok", "removed", found.getDisplayName()));
				}
				catch (Exception e) { return errorResult(e.getMessage()); }
			}
		);
	}

	public static SyncToolSpecification getTemplates(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("get_templates",
				"List templates applied to a character",
				"""
					{ "type": "object", "properties": { "character_id": { "type": "string" } }, "required": ["character_id"] }
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					List<Map<String, Object>> list = new ArrayList<>();
					for (PCTemplate t : character.getTemplates())
					{
						list.add(Map.of("key", t.getKeyName(), "name", t.getDisplayName()));
					}
					return toResult(list);
				}
				catch (Exception e) { return errorResult(e.getMessage()); }
			}
		);
	}

	public static SyncToolSpecification addTempBonus(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("add_temp_bonus",
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
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					String name = (String) args.get("bonus_name");
					TempBonusFacade found = null;
					for (TempBonusFacade b : character.getAvailableTempBonuses())
					{
						if (b.toString().equalsIgnoreCase(name))
						{
							found = b;
							break;
						}
					}
					if (found == null) return errorResult("Temp bonus not found: " + name);
					character.addTempBonus(found);
					return toResult(Map.of("status", "ok", "bonus", found.toString()));
				}
				catch (Exception e) { return errorResult(e.getMessage()); }
			}
		);
	}

	public static SyncToolSpecification removeTempBonus(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("remove_temp_bonus",
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
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					String name = (String) args.get("bonus_name");
					TempBonusFacade found = null;
					for (TempBonusFacade b : character.getTempBonuses())
					{
						if (b.toString().equalsIgnoreCase(name))
						{
							found = b;
							break;
						}
					}
					if (found == null) return errorResult("Active temp bonus not found: " + name);
					character.removeTempBonus(found);
					return toResult(Map.of("status", "ok", "removed", found.toString()));
				}
				catch (Exception e) { return errorResult(e.getMessage()); }
			}
		);
	}

	public static SyncToolSpecification listTempBonuses(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("list_temp_bonuses",
				"List available and active temporary bonuses for a character",
				"""
					{ "type": "object", "properties": { "character_id": { "type": "string" } }, "required": ["character_id"] }
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					List<String> available = new ArrayList<>();
					for (TempBonusFacade b : character.getAvailableTempBonuses()) available.add(b.toString());
					List<String> active = new ArrayList<>();
					for (TempBonusFacade b : character.getTempBonuses()) active.add(b.toString());
					return toResult(Map.of("available", available, "active", active));
				}
				catch (Exception e) { return errorResult(e.getMessage()); }
			}
		);
	}

	public static SyncToolSpecification addKit(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("add_kit",
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
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					String key = (String) args.get("kit_key");
					Kit found = null;
					for (Kit k : character.getAvailableKits())
					{
						if (k.getKeyName().equalsIgnoreCase(key) || k.getDisplayName().equalsIgnoreCase(key))
						{
							found = k;
							break;
						}
					}
					if (found == null) return errorResult("Kit not found: " + key);
					character.addKit(found);
					return toResult(Map.of("status", "ok", "kit", found.getDisplayName()));
				}
				catch (Exception e) { return errorResult(e.getMessage()); }
			}
		);
	}

	private static CallToolResult toResult(Object data)
	{
		try
		{
			return new CallToolResult(List.of(new TextContent(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(data))), false);
		}
		catch (JsonProcessingException e) { return errorResult("JSON error: " + e.getMessage()); }
	}

	private static CallToolResult errorResult(String message)
	{
		return new CallToolResult(List.of(new TextContent(message)), true);
	}
}
