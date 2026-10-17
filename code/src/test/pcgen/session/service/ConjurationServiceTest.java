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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

import pcgen.AbstractCharacterTestCase;
import pcgen.core.PlayerCharacter;
import pcgen.core.conjurations.ConjurationSession;
import pcgen.core.conjurations.ConjuredCreature;
import pcgen.session.service.model.ConjurationSheet;
import pcgen.session.service.model.ConjurationSheetStored;
import pcgen.session.service.model.ConjuredCreatureEntry;

import org.junit.jupiter.api.Test;

/**
 * What happens to a catalog of conjurations a caller sends in, and what a
 * caller reads back of the fight.
 */
public class ConjurationServiceTest extends AbstractCharacterTestCase
{

	private static final String A_CATALOG = """
		## Summon Monster IV
		summon: Hound Archon
		summon: Lantern Archon

		## Summon Monster V
		summon: Bralani Azata
		""";

	private static String reasonRefused(ServiceResult<?> result)
	{
		if (result instanceof ServiceResult.Failure<?> failure
			&& failure.error() instanceof ServiceError.InvalidArgument invalid)
		{
			return invalid.reason();
		}
		throw new AssertionError("Expected an invalid argument, got " + result);
	}

	@Test
	public void countsTheGroupsAndCreaturesOfTheCatalogItStores()
	{
		ServiceResult<ConjurationSheetStored> stored = ConjurationService.write(getCharacter(), A_CATALOG);

		assertEquals(ServiceResult.success(new ConjurationSheetStored(2, 3)), stored);
	}

	@Test
	public void storesTheCatalogExactlyAsItWasSent()
	{
		PlayerCharacter character = getCharacter();

		ConjurationService.write(character, A_CATALOG);

		assertEquals(A_CATALOG, character.getDisplay().getConjurationCatalog().orElseThrow());
	}

	@Test
	public void namesEveryLineOfACatalogThatDoesNotRead()
	{
		String reason = reasonRefused(
			ConjurationService.write(getCharacter(), "## IV\nmonster: Hound Archon\nsummon: Lantern\n  size: small\n"));

		assertEquals(List.of("line 2", "line 4"),
			Arrays.stream(reason.split("\n")).filter(line -> line.startsWith("line ")).map(line -> line.split(":")[0])
				.toList());
	}

	@Test
	public void keepsTheCatalogItHeldWhenTheNewOneDoesNotRead()
	{
		PlayerCharacter character = getCharacter();
		character.setConjurationCatalog(A_CATALOG);

		ConjurationService.write(character, "## IV\nmonster: Hound Archon\n");

		assertEquals(A_CATALOG, character.getDisplay().getConjurationCatalog().orElseThrow());
	}

	@Test
	public void refusesABlankCatalog()
	{
		assertTrue(reasonRefused(ConjurationService.write(getCharacter(), " ")).contains("clear_conjuration_sheet"));
	}

	@Test
	public void describesTheCatalogAndTheCreaturesInPlay()
	{
		PlayerCharacter character = getCharacter();
		character.setConjurationCatalog(A_CATALOG);
		character.setConjurationSession(new ConjurationSession(12, List.of(
			new ConjuredCreature("Hound Archon", 2, 14, OptionalInt.of(7)),
			new ConjuredCreature("Lantern Archon", 1, 0, OptionalInt.empty()))));

		assertEquals(new ConjurationSheet(A_CATALOG, 12, List.of(
			new ConjuredCreatureEntry("Hound Archon", 2, 14, 7),
			new ConjuredCreatureEntry("Lantern Archon", 1, 0, null))), ConjurationService.describe(character));
	}

	@Test
	public void clearingForgetsTheCatalogAndTheFight()
	{
		PlayerCharacter character = getCharacter();
		character.setConjurationCatalog(A_CATALOG);
		character.setConjurationSession(new ConjurationSession(12,
			List.of(new ConjuredCreature("Hound Archon", 1, 0, OptionalInt.of(12)))));

		ConjurationService.clear(character);

		assertEquals(new ConjurationSheet("", 0, List.of()), ConjurationService.describe(character));
	}
}
