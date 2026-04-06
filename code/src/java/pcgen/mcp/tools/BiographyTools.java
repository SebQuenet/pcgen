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

import pcgen.cdom.enumeration.Gender;
import pcgen.cdom.enumeration.Handed;
import pcgen.cdom.enumeration.PCStringKey;
import pcgen.core.NoteItem;
import pcgen.facade.core.CharacterFacade;
import pcgen.facade.core.DescriptionFacade;
import pcgen.mcp.McpSessionManager;

public final class BiographyTools
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private BiographyTools()
	{
	}

	private static final Map<String, PCStringKey> NOTE_FIELD_MAP = Map.of(
		"bio", PCStringKey.BIO,
		"description", PCStringKey.DESCRIPTION,
		"companions", PCStringKey.COMPANIONS,
		"assets", PCStringKey.ASSETS,
		"magic", PCStringKey.MAGIC,
		"gm_notes", PCStringKey.GMNOTES
	);

	public static SyncToolSpecification setBiography(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("set_biography",
				"Set biography fields for a character. Supports basic fields (gender, age, etc.) "
					+ "and text note fields (bio, description, companions, assets, magic, gm_notes).",
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
							"players_name": { "type": "string", "description": "Player's real name" },
							"bio": { "type": "string", "description": "Character biography / backstory text" },
							"description": { "type": "string", "description": "Physical description of the character" },
							"companions": { "type": "string", "description": "Notes about companions and allies" },
							"assets": { "type": "string", "description": "Notes about other assets and possessions" },
							"magic": { "type": "string", "description": "Notes about magic items and effects" },
							"gm_notes": { "type": "string", "description": "GM-only notes" }
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

					// Handle text note fields (bio, description, companions, assets, magic, gm_notes)
					DescriptionFacade descFacade = character.getDescriptionFacade();
					for (Map.Entry<String, PCStringKey> entry : NOTE_FIELD_MAP.entrySet())
					{
						String fieldName = entry.getKey();
						if (args.containsKey(fieldName))
						{
							String text = (String) args.get(fieldName);
							PCStringKey targetKey = entry.getValue();
							for (NoteItem note : descFacade.getNotes())
							{
								if (note.getPCStringKey().isPresent() && note.getPCStringKey().get() == targetKey)
								{
									descFacade.setNote(note, text);
									updated.add(fieldName);
									break;
								}
							}
						}
					}

					return toResult(Map.of("status", "ok", "updated", updated));
				}
				catch (Exception e)
				{
					String msg = e.getMessage();
					return errorResult(msg != null ? msg : e.getClass().getSimpleName());
				}
			}
		);
	}

	public static SyncToolSpecification getBiography(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("get_biography",
				"Get biography details for a character, including basic fields and text notes "
					+ "(bio, description, companions, assets, magic, gm_notes)",
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

					// Include text note fields
					DescriptionFacade descFacade = character.getDescriptionFacade();
					Map<PCStringKey, String> keyToField = new LinkedHashMap<>();
					for (Map.Entry<String, PCStringKey> entry : NOTE_FIELD_MAP.entrySet())
					{
						keyToField.put(entry.getValue(), entry.getKey());
					}
					for (NoteItem note : descFacade.getNotes())
					{
						if (note.getPCStringKey().isPresent())
						{
							String fieldName = keyToField.get(note.getPCStringKey().get());
							if (fieldName != null)
							{
								bio.put(fieldName, note.getValue());
							}
						}
					}

					return toResult(bio);
				}
				catch (Exception e)
				{
					String msg = e.getMessage();
					return errorResult(msg != null ? msg : e.getClass().getSimpleName());
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
