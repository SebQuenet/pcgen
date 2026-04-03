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

import pcgen.core.Skill;
import pcgen.facade.core.CharacterFacade;
import pcgen.facade.core.CharacterLevelFacade;
import pcgen.facade.core.CharacterLevelsFacade;
import pcgen.facade.core.DataSetFacade;
import pcgen.mcp.McpSessionManager;

public final class SkillTools
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private SkillTools()
	{
	}

	public static SyncToolSpecification investSkillPoints(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("invest_skill_points",
				"Invest skill points in a skill at a specific character level",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"skill_key": { "type": "string", "description": "Skill key or name" },
							"points": { "type": "integer", "description": "Number of skill points to invest" },
							"level_index": { "type": "integer", "description": "Character level index (0-based, defaults to latest level)", "default": -1 }
						},
						"required": ["character_id", "skill_key", "points"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					String skillKey = (String) args.get("skill_key");
					int points = ((Number) args.get("points")).intValue();
					int levelIndex = args.containsKey("level_index") ? ((Number) args.get("level_index")).intValue() : -1;

					DataSetFacade dataSet = character.getDataSet();
					Skill foundSkill = null;
					for (Skill skill : dataSet.getSkills())
					{
						if (skill.getKeyName().equalsIgnoreCase(skillKey) || skill.getDisplayName().equalsIgnoreCase(skillKey))
						{
							foundSkill = skill;
							break;
						}
					}
					if (foundSkill == null)
					{
						return errorResult("Skill not found: " + skillKey);
					}

					CharacterLevelsFacade levels = character.getCharacterLevelsFacade();
					if (levels.getSize() == 0)
					{
						return errorResult("Character has no class levels");
					}

					CharacterLevelFacade level;
					if (levelIndex < 0 || levelIndex >= levels.getSize())
					{
						level = levels.getElementAt(levels.getSize() - 1);
					}
					else
					{
						level = levels.getElementAt(levelIndex);
					}

					int remaining = levels.getRemainingSkillPoints(level);
					boolean success = levels.investSkillPoints(level, foundSkill, points);

					if (!success)
					{
						return errorResult("Failed to invest " + points + " points in " + foundSkill.getDisplayName()
							+ ". Remaining points: " + remaining);
					}

					Map<String, Object> result = new LinkedHashMap<>();
					result.put("status", "ok");
					result.put("skill", foundSkill.getDisplayName());
					result.put("pointsInvested", points);
					result.put("totalRanks", levels.getSkillRanks(level, foundSkill));
					result.put("remainingPoints", levels.getRemainingSkillPoints(level));
					return toResult(result);
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification getSkillSummary(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("get_skill_summary",
				"Get skill ranks, modifiers, and remaining points for a character",
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
					CharacterLevelsFacade levels = character.getCharacterLevelsFacade();
					DataSetFacade dataSet = character.getDataSet();

					List<Map<String, Object>> levelSummaries = new ArrayList<>();
					for (int i = 0; i < levels.getSize(); i++)
					{
						CharacterLevelFacade level = levels.getElementAt(i);
						Map<String, Object> levelMap = new LinkedHashMap<>();
						levelMap.put("level", i + 1);
						levelMap.put("class", levels.getClassTaken(level) != null ? levels.getClassTaken(level).getDisplayName() : "");
						levelMap.put("remainingSkillPoints", levels.getRemainingSkillPoints(level));
						levelMap.put("spentSkillPoints", levels.getSpentSkillPoints(level));
						levelMap.put("gainedSkillPoints", levels.getGainedSkillPoints(level));
						levelSummaries.add(levelMap);
					}

					List<Map<String, Object>> skills = new ArrayList<>();
					if (levels.getSize() > 0)
					{
						CharacterLevelFacade lastLevel = levels.getElementAt(levels.getSize() - 1);
						for (Skill skill : dataSet.getSkills())
						{
							float ranks = levels.getSkillRanks(lastLevel, skill);
							if (ranks > 0)
							{
								CharacterLevelsFacade.SkillBreakdown breakdown = levels.getSkillBreakdown(lastLevel, skill);
								Map<String, Object> skillMap = new LinkedHashMap<>();
								skillMap.put("name", skill.getDisplayName());
								skillMap.put("key", skill.getKeyName());
								skillMap.put("ranks", breakdown.ranks);
								skillMap.put("modifier", breakdown.modifier);
								skillMap.put("total", breakdown.total);
								skills.add(skillMap);
							}
						}
					}

					Map<String, Object> result = new LinkedHashMap<>();
					result.put("levels", levelSummaries);
					result.put("trainedSkills", skills);
					return toResult(result);
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification batchInvestSkills(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("batch_invest_skills",
				"Invest skill points across multiple levels in one call. Each entry specifies a skill and points per level.",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"investments": {
								"type": "array",
								"description": "List of skill investments: [{skill_key, points, level_index}]",
								"items": {
									"type": "object",
									"properties": {
										"skill_key": { "type": "string" },
										"points": { "type": "integer" },
										"level_index": { "type": "integer" }
									},
									"required": ["skill_key", "points", "level_index"]
								}
							}
						},
						"required": ["character_id", "investments"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					@SuppressWarnings("unchecked")
					List<Map<String, Object>> investments = (List<Map<String, Object>>) args.get("investments");
					DataSetFacade dataSet = character.getDataSet();
					CharacterLevelsFacade levels = character.getCharacterLevelsFacade();

					// Cache skill lookups
					Map<String, Skill> skillCache = new java.util.HashMap<>();
					for (Skill skill : dataSet.getSkills())
					{
						skillCache.put(skill.getKeyName().toLowerCase(), skill);
						skillCache.put(skill.getDisplayName().toLowerCase(), skill);
					}

					int successCount = 0;
					List<String> errors = new ArrayList<>();

					for (Map<String, Object> inv : investments)
					{
						String skillKey = (String) inv.get("skill_key");
						int points = ((Number) inv.get("points")).intValue();
						int levelIndex = ((Number) inv.get("level_index")).intValue();

						Skill foundSkill = skillCache.get(skillKey.toLowerCase());
						if (foundSkill == null)
						{
							errors.add("Skill not found: " + skillKey);
							continue;
						}

						if (levelIndex < 0 || levelIndex >= levels.getSize())
						{
							errors.add("Invalid level index: " + levelIndex);
							continue;
						}

						CharacterLevelFacade level = levels.getElementAt(levelIndex);
						boolean success = levels.investSkillPoints(level, foundSkill, points);
						if (success)
						{
							successCount++;
						}
						else
						{
							errors.add("Failed: " + skillKey + " at level " + (levelIndex + 1));
						}
					}

					Map<String, Object> result = new LinkedHashMap<>();
					result.put("status", errors.isEmpty() ? "ok" : "partial");
					result.put("successCount", successCount);
					result.put("totalRequested", investments.size());
					if (!errors.isEmpty())
					{
						result.put("errors", errors);
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
