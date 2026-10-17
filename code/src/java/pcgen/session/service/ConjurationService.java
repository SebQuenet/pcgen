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

import pcgen.core.PlayerCharacter;
import pcgen.core.conjurations.ConjurationSession;
import pcgen.core.conjurations.ConjuredCreature;
import pcgen.core.conjurations.SummonCatalog;
import pcgen.core.conjurations.SummonCatalogParser;
import pcgen.core.conjurations.SummonParseError;
import pcgen.core.conjurations.SummonParseFailure;
import pcgen.core.conjurations.SummonParseSuccess;
import pcgen.session.PcgenSession;
import pcgen.session.service.model.ConjurationSheet;
import pcgen.session.service.model.ConjurationSheetStored;
import pcgen.session.service.model.ConjuredCreatureEntry;

/**
 * The catalog of conjurations: the creatures a character can conjure, and the
 * ones it has conjured into the fight.
 *
 * <p>
 * The catalog is source text in the syntax the Conjurations tab shows, so what
 * is written here is what a player reads and corrects there. Text that does
 * not read is refused with the offending lines named, rather than half stored.
 * The fight is the player's to play: a caller reads it, and never writes it.
 */
public final class ConjurationService
{
	private final CharacterLookup characters;

	public ConjurationService(PcgenSession session)
	{
		this.characters = new CharacterLookup(session);
	}

	public ServiceResult<ConjurationSheetStored> setConjurationSheet(String characterId, String source)
	{
		return characters.playerCharacterById(characterId).andThen(character -> write(character, source));
	}

	public ServiceResult<ConjurationSheet> getConjurationSheet(String characterId)
	{
		return characters.playerCharacterById(characterId).map(ConjurationService::describe);
	}

	public ServiceResult<ConjurationSheetStored> clearConjurationSheet(String characterId)
	{
		return characters.playerCharacterById(characterId).map(ConjurationService::clear);
	}

	/**
	 * @param character the character to write the catalog on
	 * @param source    the catalog's source text
	 * @return how much of it was read; a refusal naming every offending line
	 *         when it does not read, in which case the character keeps the
	 *         catalog it held
	 */
	static ServiceResult<ConjurationSheetStored> write(PlayerCharacter character, String source)
	{
		if (source == null || source.isBlank())
		{
			return ServiceResult.failure(new ServiceError.InvalidArgument("source",
				"a catalog of conjurations needs text; use clear_conjuration_sheet to remove one"));
		}
		return switch (SummonCatalogParser.parse(source))
		{
			case SummonParseSuccess read -> {
				character.setConjurationCatalog(source);
				yield ServiceResult.success(storedOf(read.catalog()));
			}
			case SummonParseFailure refused -> ServiceResult.failure(
				new ServiceError.InvalidArgument("source", "the catalog does not read:\n" + listed(refused)));
		};
	}

	/**
	 * @param character the character whose conjurations to read
	 * @return the catalog as written and the fight as it stands
	 */
	static ConjurationSheet describe(PlayerCharacter character)
	{
		ConjurationSession fight = character.getDisplay().getConjurationSession();
		return new ConjurationSheet(character.getDisplay().getConjurationCatalog().orElse(""), fight.duration(),
			fight.conjured().stream().map(ConjurationService::entryOf).toList());
	}

	/**
	 * @param character the character to remove the catalog and the fight from
	 * @return what is left, which is nothing
	 */
	static ConjurationSheetStored clear(PlayerCharacter character)
	{
		character.clearConjurations();
		return new ConjurationSheetStored(0, 0);
	}

	private static ConjurationSheetStored storedOf(SummonCatalog catalog)
	{
		return new ConjurationSheetStored(catalog.groups().size(),
			catalog.groups().stream().mapToInt(group -> group.summons().size()).sum());
	}

	private static ConjuredCreatureEntry entryOf(ConjuredCreature creature)
	{
		Integer roundsLeft = creature.roundsLeft().isPresent() ? creature.roundsLeft().getAsInt() : null;
		return new ConjuredCreatureEntry(creature.creature(), creature.number(), creature.damage(), roundsLeft);
	}

	private static String listed(SummonParseFailure refused)
	{
		StringBuilder lines = new StringBuilder();
		for (SummonParseError error : refused.errors())
		{
			lines.append("line ").append(error.line()).append(": ").append(error.message()).append('\n');
		}
		return lines.toString();
	}
}
