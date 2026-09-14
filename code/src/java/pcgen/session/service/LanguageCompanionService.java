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
import java.util.Map;

import pcgen.core.Language;
import pcgen.core.Race;
import pcgen.facade.core.CharacterFacade;
import pcgen.facade.core.CompanionFacade;
import pcgen.facade.core.CompanionStubFacade;
import pcgen.facade.core.CompanionSupportFacade;
import pcgen.facade.core.LanguageChooserFacade;
import pcgen.facade.util.MapFacade;
import pcgen.session.HeadlessUIDelegate;
import pcgen.session.PcgenSession;
import pcgen.session.service.model.CompanionAdded;
import pcgen.session.service.model.CompanionRemoved;
import pcgen.session.service.model.CompanionSummary;
import pcgen.session.service.model.Companions;
import pcgen.session.service.model.KnownLanguage;
import pcgen.session.service.model.LanguageAdded;
import pcgen.session.service.model.LanguageChooserSummary;
import pcgen.session.service.model.LanguageRemoved;
import pcgen.session.service.model.Languages;
import pcgen.system.CharacterManager;
import pcgen.util.chooser.ChooserFactory;

/**
 * What a character speaks, and what follows them about.
 *
 * <p>
 * A bonus language is never added directly: it is taken from one of the offers a
 * race, class or feat makes, which is also what limits how many there can be. A
 * companion is a character in its own right, created and then tied to its master.
 */
public final class LanguageCompanionService
{
	private static final String UNKNOWN_RACE = "Unknown";

	private final PcgenSession session;
	private final CharacterLookup characters;

	public LanguageCompanionService(PcgenSession session)
	{
		this.session = session;
		this.characters = new CharacterLookup(session);
	}

	public ServiceResult<Languages> getLanguages(String characterId)
	{
		return characters.byId(characterId).map(character -> {
			List<KnownLanguage> spoken = new ArrayList<>();
			for (Language language : character.getLanguages())
			{
				spoken.add(new KnownLanguage(language.getKeyName(), language.getDisplayName(),
					character.isAutomatic(language), character.isRemovable(language)));
			}
			List<LanguageChooserSummary> offers = new ArrayList<>();
			for (LanguageChooserFacade chooser : character.getLanguageChoosers())
			{
				offers.add(new LanguageChooserSummary(chooser.getName(),
					chooser.getRemainingSelections().get(),
					displayNames(chooser.getAvailableList()),
					displayNames(chooser.getSelectedList())));
			}
			return new Languages(List.copyOf(spoken), List.copyOf(offers));
		});
	}

	public ServiceResult<LanguageAdded> addLanguage(String characterId, String languageKey)
	{
		return characters.byId(characterId).andThen(character -> {
			for (LanguageChooserFacade chooser : character.getLanguageChoosers())
			{
				if (chooser.getRemainingSelections().get() <= 0)
				{
					continue;
				}
				for (Language offered : chooser.getAvailableList())
				{
					if (offered.getKeyName().equalsIgnoreCase(languageKey)
						|| offered.getDisplayName().equalsIgnoreCase(languageKey))
					{
						chooser.addSelected(offered);
						chooser.commit();
						return ServiceResult.success(
							new LanguageAdded(offered.getDisplayName(), chooser.getName()));
					}
				}
			}
			return ServiceResult.failure(new ServiceError.NotAllowed(
				"Language not found or no available chooser with remaining selections: " + languageKey));
		});
	}

	public ServiceResult<LanguageRemoved> removeLanguage(String characterId, String languageKey)
	{
		return characters.byId(characterId).andThen(character -> {
			Language spoken = null;
			for (Language candidate : character.getLanguages())
			{
				if (candidate.getKeyName().equalsIgnoreCase(languageKey)
					|| candidate.getDisplayName().equalsIgnoreCase(languageKey))
				{
					spoken = candidate;
					break;
				}
			}
			if (spoken == null)
			{
				return ServiceResult.failure(new ServiceError.EntryNotFound("Language", languageKey));
			}
			if (!character.isRemovable(spoken))
			{
				return ServiceResult.failure(
					new ServiceError.NotAllowed("Language is not removable: " + spoken.getDisplayName()));
			}
			character.removeLanguage(spoken);
			return ServiceResult.success(new LanguageRemoved(spoken.getDisplayName()));
		});
	}

	public ServiceResult<Companions> getCompanions(String characterId)
	{
		return characters.byId(characterId).map(character -> {
			CompanionSupportFacade support = character.getCompanionSupport();
			List<CompanionSummary> following = new ArrayList<>();
			for (CompanionFacade companion : support.getCompanions())
			{
				following.add(new CompanionSummary(companion.getCompanionType(), raceNameOf(companion)));
			}
			List<CompanionSummary> offered = new ArrayList<>();
			for (CompanionStubFacade stub : support.getAvailableCompanions())
			{
				offered.add(new CompanionSummary(stub.getCompanionType(), raceNameOf(stub.getRaceRef().get())));
			}
			MapFacade<String, Integer> allowed = support.getMaxCompanionsMap();
			Map<String, Integer> maxPerType = new LinkedHashMap<>();
			for (String type : allowed.getKeys())
			{
				maxPerType.put(type, allowed.getValue(type));
			}
			return new Companions(List.copyOf(following), List.copyOf(offered), Map.copyOf(maxPerType));
		});
	}

	public ServiceResult<CompanionAdded> addCompanion(String characterId, String companionType, String companionRace)
	{
		return characters.byId(characterId).andThen(character -> {
			CompanionSupportFacade support = character.getCompanionSupport();
			CompanionStubFacade offered = stubFor(support, companionType, companionRace);
			if (offered == null)
			{
				return ServiceResult.failure(new ServiceError.EntryNotFound(
					"Available companion of type '" + companionType + "' with race", companionRace));
			}

			HeadlessUIDelegate delegate = new HeadlessUIDelegate();
			ChooserFactory.setDelegate(delegate);
			CharacterFacade companion =
				CharacterManager.createNewCharacter(delegate, session.getCurrentDataSet());
			if (companion == null)
			{
				return ServiceResult.failure(new ServiceError.DataFailure("Failed to create companion character"));
			}

			Race race = offered.getRaceRef().get();
			companion.setRace(race);
			support.addCompanion(companion, companionType);
			return ServiceResult.success(new CompanionAdded(companionType, raceNameOf(race)));
		});
	}

	public ServiceResult<CompanionRemoved> removeCompanion(String characterId, String companionType,
		String companionRace)
	{
		return characters.byId(characterId).andThen(character -> {
			CompanionSupportFacade support = character.getCompanionSupport();
			CompanionFacade following = companionOf(support, companionType, companionRace);
			if (following == null)
			{
				return ServiceResult.failure(new ServiceError.EntryNotFound(
					"Companion of type '" + companionType + "' with race",
					companionRace == null ? "any" : companionRace));
			}
			String raceName = raceNameOf(following);
			support.removeCompanion(following);
			return ServiceResult.success(new CompanionRemoved(companionType, raceName));
		});
	}

	private static CompanionStubFacade stubFor(CompanionSupportFacade support, String companionType,
		String companionRace)
	{
		for (CompanionStubFacade stub : support.getAvailableCompanions())
		{
			Race race = stub.getRaceRef().get();
			if (race != null && stub.getCompanionType().equals(companionType)
				&& (race.getDisplayName().equalsIgnoreCase(companionRace)
					|| race.getKeyName().equalsIgnoreCase(companionRace)))
			{
				return stub;
			}
		}
		return null;
	}

	/** The companion of that type, or the first of them when no race is named. */
	private static CompanionFacade companionOf(CompanionSupportFacade support, String companionType,
		String companionRace)
	{
		boolean anyRaceWillDo = companionRace == null || companionRace.isBlank();
		for (CompanionFacade companion : support.getCompanions())
		{
			if (companion.getCompanionType().equalsIgnoreCase(companionType)
				&& (anyRaceWillDo || raceNameOf(companion).equalsIgnoreCase(companionRace)))
			{
				return companion;
			}
		}
		return null;
	}

	private static List<String> displayNames(Iterable<Language> languages)
	{
		List<String> names = new ArrayList<>();
		for (Language language : languages)
		{
			names.add(language.getDisplayName());
		}
		return List.copyOf(names);
	}

	private static String raceNameOf(CompanionFacade companion)
	{
		return raceNameOf(companion.getRaceRef().get());
	}

	private static String raceNameOf(Race race)
	{
		return race == null ? UNKNOWN_RACE : race.getDisplayName();
	}
}
