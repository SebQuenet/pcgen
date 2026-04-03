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

import pcgen.mcp.McpSessionManager;
import pcgen.mcp.PendingChoice;

public final class ChoiceTools
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private ChoiceTools()
	{
	}

	public static SyncToolSpecification getPendingChoices(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("get_pending_choices",
				"Get all pending choices that need to be resolved (e.g., skill selection for Skill Focus feat)",
				"""
					{
						"type": "object",
						"properties": {},
						"required": []
					}
					"""),
			(exchange, args) -> {
				try
				{
					Map<String, PendingChoice> choices = session.getAllPendingChoices();
					List<Map<String, Object>> result = new ArrayList<>();
					for (var entry : choices.entrySet())
					{
						PendingChoice pc = entry.getValue();
						Map<String, Object> choiceMap = new LinkedHashMap<>();
						choiceMap.put("choice_id", pc.choiceId());
						choiceMap.put("title", pc.title());
						choiceMap.put("available_options", pc.availableOptions());
						choiceMap.put("remaining_selections", pc.remainingSelections());
						choiceMap.put("require_complete", pc.requireCompleteSelection());
						result.add(choiceMap);
					}
					return toResult(result);
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification resolveChoice(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("resolve_choice",
				"Resolve a pending choice by providing selections (e.g., choose a skill for Skill Focus)",
				"""
					{
						"type": "object",
						"properties": {
							"choice_id": {
								"type": "string",
								"description": "ID of the pending choice (from get_pending_choices)"
							},
							"selections": {
								"type": "array",
								"items": { "type": "string" },
								"description": "List of selected option names"
							}
						},
						"required": ["choice_id", "selections"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					String choiceId = (String) args.get("choice_id");
					@SuppressWarnings("unchecked")
					List<String> selections = (List<String>) args.get("selections");

					boolean resolved = session.resolveChoice(choiceId, selections);
					if (!resolved)
					{
						return errorResult("Choice not found: " + choiceId);
					}
					return toResult(Map.of("status", "resolved"));
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
