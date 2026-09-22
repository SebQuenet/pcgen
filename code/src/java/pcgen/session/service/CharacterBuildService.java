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
import java.util.Locale;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pcgen.core.PCClass;
import pcgen.core.PCStat;
import pcgen.facade.core.CharacterFacade;
import pcgen.cdom.enumeration.ListKey;
import pcgen.core.SubClass;
import pcgen.session.HeadlessUIDelegate;
import pcgen.session.PcgenSession;
import pcgen.session.service.model.AbilityScoreSet;
import pcgen.session.service.model.BatchOutcome;
import pcgen.session.service.model.AlignmentSet;
import pcgen.session.service.model.CharacterName;
import pcgen.session.service.model.LevelsAdded;
import pcgen.session.service.model.RaceSet;

/**
 * The choices that make a character who they are: name, race, class levels,
 * ability scores, alignment.
 */
public final class CharacterBuildService
{
	private static final String STATUS_OK = "ok";

	private final CharacterLookup characters;
	private final PcgenSession session;

	public CharacterBuildService(PcgenSession session)
	{
		this.characters = new CharacterLookup(session);
		this.session = session;
	}

	public ServiceResult<CharacterName> setName(String characterId, String name)
	{
		return characters.byId(characterId).map(character -> {
			character.setName(name);
			return new CharacterName(character.getNameRef().get());
		});
	}

	public ServiceResult<RaceSet> setRace(String characterId, String raceKey)
	{
		return characters.byId(characterId).andThen(character ->
			DataSetLookup.require("Race", raceKey, character.getDataSet().getRaces()).map(race -> {
				character.setRace(race);
				return new RaceSet(race.getDisplayName());
			}));
	}

	public ServiceResult<AlignmentSet> setAlignment(String characterId, String alignmentKey)
	{
		return characters.byId(characterId).andThen(character ->
			DataSetLookup.require("Alignment", alignmentKey, character.getDataSet().getAlignments())
				.map(alignment -> {
					character.setAlignment(alignment);
					return new AlignmentSet(alignment.getDisplayName());
				}));
	}

	public ServiceResult<AbilityScoreSet> setAbilityScore(String characterId, String statKey, int score)
	{
		return characters.byId(characterId).andThen(character ->
			DataSetLookup.require("Stat", statKey, character.getDataSet().getStats()).map(stat -> {
				character.setScoreBase(stat, score);
				return new AbilityScoreSet(stat.getKeyName(), score, character.getModTotal(stat));
			}));
	}

	/**
	 * Set several scores in one call. A stat that cannot be set does not stop the
	 * others: what failed comes back in the result rather than as a failed call.
	 */
	public ServiceResult<BatchOutcome> setAllAbilityScores(String characterId, Map<String, Integer> scores)
	{
		return characters.byId(characterId).map(character -> {
			Map<String, PCStat> knownStats = statsByEveryNameTheyAnswerTo(character);
			List<String> errors = new ArrayList<>();
			int accepted = 0;

			for (Map.Entry<String, Integer> wanted : scores.entrySet())
			{
				PCStat stat = knownStats.get(wanted.getKey().toUpperCase(Locale.ROOT));
				if (stat == null)
				{
					errors.add("Stat not found: " + wanted.getKey());
					continue;
				}
				try
				{
					character.setScoreBase(stat, wanted.getValue());
					accepted++;
				}
				catch (RuntimeException e)
				{
					errors.add("Failed: " + wanted.getKey() + " - " + e.getMessage());
				}
			}
			return new BatchOutcome(accepted, scores.size(), List.copyOf(errors));
		});
	}

	/**
	 * Add levels of a class.
	 *
	 * <p>
	 * PCGen may take fewer levels than were asked for, and may raise the character's
	 * level and then fail on something that follows. Both are reported as a
	 * successful call carrying a note, because the levels really were added and a
	 * caller told otherwise would add them twice.
	 */
	public ServiceResult<LevelsAdded> addClassLevel(String characterId, String classKey, int levels)
	{
		return addClassLevel(characterId, classKey, levels, null);
	}

	/**
	 * Add levels of a class, naming the subclass when the class has them.
	 *
	 * <p>
	 * A wizard's arcane school, a cleric's order and anything else PCGen models
	 * as a SUBCLASS is asked for at the first level through a chooser. Headless,
	 * that chooser answers itself with the first entry, which is why every
	 * wizard came out an abjurer. Naming the subclass here hands the chooser the
	 * answer before it is raised.
	 */
	public ServiceResult<LevelsAdded> addClassLevel(String characterId, String classKey, int levels,
		String subclassKey)
	{
		return characters.byId(characterId).andThen(character ->
			DataSetLookup.require("Class", classKey, character.getDataSet().getClasses())
				.andThen(pcClass -> requireSubclass(pcClass, subclassKey)
					.andThen(subclass -> addLevels(character,
						session == null ? null : session.getDelegate(characterId), pcClass, levels, subclass))));
	}

	/**
	 * The subclass PCGen holds under that key, or none when the caller named none.
	 */
	private static ServiceResult<String> requireSubclass(PCClass pcClass, String subclassKey)
	{
		if (subclassKey == null || subclassKey.isBlank())
		{
			return ServiceResult.success(null);
		}
		List<SubClass> subclasses = pcClass.getListFor(ListKey.SUB_CLASS);
		if (subclasses == null || subclasses.isEmpty())
		{
			return ServiceResult.failure(new ServiceError.NotAllowed(
				pcClass.getDisplayName() + " has no subclasses to choose from."));
		}
		for (SubClass subclass : subclasses)
		{
			if (subclass.getKeyName().equalsIgnoreCase(subclassKey)
				|| subclass.getDisplayName().equalsIgnoreCase(subclassKey))
			{
				return ServiceResult.success(subclass.getKeyName());
			}
		}
		return ServiceResult.failure(new ServiceError.EntryNotFound("Subclass of " + pcClass.getDisplayName(),
			subclassKey + " (offered: "
				+ subclasses.stream().map(SubClass::getKeyName).collect(Collectors.joining(", ")) + ")"));
	}

	private static ServiceResult<LevelsAdded> addLevels(CharacterFacade character, HeadlessUIDelegate delegate,
		PCClass pcClass, int levels, String subclassKey)
	{
		int before = character.getClassLevel(pcClass);
		try
		{
			if (delegate != null && subclassKey != null)
			{
				delegate.setPreSelectedChoices(List.of(subclassKey));
			}
			PCClass[] wanted = new PCClass[levels];
			Arrays.fill(wanted, pcClass);
			character.addCharacterLevels(wanted);
		}
		catch (RuntimeException e)
		{
			int added = character.getClassLevel(pcClass) - before;
			if (added > 0)
			{
				return ServiceResult.success(new LevelsAdded("ok_with_warning", pcClass.getDisplayName(),
					added, levels, "Levels were added but an error occurred: " + e.getMessage()));
			}
			return ServiceResult.failure(new ServiceError.NotAllowed(String.valueOf(e.getMessage())));
		}

		finally
		{
			if (delegate != null)
			{
				delegate.clearPreSelectedChoices();
			}
		}

		int added = character.getClassLevel(pcClass) - before;
		if (added == 0)
		{
			return ServiceResult.failure(new ServiceError.NotAllowed(
				"Failed to add class level. Check character prerequisites and abilities."));
		}
		if (added < levels)
		{
			return ServiceResult.success(new LevelsAdded("partial", pcClass.getDisplayName(), added, levels,
				"Only " + added + " of " + levels + " levels were added."));
		}
		return ServiceResult.success(new LevelsAdded(STATUS_OK, pcClass.getDisplayName(), added, levels, null));
	}

	private static Map<String, PCStat> statsByEveryNameTheyAnswerTo(CharacterFacade character)
	{
		Map<String, PCStat> byName = new LinkedHashMap<>();
		for (PCStat stat : character.getDataSet().getStats())
		{
			byName.put(stat.getKeyName().toUpperCase(Locale.ROOT), stat);
			byName.put(stat.getDisplayName().toUpperCase(Locale.ROOT), stat);
		}
		return byName;
	}
}
