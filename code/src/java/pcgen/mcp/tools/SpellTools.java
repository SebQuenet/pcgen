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
import pcgen.facade.core.SpellFacade;
import pcgen.facade.core.SpellSupportFacade;
import pcgen.facade.core.SpellSupportFacade.SpellNode;
import pcgen.facade.core.SpellSupportFacade.SuperNode;
import pcgen.mcp.McpSessionManager;

public final class SpellTools
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private SpellTools()
	{
	}

	public static SyncToolSpecification getAvailableSpells(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("get_available_spells",
				"List spells available for a character to learn, optionally filtered by class and level",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"class_filter": { "type": "string", "description": "Filter by spellcasting class name (optional)" },
							"level_filter": { "type": "string", "description": "Filter by spell level (optional, e.g. '0', '1', '2')" }
						},
						"required": ["character_id"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					SpellSupportFacade spellSupport = character.getSpellSupport();
					String classFilter = (String) args.get("class_filter");
					String levelFilter = (String) args.get("level_filter");

					var available = spellSupport.getAvailableSpellNodes();
					List<Map<String, Object>> spells = new ArrayList<>();
					for (SuperNode node : available)
					{
						if (node instanceof SpellNode spellNode && spellNode.getSpell() != null)
						{
							if (classFilter != null && spellNode.getSpellcastingClass() != null
								&& !spellNode.getSpellcastingClass().getDisplayName().equalsIgnoreCase(classFilter)
								&& !spellNode.getSpellcastingClass().getKeyName().equalsIgnoreCase(classFilter))
							{
								continue;
							}
							if (levelFilter != null && !levelFilter.equals(spellNode.getSpellLevel()))
							{
								continue;
							}
							spells.add(serializeSpellNode(spellNode));
						}
					}
					return toResult(spells);
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification getKnownSpells(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("get_known_spells",
				"List all spells known by a character",
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
					SpellSupportFacade spellSupport = character.getSpellSupport();
					var known = spellSupport.getAllKnownSpellNodes();
					List<Map<String, Object>> spells = new ArrayList<>();
					for (SuperNode node : known)
					{
						if (node instanceof SpellNode spellNode && spellNode.getSpell() != null)
						{
							spells.add(serializeSpellNode(spellNode));
						}
					}
					return toResult(spells);
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification addKnownSpell(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("add_known_spell",
				"Add a spell to a character's known spells",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"spell_name": { "type": "string", "description": "Spell name or key" },
							"class_key": { "type": "string", "description": "Spellcasting class (optional, auto-detected if only one)" },
							"spell_level": { "type": "string", "description": "Spell level (optional, auto-detected)" }
						},
						"required": ["character_id", "spell_name"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					SpellSupportFacade spellSupport = character.getSpellSupport();
					String spellName = (String) args.get("spell_name");
					String classKey = (String) args.get("class_key");
					String spellLevel = (String) args.get("spell_level");

					SpellNode found = findAvailableSpell(spellSupport, spellName, classKey, spellLevel);
					if (found == null)
					{
						return errorResult("Spell not found in available spells: " + spellName);
					}

					spellSupport.addKnownSpell(found);
					return toResult(Map.of("status", "ok", "spell", found.getSpell().toString(),
						"class", found.getSpellcastingClass() != null ? found.getSpellcastingClass().getDisplayName() : "",
						"level", found.getSpellLevel()));
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification removeKnownSpell(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("remove_known_spell",
				"Remove a spell from a character's known spells",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"spell_name": { "type": "string", "description": "Spell name or key" }
						},
						"required": ["character_id", "spell_name"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					SpellSupportFacade spellSupport = character.getSpellSupport();
					String spellName = (String) args.get("spell_name");

					SpellNode found = findKnownSpell(spellSupport, spellName);
					if (found == null)
					{
						return errorResult("Spell not found in known spells: " + spellName);
					}
					spellSupport.removeKnownSpell(found);
					return toResult(Map.of("status", "ok", "removed", found.getSpell().toString()));
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification getPreparedSpells(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("get_prepared_spells",
				"List all prepared spells for a character",
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
					SpellSupportFacade spellSupport = character.getSpellSupport();
					var prepared = spellSupport.getPreparedSpellNodes();
					List<Map<String, Object>> spells = new ArrayList<>();
					for (SuperNode node : prepared)
					{
						if (node instanceof SpellNode spellNode && spellNode.getSpell() != null)
						{
							Map<String, Object> map = serializeSpellNode(spellNode);
							map.put("count", spellNode.getCount());
							if (spellNode.getRootNode() != null)
							{
								map.put("spellList", spellNode.getRootNode().getName());
							}
							spells.add(map);
						}
					}
					return toResult(spells);
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification addPreparedSpell(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("add_prepared_spell",
				"Prepare a known spell (add to prepared spell list)",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"spell_name": { "type": "string", "description": "Spell name" },
							"spell_list": { "type": "string", "description": "Prepared spell list name (optional, uses default)" },
							"class_key": { "type": "string", "description": "Spellcasting class (optional)" }
						},
						"required": ["character_id", "spell_name"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					SpellSupportFacade spellSupport = character.getSpellSupport();
					String spellName = (String) args.get("spell_name");
					String spellList = (String) args.get("spell_list");
					String classKey = (String) args.get("class_key");

					SpellNode found = findKnownSpell(spellSupport, spellName);
					if (found == null)
					{
						found = findAvailableSpell(spellSupport, spellName, classKey, null);
					}
					if (found == null)
					{
						return errorResult("Spell not found: " + spellName);
					}

					if (spellList == null || spellList.isBlank())
					{
						var ref = spellSupport.getDefaultSpellBookRef();
						spellList = ref != null && ref.get() != null ? ref.get() : "Prepared Spells";
					}

					spellSupport.addPreparedSpell(found, spellList, false);
					return toResult(Map.of("status", "ok", "spell", found.getSpell().toString(), "spellList", spellList));
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification removePreparedSpell(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("remove_prepared_spell",
				"Remove a spell from prepared spell list",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"spell_name": { "type": "string", "description": "Spell name" },
							"spell_list": { "type": "string", "description": "Prepared spell list name (optional, uses default)" }
						},
						"required": ["character_id", "spell_name"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					SpellSupportFacade spellSupport = character.getSpellSupport();
					String spellName = (String) args.get("spell_name");
					String spellList = (String) args.get("spell_list");

					SpellNode found = findPreparedSpell(spellSupport, spellName);
					if (found == null)
					{
						return errorResult("Spell not in prepared list: " + spellName);
					}

					if (spellList == null || spellList.isBlank())
					{
						spellList = found.getRootNode() != null ? found.getRootNode().getName() : "Prepared Spells";
					}

					spellSupport.removePreparedSpell(found, spellList);
					return toResult(Map.of("status", "ok", "removed", found.getSpell().toString()));
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification getSpellbooks(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("get_spellbooks",
				"List spellbooks and their contents",
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
					SpellSupportFacade spellSupport = character.getSpellSupport();

					List<String> books = new ArrayList<>();
					for (String book : spellSupport.getSpellbooks())
					{
						books.add(book);
					}

					var bookNodes = spellSupport.getBookSpellNodes();
					List<Map<String, Object>> bookSpells = new ArrayList<>();
					for (SuperNode node : bookNodes)
					{
						if (node instanceof SpellNode spellNode && spellNode.getSpell() != null)
						{
							Map<String, Object> map = serializeSpellNode(spellNode);
							if (spellNode.getRootNode() != null)
							{
								map.put("book", spellNode.getRootNode().getName());
							}
							bookSpells.add(map);
						}
					}

					Map<String, Object> result = new LinkedHashMap<>();
					result.put("spellbooks", books);
					result.put("spells", bookSpells);
					return toResult(result);
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification addToSpellbook(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("add_to_spellbook",
				"Add a known spell to a spellbook",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"spell_name": { "type": "string", "description": "Spell name" },
							"spellbook": { "type": "string", "description": "Spellbook name" }
						},
						"required": ["character_id", "spell_name", "spellbook"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					SpellSupportFacade spellSupport = character.getSpellSupport();
					String spellName = (String) args.get("spell_name");
					String spellbook = (String) args.get("spellbook");

					SpellNode found = findKnownSpell(spellSupport, spellName);
					if (found == null)
					{
						return errorResult("Spell not found in known spells: " + spellName);
					}
					spellSupport.addToSpellBook(found, spellbook);
					return toResult(Map.of("status", "ok", "spell", found.getSpell().toString(), "spellbook", spellbook));
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	private static SpellNode findAvailableSpell(SpellSupportFacade support, String name, String classKey, String level)
	{
		for (SuperNode node : support.getAvailableSpellNodes())
		{
			if (node instanceof SpellNode sn && sn.getSpell() != null)
			{
				if (!sn.getSpell().toString().equalsIgnoreCase(name) && !sn.getSpell().getKeyName().equalsIgnoreCase(name))
				{
					continue;
				}
				if (classKey != null && sn.getSpellcastingClass() != null
					&& !sn.getSpellcastingClass().getKeyName().equalsIgnoreCase(classKey)
					&& !sn.getSpellcastingClass().getDisplayName().equalsIgnoreCase(classKey))
				{
					continue;
				}
				if (level != null && !level.equals(sn.getSpellLevel()))
				{
					continue;
				}
				return sn;
			}
		}
		return null;
	}

	private static SpellNode findKnownSpell(SpellSupportFacade support, String name)
	{
		for (SuperNode node : support.getAllKnownSpellNodes())
		{
			if (node instanceof SpellNode sn && sn.getSpell() != null
				&& (sn.getSpell().toString().equalsIgnoreCase(name) || sn.getSpell().getKeyName().equalsIgnoreCase(name)))
			{
				return sn;
			}
		}
		return null;
	}

	private static SpellNode findPreparedSpell(SpellSupportFacade support, String name)
	{
		for (SuperNode node : support.getPreparedSpellNodes())
		{
			if (node instanceof SpellNode sn && sn.getSpell() != null
				&& (sn.getSpell().toString().equalsIgnoreCase(name) || sn.getSpell().getKeyName().equalsIgnoreCase(name)))
			{
				return sn;
			}
		}
		return null;
	}

	private static Map<String, Object> serializeSpellNode(SpellNode node)
	{
		Map<String, Object> map = new LinkedHashMap<>();
		SpellFacade spell = node.getSpell();
		map.put("name", spell.toString());
		map.put("key", spell.getKeyName());
		map.put("level", node.getSpellLevel());
		if (node.getSpellcastingClass() != null)
		{
			map.put("class", node.getSpellcastingClass().getDisplayName());
		}
		map.put("school", spell.getSchool());
		map.put("subschool", spell.getSubschool());
		map.put("components", spell.getComponents());
		map.put("range", spell.getRange());
		map.put("duration", spell.getDuration());
		map.put("castTime", spell.getCastTime());
		return map;
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
