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

import pcgen.core.Language;
import pcgen.facade.core.CharacterFacade;
import pcgen.facade.core.CompanionSupportFacade;
import pcgen.facade.core.CompanionStubFacade;
import pcgen.facade.core.CompanionFacade;
import pcgen.facade.core.LanguageChooserFacade;
import pcgen.mcp.McpSessionManager;

public final class LanguageCompanionTools
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private LanguageCompanionTools()
	{
	}

	public static SyncToolSpecification getLanguages(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("get_languages",
				"List a character's known languages and available language choosers",
				"""
					{ "type": "object", "properties": { "character_id": { "type": "string" } }, "required": ["character_id"] }
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					List<Map<String, Object>> languages = new ArrayList<>();
					for (Language lang : character.getLanguages())
					{
						Map<String, Object> map = new LinkedHashMap<>();
						map.put("name", lang.getDisplayName());
						map.put("key", lang.getKeyName());
						map.put("automatic", character.isAutomatic(lang));
						map.put("removable", character.isRemovable(lang));
						languages.add(map);
					}

					List<Map<String, Object>> choosers = new ArrayList<>();
					for (LanguageChooserFacade lc : character.getLanguageChoosers())
					{
						choosers.add(Map.of("name", lc.toString()));
					}

					return toResult(Map.of("languages", languages, "choosers", choosers));
				}
				catch (Exception e) { return errorResult(e.getMessage()); }
			}
		);
	}

	public static SyncToolSpecification removeLanguage(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("remove_language",
				"Remove a bonus language from a character",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string" },
							"language_key": { "type": "string", "description": "Language name or key" }
						},
						"required": ["character_id", "language_key"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					String key = (String) args.get("language_key");
					Language found = null;
					for (Language lang : character.getLanguages())
					{
						if (lang.getKeyName().equalsIgnoreCase(key) || lang.getDisplayName().equalsIgnoreCase(key))
						{
							found = lang;
							break;
						}
					}
					if (found == null) return errorResult("Language not found: " + key);
					if (!character.isRemovable(found)) return errorResult("Language is not removable: " + found.getDisplayName());
					character.removeLanguage(found);
					return toResult(Map.of("status", "ok", "removed", found.getDisplayName()));
				}
				catch (Exception e) { return errorResult(e.getMessage()); }
			}
		);
	}

	public static SyncToolSpecification getCompanions(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("get_companions",
				"List a character's companions (familiars, animal companions, cohorts) and available companion types",
				"""
					{ "type": "object", "properties": { "character_id": { "type": "string" } }, "required": ["character_id"] }
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					CompanionSupportFacade support = character.getCompanionSupport();

					List<Map<String, Object>> companions = new ArrayList<>();
					for (CompanionFacade c : support.getCompanions())
					{
						Map<String, Object> map = new LinkedHashMap<>();
						map.put("type", c.getCompanionType());
						map.put("race", c.getRaceRef().get() != null ? c.getRaceRef().get().getDisplayName() : "");
						companions.add(map);
					}

					List<Map<String, Object>> available = new ArrayList<>();
					for (CompanionStubFacade stub : support.getAvailableCompanions())
					{
						Map<String, Object> map = new LinkedHashMap<>();
						map.put("type", stub.getCompanionType());
						map.put("race", stub.getRaceRef().get() != null ? stub.getRaceRef().get().getDisplayName() : "");
						available.add(map);
					}

					Map<String, Integer> maxMap = new LinkedHashMap<>();
					var maxCompanions = support.getMaxCompanionsMap();
					for (String type : maxCompanions.getKeys())
					{
						maxMap.put(type, maxCompanions.getValue(type));
					}

					return toResult(Map.of("companions", companions, "available", available, "maxCompanions", maxMap));
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
