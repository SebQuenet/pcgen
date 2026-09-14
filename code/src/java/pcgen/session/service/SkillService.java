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
package pcgen.session.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import pcgen.core.Skill;
import pcgen.core.analysis.ChooseActivation;
import pcgen.facade.core.CharacterFacade;
import pcgen.facade.core.CharacterLevelFacade;
import pcgen.facade.core.CharacterLevelsFacade;
import pcgen.session.HeadlessUIDelegate;
import pcgen.session.PcgenSession;
import pcgen.session.service.model.BatchOutcome;
import pcgen.session.service.model.LevelSkillPoints;
import pcgen.session.service.model.SkillInvestment;
import pcgen.session.service.model.SkillSummary;
import pcgen.session.service.model.TrainedSkill;

/**
 * Where a character's skill points go.
 *
 * <p>
 * Points are spent per character level, and a skill carrying a CHOOSE token —
 * Linguistics, Craft, Perform, Profession — asks what the rank buys. A caller
 * with an answer passes it in; without one, PCGen takes the first option.
 */
public final class SkillService
{
	private static final String NO_CHOICES_GIVEN =
		"Skill has CHOOSE token but no choices provided - first available option was auto-selected";

	private final PcgenSession session;
	private final CharacterLookup characters;

	public SkillService(PcgenSession session)
	{
		this.session = session;
		this.characters = new CharacterLookup(session);
	}

	public ServiceResult<SkillInvestment> investSkillPoints(String characterId, String skillKey, int points,
		int levelIndex, List<String> choices)
	{
		return characters.byId(characterId).andThen(character ->
			DataSetLookup.require("Skill", skillKey, character.getDataSet().getSkills()).andThen(skill -> {
				CharacterLevelsFacade levels = character.getCharacterLevelsFacade();
				if (levels.getSize() == 0)
				{
					return ServiceResult.failure(new ServiceError.NotAllowed("Character has no class levels"));
				}
				CharacterLevelFacade level = levelAt(levels, levelIndex);
				return invest(session.getDelegate(characterId), levels, level, skill, points, choices);
			}));
	}

	public ServiceResult<SkillSummary> getSkillSummary(String characterId)
	{
		return characters.byId(characterId).map(character -> {
			CharacterLevelsFacade levels = character.getCharacterLevelsFacade();
			List<LevelSkillPoints> perLevel = new ArrayList<>();
			for (int index = 0; index < levels.getSize(); index++)
			{
				CharacterLevelFacade level = levels.getElementAt(index);
				perLevel.add(new LevelSkillPoints(
					index + 1,
					levels.getClassTaken(level) == null ? "" : levels.getClassTaken(level).getDisplayName(),
					levels.getRemainingSkillPoints(level),
					levels.getSpentSkillPoints(level),
					levels.getGainedSkillPoints(level)));
			}
			return new SkillSummary(List.copyOf(perLevel), trainedSkills(character, levels));
		});
	}

	/**
	 * Invest across several levels in one call. An investment that is refused does
	 * not stop the ones after it.
	 */
	public ServiceResult<BatchOutcome> batchInvestSkills(String characterId, List<SkillInvestmentRequest> requests)
	{
		return characters.byId(characterId).map(character -> {
			CharacterLevelsFacade levels = character.getCharacterLevelsFacade();
			HeadlessUIDelegate delegate = session.getDelegate(characterId);
			Map<String, Skill> knownSkills = skillsByEveryNameTheyAnswerTo(character);
			List<String> errors = new ArrayList<>();
			int accepted = 0;

			for (SkillInvestmentRequest request : requests)
			{
				Skill skill = knownSkills.get(request.skillKey().toLowerCase(Locale.ROOT));
				if (skill == null)
				{
					errors.add("Skill not found: " + request.skillKey());
					continue;
				}
				if (request.levelIndex() < 0 || request.levelIndex() >= levels.getSize())
				{
					errors.add("Invalid level index: " + request.levelIndex());
					continue;
				}
				try
				{
					preSelect(delegate, skill, request.choices());
					CharacterLevelFacade level = levels.getElementAt(request.levelIndex());
					if (levels.investSkillPoints(level, skill, request.points()))
					{
						accepted++;
					}
					else
					{
						errors.add("Failed: " + request.skillKey() + " at level " + (request.levelIndex() + 1));
					}
				}
				finally
				{
					clearPreSelection(delegate);
				}
			}
			return new BatchOutcome(accepted, requests.size(), List.copyOf(errors));
		});
	}

	/** One line of a batch: how many points into which skill, at which level. */
	public record SkillInvestmentRequest(String skillKey, int points, int levelIndex, List<String> choices)
	{
	}

	private static ServiceResult<SkillInvestment> invest(HeadlessUIDelegate delegate, CharacterLevelsFacade levels,
		CharacterLevelFacade level, Skill skill, int points, List<String> choices)
	{
		boolean asksAQuestion = ChooseActivation.hasNewChooseToken(skill);
		try
		{
			preSelect(delegate, skill, choices);
			int remainingBefore = levels.getRemainingSkillPoints(level);
			if (!levels.investSkillPoints(level, skill, points))
			{
				return ServiceResult.failure(new ServiceError.NotAllowed(
					"Failed to invest " + points + " points in " + skill.getDisplayName()
						+ ". Remaining points: " + remainingBefore));
			}
			boolean choicesGiven = choices != null && !choices.isEmpty();
			return ServiceResult.success(new SkillInvestment(
				skill.getDisplayName(),
				points,
				levels.getSkillRanks(level, skill),
				levels.getRemainingSkillPoints(level),
				choicesGiven ? List.copyOf(choices) : List.of(),
				asksAQuestion && !choicesGiven ? NO_CHOICES_GIVEN : null));
		}
		finally
		{
			clearPreSelection(delegate);
		}
	}

	private static List<TrainedSkill> trainedSkills(CharacterFacade character, CharacterLevelsFacade levels)
	{
		if (levels.getSize() == 0)
		{
			return List.of();
		}
		CharacterLevelFacade latest = levels.getElementAt(levels.getSize() - 1);
		List<TrainedSkill> trained = new ArrayList<>();
		for (Skill skill : character.getDataSet().getSkills())
		{
			if (levels.getSkillRanks(latest, skill) > 0)
			{
				CharacterLevelsFacade.SkillBreakdown breakdown = levels.getSkillBreakdown(latest, skill);
				trained.add(new TrainedSkill(skill.getKeyName(), skill.getDisplayName(),
					breakdown.ranks, breakdown.modifier, breakdown.total));
			}
		}
		return List.copyOf(trained);
	}

	private static CharacterLevelFacade levelAt(CharacterLevelsFacade levels, int levelIndex)
	{
		boolean outOfRange = levelIndex < 0 || levelIndex >= levels.getSize();
		return levels.getElementAt(outOfRange ? levels.getSize() - 1 : levelIndex);
	}

	private static Map<String, Skill> skillsByEveryNameTheyAnswerTo(CharacterFacade character)
	{
		Map<String, Skill> byName = new LinkedHashMap<>();
		for (Skill skill : character.getDataSet().getSkills())
		{
			byName.put(skill.getKeyName().toLowerCase(Locale.ROOT), skill);
			byName.put(skill.getDisplayName().toLowerCase(Locale.ROOT), skill);
		}
		return byName;
	}

	private static void preSelect(HeadlessUIDelegate delegate, Skill skill, List<String> choices)
	{
		if (delegate != null && choices != null && !choices.isEmpty() && ChooseActivation.hasNewChooseToken(skill))
		{
			delegate.setPreSelectedChoices(choices);
		}
	}

	private static void clearPreSelection(HeadlessUIDelegate delegate)
	{
		if (delegate != null)
		{
			delegate.clearPreSelectedChoices();
		}
	}
}
