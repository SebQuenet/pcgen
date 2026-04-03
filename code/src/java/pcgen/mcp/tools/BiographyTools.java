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

import pcgen.cdom.enumeration.Gender;
import pcgen.cdom.enumeration.Handed;
import pcgen.facade.core.CharacterFacade;
import pcgen.mcp.McpSessionManager;

public final class BiographyTools
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private BiographyTools()
	{
	}

	public static SyncToolSpecification setBiography(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("set_biography",
				"Set biography fields for a character (gender, age, height, weight, hair color, eye color, skin color, handed)",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"gender": { "type": "string", "description": "Gender (Male, Female, etc.)" },
							"age": { "type": "integer", "description": "Age in years" },
							"weight": { "type": "integer", "description": "Weight in pounds" },
							"hair_color": { "type": "string", "description": "Hair color" },
							"eye_color": { "type": "string", "description": "Eye color" },
							"skin_color": { "type": "string", "description": "Skin color" },
							"handed": { "type": "string", "description": "Handedness (Right, Left, Ambidextrous)" },
							"players_name": { "type": "string", "description": "Player's real name" }
						},
						"required": ["character_id"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					List<String> updated = new ArrayList<>();

					if (args.containsKey("gender"))
					{
						character.setGender((String) args.get("gender"));
						updated.add("gender");
					}
					if (args.containsKey("age"))
					{
						character.setAge(((Number) args.get("age")).intValue());
						updated.add("age");
					}
					if (args.containsKey("weight"))
					{
						character.setWeight(((Number) args.get("weight")).intValue());
						updated.add("weight");
					}
					if (args.containsKey("hair_color"))
					{
						character.setHairColor((String) args.get("hair_color"));
						updated.add("hair_color");
					}
					if (args.containsKey("eye_color"))
					{
						character.setEyeColor((String) args.get("eye_color"));
						updated.add("eye_color");
					}
					if (args.containsKey("skin_color"))
					{
						character.setSkinColor((String) args.get("skin_color"));
						updated.add("skin_color");
					}
					if (args.containsKey("handed"))
					{
						String handedStr = (String) args.get("handed");
						for (Handed h : character.getAvailableHands())
						{
							if (h.name().equalsIgnoreCase(handedStr) || h.toString().equalsIgnoreCase(handedStr))
							{
								character.setHanded(h);
								updated.add("handed");
								break;
							}
						}
					}
					if (args.containsKey("players_name"))
					{
						character.setPlayersName((String) args.get("players_name"));
						updated.add("players_name");
					}

					return toResult(Map.of("status", "ok", "updated", updated));
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification getBiography(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("get_biography",
				"Get biography details for a character",
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
					Map<String, Object> bio = new LinkedHashMap<>();
					bio.put("name", character.getNameRef().get());
					bio.put("playersName", character.getPlayersNameRef().get());
					bio.put("gender", character.getGenderRef().get() != null ? character.getGenderRef().get().toString() : null);
					bio.put("age", character.getAgeRef().get());
					bio.put("ageCategory", character.getAgeCategoryRef().get());
					bio.put("weight", character.getWeightRef().get());
					bio.put("hairColor", character.getHairColorRef().get());
					bio.put("eyeColor", character.getEyeColorRef().get());
					bio.put("skinColor", character.getSkinColorRef().get());
					bio.put("handed", character.getHandedRef().get() != null ? character.getHandedRef().get().toString() : null);
					return toResult(bio);
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification setXP(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("set_xp",
				"Set a character's experience points",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"xp": { "type": "integer", "description": "Total XP value" }
						},
						"required": ["character_id", "xp"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					int xp = ((Number) args.get("xp")).intValue();
					character.setXP(xp);
					return toResult(Map.of("status", "ok", "xp", character.getXPRef().get(),
						"xpForNextLevel", character.getXPForNextLevelRef().get()));
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
