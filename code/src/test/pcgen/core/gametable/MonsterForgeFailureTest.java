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
package pcgen.core.gametable;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * A race whose data make PCGen throw while building it: the forge reports it rather than letting it through.
 */
class MonsterForgeFailureTest
{
	private static MonsterForge forge;

	@BeforeAll
	static void loadAnAdventurePathWithoutTheTechnologyGuide()
	{
		forge = TestSources.forge(List.of("Core Rulebook", "Bestiary", "Pathfinder Adventure Path #88 ~ Creatures"));
	}

	@Test
	void aRaceWhoseDataNeedAnUnloadedBookIsMissingWithTheReason()
	{
		// Its kit gives it Technologist, a feat of the Technology Guide
		ForgedMonster built = forge.forge(MonsterRecipe.of(new MonsterSource.BestiaryRace("Reclamation Robot")));
		ForgedMonster.Missing missing = assertInstanceOf(ForgedMonster.Missing.class, built);
		assertTrue(missing.reason().contains("Reclamation Robot"), missing.reason());
	}
}
