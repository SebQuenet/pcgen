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

import pcgen.core.Domain;
import pcgen.core.QualifiedObject;
import pcgen.session.PcgenSession;
import pcgen.session.service.model.DeitySet;
import pcgen.session.service.model.DomainAdded;
import pcgen.session.service.model.DomainRemoved;

/**
 * Who a character worships, and which of their deity's domains they draw on.
 */
public final class DeityDomainService
{
	private final CharacterLookup characters;

	public DeityDomainService(PcgenSession session)
	{
		this.characters = new CharacterLookup(session);
	}

	public ServiceResult<DeitySet> setDeity(String characterId, String deityKey)
	{
		return characters.byId(characterId).andThen(character ->
			DataSetLookup.require("Deity", deityKey, character.getDataSet().getDeities()).map(deity -> {
				character.setDeity(deity);
				return new DeitySet(deity.getDisplayName());
			}));
	}

	public ServiceResult<DomainAdded> addDomain(String characterId, String domainKey)
	{
		return characters.byId(characterId).andThen(character -> {
			QualifiedObject<Domain> offered = domainNamed(character.getAvailableDomains(), domainKey);
			if (offered == null)
			{
				return ServiceResult.failure(
					new ServiceError.EntryNotFound("Domain, or not available,", domainKey));
			}
			if (character.getRemainingDomainSelectionsRef().get() <= 0)
			{
				return ServiceResult.failure(new ServiceError.NotAllowed("No domain selections remaining"));
			}
			character.addDomain(offered);
			return ServiceResult.success(new DomainAdded(offered.getRawObject().getDisplayName(),
				character.getRemainingDomainSelectionsRef().get()));
		});
	}

	public ServiceResult<DomainRemoved> removeDomain(String characterId, String domainKey)
	{
		return characters.byId(characterId).andThen(character -> {
			QualifiedObject<Domain> held = domainNamed(character.getDomains(), domainKey);
			if (held == null)
			{
				return ServiceResult.failure(
					new ServiceError.NotAllowed("Character does not have domain: " + domainKey));
			}
			character.removeDomain(held);
			return ServiceResult.success(new DomainRemoved(held.getRawObject().getDisplayName()));
		});
	}

	private static QualifiedObject<Domain> domainNamed(Iterable<QualifiedObject<Domain>> candidates, String wanted)
	{
		for (QualifiedObject<Domain> candidate : candidates)
		{
			Domain domain = candidate.getRawObject();
			if (domain.getKeyName().equalsIgnoreCase(wanted) || domain.getDisplayName().equalsIgnoreCase(wanted))
			{
				return candidate;
			}
		}
		return null;
	}
}
