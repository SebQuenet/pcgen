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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import pcgen.core.AbilityCategory;
import pcgen.facade.core.AbilityFacade;
import pcgen.facade.core.CharacterFacade;
import pcgen.facade.util.ListFacade;
import pcgen.session.HeadlessUIDelegate;
import pcgen.session.PcgenSession;
import pcgen.session.service.model.AbilityAdded;
import pcgen.session.service.model.AbilityCategorySummary;
import pcgen.session.service.model.AbilityRemoved;
import pcgen.session.service.model.AvailableAbility;
import pcgen.session.service.model.BatchOutcome;

/**
 * The feats, traits and class abilities a character takes.
 *
 * <p>
 * Many abilities ask a question when taken — which weapon, which school. A
 * caller that has an answer passes it in; a caller that has none gets the list
 * of options back and nothing is added.
 */
public final class AbilityService
{
	private static final String STATUS_OK = "ok";
	private static final String STATUS_CHOICE_REQUIRED = "choice_required";
	private static final String CATEGORY = "Ability category";

	private final PcgenSession session;
	private final CharacterLookup characters;

	public AbilityService(PcgenSession session)
	{
		this.session = session;
		this.characters = new CharacterLookup(session);
	}

	public ServiceResult<List<AbilityCategorySummary>> listAbilityCategories(String characterId)
	{
		return characters.byId(characterId).map(character -> {
			List<AbilityCategorySummary> summaries = new ArrayList<>();
			Set<String> seen = new LinkedHashSet<>();
			for (AbilityCategory category : character.getActiveAbilityCategories())
			{
				summaries.add(summaryOf(character, category));
				seen.add(category.getKeyName());
			}
			// Headless, a category with a pool can be absent from the active list —
			// mythic paths are the usual case — so anything with selections is added.
			for (AbilityCategory category : character.getDataSet().getAbilities().getKeys())
			{
				if (!seen.contains(category.getKeyName()) && character.getTotalSelections(category) > 0)
				{
					summaries.add(summaryOf(character, category));
					seen.add(category.getKeyName());
				}
			}
			return List.copyOf(summaries);
		});
	}

	public ServiceResult<List<AvailableAbility>> listAbilities(String characterId, String categoryKey)
	{
		return characters.byId(characterId).andThen(character ->
			findCategory(character, categoryKey).map(category -> {
				List<AvailableAbility> available = new ArrayList<>();
				for (AbilityFacade ability : abilitiesIn(character, category))
				{
					available.add(new AvailableAbility(
						ability.getKeyName(), ability.toString(), character.isQualifiedFor(ability)));
				}
				return List.copyOf(available);
			}));
	}

	public ServiceResult<AbilityAdded> addAbility(String characterId, String categoryKey, String abilityKey,
		List<String> choice)
	{
		return characters.byId(characterId).andThen(character ->
			findCategory(character, categoryKey).andThen(category ->
				findAbility(character, category, abilityKey).andThen(ability ->
					add(characterId, character, category, ability, choice))));
	}

	public ServiceResult<AbilityRemoved> removeAbility(String characterId, String categoryKey, String abilityKey)
	{
		return characters.byId(characterId).andThen(character ->
			findCategory(character, categoryKey).andThen(category -> {
				AbilityFacade held = matching(character.getAbilities(category), abilityKey);
				if (held == null)
				{
					return ServiceResult.failure(
						new ServiceError.NotAllowed("Character does not have ability: " + abilityKey));
				}
				character.removeAbility(category, held);
				return ServiceResult.success(new AbilityRemoved(held.toString()));
			}));
	}

	/**
	 * Add several abilities in one call. One that cannot be added does not stop the
	 * others: what failed comes back in the result.
	 */
	public ServiceResult<BatchOutcome> batchAddAbilities(String characterId, List<AbilityRequest> requests)
	{
		return characters.byId(characterId).map(character -> {
			HeadlessUIDelegate delegate = session.getDelegate(characterId);
			List<String> errors = new ArrayList<>();
			int accepted = 0;

			for (AbilityRequest request : requests)
			{
				AbilityCategory inCategory = switch (findCategory(character, request.categoryKey()))
				{
					case ServiceResult.Success<AbilityCategory> found -> found.value();
					case ServiceResult.Failure<AbilityCategory> missing -> {
						errors.add(missing.error().message());
						yield null;
					}
				};
				if (inCategory == null)
				{
					continue;
				}
				AbilityFacade ability = matching(abilitiesIn(character, inCategory), request.abilityKey());
				if (ability == null)
				{
					errors.add("Ability not found: " + request.abilityKey() + " in " + request.categoryKey());
					continue;
				}
				try
				{
					preSelect(delegate, request.choice());
					character.addAbility(inCategory, ability);
					accepted++;
				}
				catch (RuntimeException e)
				{
					errors.add("Failed: " + request.abilityKey() + " - " + e.getMessage());
				}
				finally
				{
					clearPreSelection(delegate);
				}
			}
			return new BatchOutcome(accepted, requests.size(), List.copyOf(errors));
		});
	}

	/** One line of a batch: which ability, in which category, with which answer. */
	public record AbilityRequest(String categoryKey, String abilityKey, List<String> choice)
	{
	}

	private ServiceResult<AbilityAdded> add(String characterId, CharacterFacade character,
		AbilityCategory category, AbilityFacade ability, List<String> choice)
	{
		HeadlessUIDelegate delegate = session.getDelegate(characterId);
		if (ability.isMult() && (choice == null || choice.isEmpty()))
		{
			return offerTheChoices(character, category, ability, delegate);
		}

		try
		{
			preSelect(delegate, choice);
			if (delegate != null)
			{
				delegate.consumeLastError();
			}
			character.addAbility(category, ability);
			String refusal = delegate == null ? null : delegate.consumeLastError();
			if (refusal != null)
			{
				return ServiceResult.failure(new ServiceError.NotAllowed("Failed to add ability: " + refusal));
			}
			return ServiceResult.success(new AbilityAdded(STATUS_OK, ability.toString(),
				choice == null ? List.of() : List.copyOf(choice), List.of(), null));
		}
		finally
		{
			clearPreSelection(delegate);
		}
	}

	/**
	 * Ask the ability what it would offer, without taking it. The delegate records
	 * the options the chooser would have shown and rolls the addition back.
	 */
	private static ServiceResult<AbilityAdded> offerTheChoices(CharacterFacade character, AbilityCategory category,
		AbilityFacade ability, HeadlessUIDelegate delegate)
	{
		if (delegate != null)
		{
			delegate.enableCaptureMode();
		}
		character.addAbility(category, ability);
		if (delegate == null)
		{
			return ServiceResult.success(new AbilityAdded(STATUS_OK, ability.toString(), List.of(), List.of(), null));
		}
		delegate.disableCaptureMode();
		List<String> options = delegate.getCapturedOptions();
		if (options == null || options.isEmpty())
		{
			return ServiceResult.success(new AbilityAdded(STATUS_OK, ability.toString(), List.of(), List.of(), null));
		}
		return ServiceResult.success(new AbilityAdded(STATUS_CHOICE_REQUIRED, ability.toString(), List.of(),
			List.copyOf(options),
			"This ability requires a choice. Call add_ability again with the 'choice' parameter."));
	}

	/**
	 * The category a caller named. Active categories come first; the rest of the
	 * data set is searched too, because headless a category with a pool can be
	 * missing from the active list.
	 */
	private static ServiceResult<AbilityCategory> findCategory(CharacterFacade character, String categoryKey)
	{
		ServiceResult<AbilityCategory> active =
			DataSetLookup.require(CATEGORY, categoryKey, character.getActiveAbilityCategories());
		return switch (active)
		{
			case ServiceResult.Success<AbilityCategory> found -> found;
			case ServiceResult.Failure<AbilityCategory> ignored ->
				DataSetLookup.require(CATEGORY, categoryKey, character.getDataSet().getAbilities().getKeys());
		};
	}

	private static ServiceResult<AbilityFacade> findAbility(CharacterFacade character, AbilityCategory category,
		String abilityKey)
	{
		AbilityFacade ability = matching(abilitiesIn(character, category), abilityKey);
		return ability == null
			? ServiceResult.failure(new ServiceError.EntryNotFound("Ability", abilityKey))
			: ServiceResult.success(ability);
	}

	private static Iterable<AbilityFacade> abilitiesIn(CharacterFacade character, AbilityCategory category)
	{
		ListFacade<AbilityFacade> abilities = character.getDataSet().getAbilities().getValue(category);
		return abilities == null ? List.of() : abilities;
	}

	private static AbilityFacade matching(Iterable<AbilityFacade> candidates, String wanted)
	{
		for (AbilityFacade candidate : candidates)
		{
			if (candidate.getKeyName().equalsIgnoreCase(wanted) || candidate.toString().equalsIgnoreCase(wanted))
			{
				return candidate;
			}
		}
		return null;
	}

	private static AbilityCategorySummary summaryOf(CharacterFacade character, AbilityCategory category)
	{
		return new AbilityCategorySummary(category.getKeyName(), category.getDisplayName(),
			character.getRemainingSelections(category), character.getTotalSelections(category));
	}

	private static void preSelect(HeadlessUIDelegate delegate, List<String> choice)
	{
		if (delegate != null && choice != null && !choice.isEmpty())
		{
			delegate.setPreSelectedChoices(choice);
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
