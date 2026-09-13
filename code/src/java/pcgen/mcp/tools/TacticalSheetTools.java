/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package pcgen.mcp.tools;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import pcgen.core.PlayerCharacter;
import pcgen.core.tactics.TacticalSheet;
import pcgen.mcp.McpSessionManager;

/**
 * MCP tools for the tactical sheet: what a character should do round by round
 * and situation by situation. The sheet is written here by an AI agent, shown in
 * the Tactical tab and rendered on the exported character sheet.
 */
public final class TacticalSheetTools
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private TacticalSheetTools()
	{
	}

	public static SyncToolSpecification setTacticalSheet(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("set_tactical_sheet",
				"Replace the tactical sheet of a character: what to do round by round and in which "
					+ "circumstances. Read the character first (get_character_details, get_known_spells, "
					+ "get_equipped_items) so the actions match what the character can actually do. "
					+ "The whole sheet is replaced, so send every section in one call.",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"sections": {
								"type": "array",
								"description": "Sections of the sheet, in reading order",
								"items": {
									"type": "object",
									"properties": {
										"title": {
											"type": "string",
											"description": "Heading of the section, such as 'Opening', 'Rounds 1-2' or 'Emergency'"
										},
										"entries": {
											"type": "array",
											"description": "Lines of the section, in reading order",
											"items": {
												"type": "object",
												"properties": {
													"trigger": {
														"type": "string",
														"description": "The circumstance in plain words: 'Round 1', 'enemy at range', 'HP below 12'"
													},
													"actions": {
														"type": "string",
														"description": "What the character does, with the numbers that matter (attack bonus, damage, save DC)"
													},
													"note": {
														"type": "string",
														"description": "Optional extra detail, such as a caveat or a resource cost"
													}
												},
												"required": ["trigger", "actions"]
											}
										}
									},
									"required": ["title", "entries"]
								}
							}
						},
						"required": ["character_id", "sections"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					PlayerCharacter pc = session.getPlayerCharacter((String) args.get("character_id"));
					TacticalSheet sheet = TacticalSheetPayload.toSheet(args.get("sections"));
					pc.setTacticalSheet(sheet);

					return toResult(Map.of("status", "ok", "sections", sheet.sections().size(),
						"entries", countEntries(sheet)));
				}
				catch (Exception e)
				{
					String msg = e.getMessage();
					return errorResult(msg != null ? msg : e.getClass().getSimpleName());
				}
			}
		);
	}

	public static SyncToolSpecification getTacticalSheet(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("get_tactical_sheet",
				"Get the tactical sheet of a character: what to do round by round and in which circumstances.",
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
					PlayerCharacter pc = session.getPlayerCharacter((String) args.get("character_id"));
					Optional<TacticalSheet> sheet = pc.getDisplay().getTacticalSheet();

					return toResult(Map.of("sections",
						sheet.map(TacticalSheetPayload::toPayload).orElse(List.of())));
				}
				catch (Exception e)
				{
					String msg = e.getMessage();
					return errorResult(msg != null ? msg : e.getClass().getSimpleName());
				}
			}
		);
	}

	public static SyncToolSpecification clearTacticalSheet(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("clear_tactical_sheet", "Remove the tactical sheet of a character.",
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
					PlayerCharacter pc = session.getPlayerCharacter((String) args.get("character_id"));
					pc.clearTacticalSheet();

					return toResult(Map.of("status", "ok"));
				}
				catch (Exception e)
				{
					String msg = e.getMessage();
					return errorResult(msg != null ? msg : e.getClass().getSimpleName());
				}
			}
		);
	}

	private static int countEntries(TacticalSheet sheet)
	{
		return sheet.sections().stream().mapToInt(section -> section.entries().size()).sum();
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
