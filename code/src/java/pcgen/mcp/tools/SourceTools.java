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

import pcgen.core.Campaign;
import pcgen.core.GameMode;
import pcgen.mcp.McpSessionManager;
import pcgen.mcp.serialization.FacadeSerializer;

public final class SourceTools
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private SourceTools()
	{
	}

	public static SyncToolSpecification listGameModes(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("list_game_modes",
				"List available game modes (e.g., D&D 3.5e, Pathfinder, D&D 5e)",
				"""
					{
						"type": "object",
						"properties": {},
						"required": []
					}
					"""),
			(exchange, args) -> {
				var modes = FacadeSerializer.serializeList(session.getGameModes(), FacadeSerializer::serializeGameMode);
				return toResult(modes);
			}
		);
	}

	public static SyncToolSpecification listSources(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("list_sources",
				"List available campaign sources for a game mode",
				"""
					{
						"type": "object",
						"properties": {
							"game_mode": {
								"type": "string",
								"description": "Name of the game mode (from list_game_modes)"
							}
						},
						"required": ["game_mode"]
					}
					"""),
			(exchange, args) -> {
				String gameModeName = (String) args.get("game_mode");
				GameMode gameMode = session.findGameMode(gameModeName);
				if (gameMode == null)
				{
					return errorResult("Game mode not found: " + gameModeName);
				}
				var campaigns = FacadeSerializer.serializeList(
					session.getSupportedCampaigns(gameMode), FacadeSerializer::serializeCampaign);
				return toResult(campaigns);
			}
		);
	}

	public static SyncToolSpecification loadSources(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("load_sources",
				"Load game data sources (campaigns) for a game mode. This is required before creating characters.",
				"""
					{
						"type": "object",
						"properties": {
							"game_mode": {
								"type": "string",
								"description": "Name of the game mode"
							},
							"campaigns": {
								"type": "array",
								"items": { "type": "string" },
								"description": "List of campaign keys to load (from list_sources)"
							}
						},
						"required": ["game_mode", "campaigns"]
					}
					"""),
			(exchange, args) -> {
				String gameModeName = (String) args.get("game_mode");
				@SuppressWarnings("unchecked")
				List<String> campaigns = (List<String>) args.get("campaigns");
				try
				{
					McpSessionManager.LoadSourcesResult result =
						session.loadSources(gameModeName, campaigns);
					Map<String, Object> response = new LinkedHashMap<>();
					response.put("source_set_id", result.getSourceSetId());
					response.put("status", "loaded");

					List<Campaign> autoAdded = result.getResolved().getAutoAdded();
					if (!autoAdded.isEmpty())
					{
						List<String> addedKeys = new ArrayList<>();
						for (Campaign c : autoAdded)
						{
							addedKeys.add(c.getKeyName());
						}
						response.put("auto_added_dependencies", addedKeys);
					}

					List<String> warnings = result.getResolved().getWarnings();
					if (!warnings.isEmpty())
					{
						response.put("warnings", warnings);
					}

					return toResult(response);
				}
				catch (Exception e)
				{
					return errorResult("Failed to load sources: " + e.getMessage());
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
