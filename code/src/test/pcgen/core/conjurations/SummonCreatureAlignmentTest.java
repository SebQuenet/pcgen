/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package pcgen.core.conjurations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * A summoned creature's alignment comes from its Bestiary kit, changed by its
 * summoning template, so that the catalog can be filtered by alignment.
 */
class SummonCreatureAlignmentTest
{
	private static Optional<String> alignmentOf(String catalog)
	{
		BestiarySources.session();
		return SummonCatalogParser.catalogOf(catalog).flatMap(parsed -> parsed.groups().stream()
			.flatMap(group -> group.summons().stream()).findFirst()).flatMap(PathfinderSummonOptions::alignmentOf);
	}

	@Test
	void readsTheAlignmentOfTheCreaturesKit()
	{
		assertEquals(Optional.of("LG"), alignmentOf("## Convocation de monstres IV\nsummon: Archon (Hound)\n"));
	}

	@Test
	void appliesTheCelestialTemplateToANeutralAnimal()
	{
		assertEquals(Optional.of("NG"), alignmentOf("## Convocation de monstres IV\nsummon: Lion\n"
			+ "  template: Celestial Creature\n"));
	}
}
