/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package pcgen.gui2.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import pcgen.core.PlayerCharacter;
import pcgen.core.conjurations.BestiarySources;
import pcgen.core.conjurations.TemplateDifference;
import pcgen.facade.core.ConjurationSheetFacade;

import org.junit.jupiter.api.Test;

/**
 * A creature in play shows what its templates change, against the same creature without them.
 */
class ConjurationVariantFacadeTest
{
	private static final String AGILE_ARCHON = "Archon (Hound) (Mythic Simple Template ~ Agile)";

	private static ConjurationSheetFacade sheetOf(String catalog)
	{
		PlayerCharacter character = BestiarySources.newCharacter("Conjurer");
		character.setConjurationCatalog(catalog);
		return new ConjurationSheetFacadeImpl(character);
	}

	@Test
	void anAgileHoundArchonShowsItsInitiativeAgainstTheBareArchon()
	{
		ConjurationSheetFacade sheet = sheetOf("## Summon Monster IV\nsummon: Archon (Hound)\n"
			+ "  template: Mythic Simple Template ~ Agile\n");
		int number = sheet.conjure(AGILE_ARCHON);

		List<TemplateDifference> differences = sheet.viewOf(AGILE_ARCHON, number).orElseThrow().templateDifferences();

		assertTrue(differences.contains(new TemplateDifference("init", "+4", "+24")));
	}

	@Test
	void aCreatureWithoutTemplatesShowsNoDifference()
	{
		ConjurationSheetFacade sheet = sheetOf("## Summon Monster IV\nsummon: Archon (Hound)\n");
		int number = sheet.conjure("Archon (Hound)");

		assertEquals(List.of(), sheet.viewOf("Archon (Hound)", number).orElseThrow().templateDifferences());
	}
}
