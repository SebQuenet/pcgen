package pcgen.mcp.tools;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import pcgen.mcp.McpSessionManager;
import pcgen.mcp.serialization.FacadeSerializer;

public final class CharacterLifecycleTools
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private CharacterLifecycleTools()
	{
	}

	public static SyncToolSpecification createCharacter(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("create_character",
				"Create a new character. Sources must be loaded first via load_sources.",
				"""
					{
						"type": "object",
						"properties": {
							"name": {
								"type": "string",
								"description": "Character name (optional)"
							}
						},
						"required": []
					}
					"""),
			(exchange, args) -> {
				try
				{
					String name = (String) args.get("name");
					String characterId = session.createCharacter(name);
					return toResult(Map.of("character_id", characterId, "status", "created"));
				}
				catch (Exception e)
				{
					return errorResult("Failed to create character: " + e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification getCharacter(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("get_character",
				"Get a full summary of a character including stats, classes, race, alignment, HP, etc.",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": {
								"type": "string",
								"description": "Character ID (from create_character or open_character)"
							}
						},
						"required": ["character_id"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					String characterId = (String) args.get("character_id");
					var character = session.getCharacter(characterId);
					var summary = FacadeSerializer.serializeCharacterSummary(character);
					return toResult(summary);
				}
				catch (Exception e)
				{
					return errorResult("Failed to get character: " + e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification openCharacter(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("open_character",
				"Open an existing character from a .pcg file. Sources must be loaded first. "
					+ "If the character requires different sources (same game mode), sources are "
					+ "automatically merged and all open characters are reopened with new IDs.",
				"""
					{
						"type": "object",
						"properties": {
							"file_path": {
								"type": "string",
								"description": "Path to the .pcg character file"
							}
						},
						"required": ["file_path"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					String filePath = (String) args.get("file_path");
					McpSessionManager.OpenCharacterResult result = session.openCharacter(new File(filePath));

					Map<String, Object> response = new LinkedHashMap<>();
					response.put("character_id", result.getCharacterId());
					response.put("status", "opened");
					response.put("sources_merged", result.isSourcesMerged());
					if (result.isSourcesMerged() && !result.getReopenedCharacters().isEmpty())
					{
						response.put("reopened_characters", result.getReopenedCharacters());
					}
					return toResult(response);
				}
				catch (Exception e)
				{
					return errorResult("Failed to open character: " + e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification saveCharacter(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("save_character",
				"Save a character to its .pcg file",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": {
								"type": "string",
								"description": "Character ID"
							},
							"file_path": {
								"type": "string",
								"description": "Path to save the file (optional, uses default if not set)"
							}
						},
						"required": ["character_id"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					String characterId = (String) args.get("character_id");
					String filePath = (String) args.get("file_path");
					var character = session.getCharacter(characterId);
					if (filePath != null && !filePath.isBlank())
					{
						character.setFile(new File(filePath));
					}
					boolean saved = session.saveCharacter(characterId);
					return toResult(Map.of("status", saved ? "saved" : "failed"));
				}
				catch (Exception e)
				{
					return errorResult("Failed to save character: " + e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification closeCharacter(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("close_character",
				"Close a character and release its resources",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": {
								"type": "string",
								"description": "Character ID"
							}
						},
						"required": ["character_id"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					String characterId = (String) args.get("character_id");
					session.closeCharacter(characterId);
					return toResult(Map.of("status", "closed"));
				}
				catch (Exception e)
				{
					return errorResult("Failed to close character: " + e.getMessage());
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
