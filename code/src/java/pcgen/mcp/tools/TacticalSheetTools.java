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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import pcgen.cdom.base.Constants;
import pcgen.core.Equipment;
import pcgen.core.PlayerCharacter;
import pcgen.core.tactics.TacticalParseError;
import pcgen.core.tactics.TacticalParseFailure;
import pcgen.core.tactics.TacticalParseSuccess;
import pcgen.core.tactics.TacticalPlanParser;
import pcgen.core.tactics.TacticalSessionState;
import pcgen.io.tactics.ResolvedSpell;
import pcgen.io.tactics.TacticalResolver;
import pcgen.session.PcgenSession;

/**
 * MCP tools for the tactical sheet: what a character should do round by round
 * and situation by situation.
 *
 * <p>
 * The plan is source text, in the same syntax the Tactical tab shows, so what
 * an agent writes here is what a player reads and corrects there.
 */
public final class TacticalSheetTools
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final String SYNTAX = """
		A plan is line oriented text.

		'## title' opens a section. At column zero, one of these opens a block:
		  step: <when>              then '  do: <what to do>' and optionally '  note: <detail>'
		  note: <title>             then one or more indented lines of prose
		  resource: <label> | <maximum> | <action>      action is optional
		  attack: <@weapon(name)>   then optionally '  target: <kind> | <what changes>' and '  note: <detail>'
		  attack: <name> | <to hit> | <damage> | <crit>  all four fields when written out
		  creature: <name> | <source> | <duration>      then one or more '  row: <heading> | <contents>'
		  spells: @prepared                             then optionally '  tag: <spell> | <tag>, <tag>'

		Fields are separated by '|'. Two spaces of indentation attach a line to the
		block above it. In a single line field, the two characters \\n mean a line break.

		'@weapon(name)' and '@var(name)' read the character's own numbers, so the sheet
		follows a level up without being rewritten. Call list_tactical_references to see
		what those names may be. Anything else is written out as text.
		""";

	private TacticalSheetTools()
	{
	}

	public static SyncToolSpecification setTacticalSheet(PcgenSession session)
	{
		return new SyncToolSpecification(
			new Tool("set_tactical_sheet",
				"Write the tactical sheet of a character, as source text. Refuses text that does not read, "
					+ "naming the offending line.\n\n" + SYNTAX,
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"source": { "type": "string", "description": "The plan's source text" }
						},
						"required": ["character_id", "source"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					PlayerCharacter pc = session.getPlayerCharacter((String) args.get("character_id"));
					String source = (String) args.get("source");
					if (source == null || source.isBlank())
					{
						return errorResult("A tactical sheet needs source text; use clear_tactical_sheet to remove one");
					}
					return switch (TacticalPlanParser.parse(source))
					{
						case TacticalParseSuccess success ->
						{
							pc.setTacticalPlan(source);
							yield toResult(Map.of("status", "ok", "sections", success.sheet().sections().size(),
								"blocks", success.sheet().sections().stream()
									.mapToInt(section -> section.blocks().size()).sum()));
						}
						case TacticalParseFailure failure -> errorResult("The plan does not read:\n" + listed(failure));
					};
				}
				catch (Exception e)
				{
					String msg = e.getMessage();
					return errorResult(msg != null ? msg : e.getClass().getSimpleName());
				}
			}
		);
	}

	public static SyncToolSpecification getTacticalSheet(PcgenSession session)
	{
		return new SyncToolSpecification(
			new Tool("get_tactical_sheet",
				"Get the tactical sheet of a character as source text, together with what the session has used up.",
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
					TacticalSessionState played = pc.getDisplay().getTacticalSession();
					return toResult(Map.of("source", pc.getDisplay().getTacticalPlan().orElse(""), "damageTaken",
						played.damageTaken(), "resourcesSpent", played.resourcesSpent()));
				}
				catch (Exception e)
				{
					String msg = e.getMessage();
					return errorResult(msg != null ? msg : e.getClass().getSimpleName());
				}
			}
		);
	}

	public static SyncToolSpecification clearTacticalSheet(PcgenSession session)
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

	/**
	 * Without this, an agent guesses the names behind {@code @weapon(...)} and
	 * {@code @var(...)}, and the reference breaks when the sheet is rendered.
	 *
	 * @param session the MCP session holding the open characters
	 * @return the tool that lists what a plan may point at
	 */
	public static SyncToolSpecification listTacticalReferences(PcgenSession session)
	{
		return new SyncToolSpecification(
			new Tool("list_tactical_references",
				"List what a tactical sheet may point at with @weapon(...) and @var(...), plus the spells "
					+ "a spells block would show. Use these names verbatim.",
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
					return toResult(Map.of("weapons", weaponsOf(pc), "variables", variablesOf(pc), "preparedSpells",
						spellsOf(pc, pcgen.core.tactics.SpellSource.PREPARED), "knownSpells",
						spellsOf(pc, pcgen.core.tactics.SpellSource.KNOWN)));
				}
				catch (Exception e)
				{
					String msg = e.getMessage();
					return errorResult(msg != null ? msg : e.getClass().getSimpleName());
				}
			}
		);
	}

	private static List<Map<String, Object>> weaponsOf(PlayerCharacter pc)
	{
		TacticalResolver resolver = new TacticalResolver(pc);
		List<Map<String, Object>> weapons = new ArrayList<>();
		for (Equipment weapon : pc.getExpandedWeapons(Constants.MERGE_ALL))
		{
			resolver.weapon(weapon.getName()).ifPresent(resolved -> weapons.add(Map.of("name", resolved.name(),
				"toHit", resolved.toHit(), "damage", resolved.damage(), "critical", resolved.critical())));
		}
		return weapons;
	}

	private static List<String> variablesOf(PlayerCharacter pc)
	{
		return pc.getVariableNames().stream().sorted().toList();
	}

	private static List<Map<String, Object>> spellsOf(PlayerCharacter pc, pcgen.core.tactics.SpellSource source)
	{
		List<Map<String, Object>> spells = new ArrayList<>();
		for (ResolvedSpell spell : new TacticalResolver(pc).spells(source))
		{
			spells.add(Map.of("name", spell.name(), "level", spell.level(), "times", spell.times()));
		}
		return spells;
	}

	private static String listed(TacticalParseFailure failure)
	{
		StringBuilder listed = new StringBuilder();
		for (TacticalParseError error : failure.errors())
		{
			listed.append("line ").append(error.line()).append(": ").append(error.message()).append('\n');
		}
		return listed.toString();
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
