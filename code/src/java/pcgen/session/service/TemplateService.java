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
import java.util.List;

import pcgen.core.PCTemplate;
import pcgen.facade.core.TempBonusFacade;
import pcgen.session.PcgenSession;
import pcgen.session.service.model.KitApplied;
import pcgen.session.service.model.NamedEntry;
import pcgen.session.service.model.TempBonusApplied;
import pcgen.session.service.model.TempBonusRemoved;
import pcgen.session.service.model.TempBonuses;
import pcgen.session.service.model.TemplateApplied;
import pcgen.session.service.model.TemplateRemoved;

/**
 * The bundles of rules laid over a character: templates, kits, and the
 * temporary bonuses a spell or effect grants for a while.
 */
public final class TemplateService
{
	private static final String TEMPLATE = "Template";

	private final CharacterLookup characters;

	public TemplateService(PcgenSession session)
	{
		this.characters = new CharacterLookup(session);
	}

	public ServiceResult<TemplateApplied> addTemplate(String characterId, String templateKey)
	{
		return characters.byId(characterId).andThen(character ->
			DataSetLookup.require(TEMPLATE, templateKey, character.getDataSet().getTemplates())
				.map(template -> {
					character.addTemplate(template);
					return new TemplateApplied(template.getDisplayName());
				}));
	}

	public ServiceResult<TemplateRemoved> removeTemplate(String characterId, String templateKey)
	{
		return characters.byId(characterId).andThen(character -> {
			PCTemplate held = null;
			for (PCTemplate candidate : character.getTemplates())
			{
				if (candidate.getKeyName().equalsIgnoreCase(templateKey)
					|| candidate.getDisplayName().equalsIgnoreCase(templateKey))
				{
					held = candidate;
					break;
				}
			}
			if (held == null)
			{
				return ServiceResult.failure(
					new ServiceError.NotAllowed("Character does not have template: " + templateKey));
			}
			character.removeTemplate(held);
			return ServiceResult.success(new TemplateRemoved(held.getDisplayName()));
		});
	}

	public ServiceResult<List<NamedEntry>> getTemplates(String characterId)
	{
		return characters.byId(characterId).map(character -> {
			List<NamedEntry> applied = new ArrayList<>();
			for (PCTemplate template : character.getTemplates())
			{
				applied.add(new NamedEntry(template.getKeyName(), template.getDisplayName()));
			}
			return List.copyOf(applied);
		});
	}

	public ServiceResult<TempBonusApplied> addTempBonus(String characterId, String bonusName)
	{
		return characters.byId(characterId).andThen(character -> {
			TempBonusFacade offered = bonusNamed(character.getAvailableTempBonuses(), bonusName);
			if (offered == null)
			{
				return ServiceResult.failure(new ServiceError.EntryNotFound("Temp bonus", bonusName));
			}
			character.addTempBonus(offered);
			return ServiceResult.success(new TempBonusApplied(offered.toString()));
		});
	}

	public ServiceResult<TempBonusRemoved> removeTempBonus(String characterId, String bonusName)
	{
		return characters.byId(characterId).andThen(character -> {
			TempBonusFacade inEffect = bonusNamed(character.getTempBonuses(), bonusName);
			if (inEffect == null)
			{
				return ServiceResult.failure(new ServiceError.EntryNotFound("Active temp bonus", bonusName));
			}
			character.removeTempBonus(inEffect);
			return ServiceResult.success(new TempBonusRemoved(inEffect.toString()));
		});
	}

	public ServiceResult<TempBonuses> listTempBonuses(String characterId)
	{
		return characters.byId(characterId).map(character -> new TempBonuses(
			namesOf(character.getAvailableTempBonuses()), namesOf(character.getTempBonuses())));
	}

	public ServiceResult<KitApplied> addKit(String characterId, String kitKey)
	{
		return characters.byId(characterId).andThen(character ->
			DataSetLookup.require("Kit", kitKey, character.getAvailableKits()).map(kit -> {
				character.addKit(kit);
				return new KitApplied(kit.getDisplayName());
			}));
	}

	private static TempBonusFacade bonusNamed(Iterable<TempBonusFacade> candidates, String wanted)
	{
		for (TempBonusFacade candidate : candidates)
		{
			if (candidate.toString().equalsIgnoreCase(wanted))
			{
				return candidate;
			}
		}
		return null;
	}

	private static List<String> namesOf(Iterable<TempBonusFacade> bonuses)
	{
		List<String> names = new ArrayList<>();
		for (TempBonusFacade bonus : bonuses)
		{
			names.add(bonus.toString());
		}
		return List.copyOf(names);
	}
}
