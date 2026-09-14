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
import java.util.function.Predicate;

import pcgen.cdom.base.Identified;
import pcgen.core.PCStat;
import pcgen.facade.core.DataSetFacade;
import pcgen.facade.core.TodoFacade;
import pcgen.session.PcgenSession;
import pcgen.session.service.model.CharacterDetails;
import pcgen.session.service.model.DirtyState;
import pcgen.session.service.model.QualificationCheck;
import pcgen.session.service.model.StatDetail;
import pcgen.session.service.model.StatsRolled;
import pcgen.session.service.model.TodoList;

/**
 * Questions asked about a character rather than changes made to them.
 */
public final class UtilityService
{
	private final CharacterLookup characters;

	public UtilityService(PcgenSession session)
	{
		this.characters = new CharacterLookup(session);
	}

	public ServiceResult<TodoList> getTodoList(String characterId)
	{
		return characters.byId(characterId).map(character -> {
			List<String> todo = new ArrayList<>();
			for (TodoFacade item : character.getTodoList())
			{
				todo.add(item.getMessageKey());
			}
			return new TodoList(List.copyOf(todo), todo.size());
		});
	}

	/**
	 * Whether a character meets what something asks of them.
	 *
	 * <p>
	 * PCGen answers this with one method per kind of thing rather than one method
	 * over a common type, so the kind a caller names decides which is called.
	 */
	public ServiceResult<QualificationCheck> isQualifiedFor(String characterId, String itemType, String itemKey)
	{
		return characters.byId(characterId).andThen(character -> {
			DataSetFacade dataSet = character.getDataSet();
			return switch (itemType.toLowerCase(Locale.ROOT))
			{
				case "race" -> check(itemType, itemKey, dataSet.getRaces(), character::isQualifiedFor);
				case "class" -> check(itemType, itemKey, dataSet.getClasses(), character::isQualifiedFor);
				case "deity" -> check(itemType, itemKey, dataSet.getDeities(), character::isQualifiedFor);
				case "template" -> check(itemType, itemKey, dataSet.getTemplates(), character::isQualifiedFor);
				case "kit" -> check(itemType, itemKey, dataSet.getKits(), character::isQualifiedFor);
				default -> ServiceResult.failure(new ServiceError.InvalidArgument("item_type",
					"unknown type '" + itemType + "'. Use: race, class, deity, template, kit"));
			};
		});
	}

	private static <T extends Identified> ServiceResult<QualificationCheck> check(String itemType, String itemKey,
		Iterable<T> candidates, Predicate<T> qualifies)
	{
		return DataSetLookup.require(itemType, itemKey, candidates).map(item ->
			new QualificationCheck(qualifies.test(item), item.getDisplayName(), itemType));
	}

	public ServiceResult<StatsRolled> rollStats(String characterId)
	{
		return characters.byId(characterId).andThen(character -> {
			if (!character.isStatRollEnabled())
			{
				return ServiceResult.failure(
					new ServiceError.NotAllowed("Stat rolling is not enabled for this game mode"));
			}
			character.rollStats();
			Map<String, Integer> scores = new LinkedHashMap<>();
			for (PCStat stat : character.getDataSet().getStats())
			{
				scores.put(stat.getKeyName(), character.getScoreBase(stat));
			}
			return ServiceResult.success(new StatsRolled(Map.copyOf(scores)));
		});
	}

	public ServiceResult<DirtyState> isDirty(String characterId)
	{
		return characters.byId(characterId).map(character -> new DirtyState(character.isDirty()));
	}

	public ServiceResult<CharacterDetails> getCharacterDetails(String characterId)
	{
		return characters.byId(characterId).map(character -> {
			Map<String, StatDetail> stats = new LinkedHashMap<>();
			for (PCStat stat : character.getDataSet().getStats())
			{
				stats.put(stat.getKeyName(), new StatDetail(
					character.getScoreBase(stat),
					character.getScoreTotalString(stat),
					character.getModTotal(stat),
					character.getScoreRaceBonus(stat),
					character.getScoreOtherBonus(stat)));
			}
			Integer hp = character.getTotalHPRef().get();
			return new CharacterDetails(
				hp == null ? 0 : hp,
				character.getCarriedWeightRef().get(),
				character.getLoadRef().get(),
				character.getWeightLimitRef().get(),
				Money.amountOf(character.getFundsRef()),
				Money.amountOf(character.getWealthRef()),
				Map.copyOf(stats));
		});
	}
}
