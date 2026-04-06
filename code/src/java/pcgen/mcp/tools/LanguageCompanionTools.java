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
import pcgen.core.Race;
import pcgen.facade.core.CharacterFacade;
import pcgen.facade.core.CompanionSupportFacade;
import pcgen.facade.core.CompanionStubFacade;
import pcgen.facade.core.CompanionFacade;
import pcgen.facade.core.LanguageChooserFacade;
import pcgen.mcp.McpSessionManager;
import pcgen.mcp.McpUIDelegate;
import pcgen.system.CharacterManager;
import pcgen.util.chooser.ChooserFactory;

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
						Map<String, Object> chooserMap = new LinkedHashMap<>();
						chooserMap.put("name", lc.getName());
						chooserMap.put("remaining", lc.getRemainingSelections().get());
						List<String> available = new ArrayList<>();
						for (Language lang : lc.getAvailableList())
						{
							available.add(lang.getDisplayName());
						}
						chooserMap.put("available", available);
						List<String> selected = new ArrayList<>();
						for (Language lang : lc.getSelectedList())
						{
							selected.add(lang.getDisplayName());
						}
						chooserMap.put("selected", selected);
						choosers.add(chooserMap);
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

	public static SyncToolSpecification addLanguage(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("add_language",
				"Add a bonus language to a character by selecting from an available language chooser",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"language_key": { "type": "string", "description": "Language name or key (from get_languages chooser available list)" }
						},
						"required": ["character_id", "language_key"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					String key = (String) args.get("language_key");

					for (LanguageChooserFacade chooser : character.getLanguageChoosers())
					{
						if (chooser.getRemainingSelections().get() <= 0)
						{
							continue;
						}
						for (Language lang : chooser.getAvailableList())
						{
							if (lang.getKeyName().equalsIgnoreCase(key) || lang.getDisplayName().equalsIgnoreCase(key))
							{
								chooser.addSelected(lang);
								chooser.commit();
								return toResult(Map.of("status", "ok", "added", lang.getDisplayName(),
									"chooser", chooser.getName()));
							}
						}
					}
					return errorResult("Language not found or no available chooser with remaining selections: " + key);
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

	public static SyncToolSpecification addCompanion(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("add_companion",
				"Add an animal companion, familiar, or mount to a character by selecting from available companion races",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"companion_type": { "type": "string", "description": "Companion type (e.g., 'Animal Companion', 'Familiar')" },
							"companion_race": { "type": "string", "description": "Race of the companion (from get_companions available list)" }
						},
						"required": ["character_id", "companion_type", "companion_race"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					String characterId = (String) args.get("character_id");
					String companionType = (String) args.get("companion_type");
					String companionRace = (String) args.get("companion_race");

					CharacterFacade character = session.getCharacter(characterId);
					CompanionSupportFacade support = character.getCompanionSupport();

					// Find the matching stub
					CompanionStubFacade matchedStub = null;
					for (CompanionStubFacade stub : support.getAvailableCompanions())
					{
						Race race = (Race) stub.getRaceRef().get();
						if (race != null && stub.getCompanionType().equals(companionType)
							&& (race.getDisplayName().equalsIgnoreCase(companionRace)
								|| race.getKeyName().equalsIgnoreCase(companionRace)))
						{
							matchedStub = stub;
							break;
						}
					}
					if (matchedStub == null)
					{
						return errorResult("No available companion found for type '" + companionType
							+ "' with race '" + companionRace + "'");
					}

					// Create a new character for the companion
					McpUIDelegate delegate = new McpUIDelegate();
					ChooserFactory.setDelegate(delegate);
					CharacterFacade newCompanion = CharacterManager.createNewCharacter(delegate, session.getCurrentDataSet());
					if (newCompanion == null)
					{
						return errorResult("Failed to create companion character");
					}

					// Set the companion's race and link it to the master
					Race selectedRace = (Race) matchedStub.getRaceRef().get();
					newCompanion.setRace(selectedRace);
					support.addCompanion(newCompanion, companionType);

					return toResult(Map.of(
						"status", "ok",
						"companion_type", companionType,
						"companion_race", selectedRace.getDisplayName()
					));
				}
				catch (Exception e) { return errorResult(e.getMessage()); }
			}
		);
	}

	public static SyncToolSpecification removeCompanion(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("remove_companion",
				"Remove a companion (animal companion, familiar, mount, follower) from a character",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"companion_type": { "type": "string", "description": "Companion type (e.g., 'Animal Companion', 'Familiar', 'Follower')" },
							"companion_race": { "type": "string", "description": "Race of the companion to remove (optional, uses first match if omitted)" }
						},
						"required": ["character_id", "companion_type"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					String companionType = (String) args.get("companion_type");
					String companionRace = (String) args.get("companion_race");
					CompanionSupportFacade support = character.getCompanionSupport();

					CompanionFacade matched = null;
					for (CompanionFacade c : support.getCompanions())
					{
						if (c.getCompanionType().equalsIgnoreCase(companionType))
						{
							if (companionRace == null || companionRace.isBlank()
								|| (c.getRaceRef().get() != null
									&& c.getRaceRef().get().getDisplayName().equalsIgnoreCase(companionRace)))
							{
								matched = c;
								break;
							}
						}
					}
					if (matched == null)
					{
						return errorResult("No companion found for type '" + companionType
							+ "'" + (companionRace != null ? " with race '" + companionRace + "'" : ""));
					}

					String removedRace = matched.getRaceRef().get() != null
						? matched.getRaceRef().get().getDisplayName() : "Unknown";
					support.removeCompanion(matched);
					return toResult(Map.of("status", "ok", "removed_type", companionType, "removed_race", removedRace));
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
