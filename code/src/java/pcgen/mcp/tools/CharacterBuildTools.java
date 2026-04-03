package pcgen.mcp.tools;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import pcgen.core.PCAlignment;
import pcgen.core.PCClass;
import pcgen.core.PCStat;
import pcgen.core.Race;
import pcgen.facade.core.CharacterFacade;
import pcgen.facade.core.DataSetFacade;
import pcgen.mcp.McpSessionManager;

public final class CharacterBuildTools
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private CharacterBuildTools()
	{
	}

	public static SyncToolSpecification setName(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("set_name",
				"Set a character's name",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"name": { "type": "string", "description": "New character name" }
						},
						"required": ["character_id", "name"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					character.setName((String) args.get("name"));
					return toResult(Map.of("status", "ok", "name", character.getNameRef().get()));
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification setRace(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("set_race",
				"Set a character's race",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"race_key": { "type": "string", "description": "Race key (from resources or list)" }
						},
						"required": ["character_id", "race_key"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					String raceKey = (String) args.get("race_key");
					DataSetFacade dataSet = character.getDataSet();

					Race found = null;
					for (Race race : dataSet.getRaces())
					{
						if (race.getKeyName().equalsIgnoreCase(raceKey) || race.getDisplayName().equalsIgnoreCase(raceKey))
						{
							found = race;
							break;
						}
					}
					if (found == null)
					{
						return errorResult("Race not found: " + raceKey);
					}
					character.setRace(found);
					return toResult(Map.of("status", "ok", "race", found.getDisplayName()));
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification addClassLevel(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("add_class_level",
				"Add one or more levels of a class to a character",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"class_key": { "type": "string", "description": "Class key (from resources or list)" },
							"levels": { "type": "integer", "description": "Number of levels to add (default: 1)", "default": 1 }
						},
						"required": ["character_id", "class_key"]
					}
					"""),
			(exchange, args) -> {
				CharacterFacade character = null;
				PCClass found = null;
				int levelsBefore = 0;
				try
				{
					character = session.getCharacter((String) args.get("character_id"));
					String classKey = (String) args.get("class_key");
					int levels = args.containsKey("levels") ? ((Number) args.get("levels")).intValue() : 1;
					DataSetFacade dataSet = character.getDataSet();

					for (PCClass pcClass : dataSet.getClasses())
					{
						if (pcClass.getKeyName().equalsIgnoreCase(classKey) || pcClass.getDisplayName().equalsIgnoreCase(classKey))
						{
							found = pcClass;
							break;
						}
					}
					if (found == null)
					{
						return errorResult("Class not found: " + classKey);
					}

					levelsBefore = character.getClassLevel(found);
					PCClass[] classArray = new PCClass[levels];
					Arrays.fill(classArray, found);
					character.addCharacterLevels(classArray);
					int levelsAfter = character.getClassLevel(found);
					int actuallyAdded = levelsAfter - levelsBefore;
					if (actuallyAdded == 0)
					{
						return errorResult("Failed to add class level. Check character prerequisites and abilities.");
					}
					if (actuallyAdded < levels)
					{
						return toResult(Map.of("status", "partial", "class", found.getDisplayName(),
							"levelsAdded", actuallyAdded, "levelsRequested", levels,
							"message", "Only " + actuallyAdded + " of " + levels + " levels were added."));
					}
					return toResult(Map.of("status", "ok", "class", found.getDisplayName(), "levelsAdded", actuallyAdded));
				}
				catch (Exception e)
				{
					// Check if levels were added despite the error
					if (character != null && found != null)
					{
						int levelsAfter = character.getClassLevel(found);
						int actuallyAdded = levelsAfter - levelsBefore;
						if (actuallyAdded > 0)
						{
							return toResult(Map.of("status", "ok_with_warning", "class", found.getDisplayName(),
								"levelsAdded", actuallyAdded,
								"warning", "Levels were added but an error occurred: " + e.getMessage()));
						}
					}
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification setAbilityScore(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("set_ability_score",
				"Set a base ability score (e.g., STR, DEX, CON, INT, WIS, CHA)",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"stat_key": { "type": "string", "description": "Stat key (e.g., STR, DEX)" },
							"score": { "type": "integer", "description": "Base score value" }
						},
						"required": ["character_id", "stat_key", "score"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					String statKey = (String) args.get("stat_key");
					int score = ((Number) args.get("score")).intValue();
					DataSetFacade dataSet = character.getDataSet();

					PCStat found = null;
					for (PCStat stat : dataSet.getStats())
					{
						if (stat.getKeyName().equalsIgnoreCase(statKey) || stat.getDisplayName().equalsIgnoreCase(statKey))
						{
							found = stat;
							break;
						}
					}
					if (found == null)
					{
						return errorResult("Stat not found: " + statKey);
					}
					character.setScoreBase(found, score);
					return toResult(Map.of("status", "ok", "stat", found.getKeyName(), "baseScore", score, "modifier", character.getModTotal(found)));
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification setAlignment(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("set_alignment",
				"Set a character's alignment (e.g., Lawful Good, Chaotic Neutral)",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"alignment_key": { "type": "string", "description": "Alignment key or name" }
						},
						"required": ["character_id", "alignment_key"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					String alignmentKey = (String) args.get("alignment_key");
					DataSetFacade dataSet = character.getDataSet();

					PCAlignment found = null;
					for (PCAlignment alignment : dataSet.getAlignments())
					{
						if (alignment.getKeyName().equalsIgnoreCase(alignmentKey) || alignment.getDisplayName().equalsIgnoreCase(alignmentKey))
						{
							found = alignment;
							break;
						}
					}
					if (found == null)
					{
						return errorResult("Alignment not found: " + alignmentKey);
					}
					character.setAlignment(found);
					return toResult(Map.of("status", "ok", "alignment", found.getDisplayName()));
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
			// Fallback: never report serialization failure as a tool error
			// since the operation may have succeeded
			return new CallToolResult(List.of(new TextContent(data.toString())), false);
		}
	}

	private static CallToolResult errorResult(String message)
	{
		return new CallToolResult(List.of(new TextContent(message)), true);
	}
}
