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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import pcgen.core.PlayerCharacter;
import pcgen.core.conjurations.BestiarySources;
import pcgen.session.HeadlessUIDelegate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * What monsters built on the Pathfinder Bestiary carry, and what is drawn for them.
 */
class CreatureLootTest
{
	private static final String CONTRACT_KILLER = "characters/combat-manager/NPC Codex/Rogue/Contract Killer.pcg";

	private static MonsterForge forge;
	private static final CreatureLoot LOOT = new CreatureLoot(TreasureTypes.bundled());

	@BeforeAll
	static void loadTheBestiary()
	{
		forge = new MonsterForge(BestiarySources.campaigns(), new HeadlessUIDelegate());
	}

	private static PlayerCharacter forged(MonsterSource source)
	{
		ForgedMonster monster = forge.forge(MonsterRecipe.of(source));
		assertInstanceOf(ForgedMonster.Forged.class, monster, monster.toString());
		return ((ForgedMonster.Forged) monster).character();
	}

	private static PlayerCharacter race(String key)
	{
		return forged(new MonsterSource.BestiaryRace(key));
	}

	@Test
	void aCodexNpcCarriesItsCloakAtItsPrice()
	{
		PlayerCharacter killer = forged(new MonsterSource.CharacterFile(CONTRACT_KILLER));

		List<LootLine> carried = LOOT.carried(killer, "Contract Killer", true);

		assertTrue(carried.contains(LootLine.carried("Cloak of Resistance +1", 1, "Contract Killer", 1_000, false, true)),
			() -> carried.toString());
	}

	@Test
	void aWolfCarriesNothing()
	{
		assertEquals(List.of(), LOOT.carried(race("Wolf"), "Wolf 1", true));
	}

	@Test
	void aWolfDrawsNothing()
	{
		assertEquals(0, LOOT.draw(race("Wolf"), "Wolf 1", "1", false, 0).amount());
	}

	@Test
	void aTreantDrawsTheStandardValueOfItsChallengeRating()
	{
		assertEquals(3_350, LOOT.draw(race("Treant"), "Treant", "8", false, 0).amount());
	}

	@Test
	void aTreantIsDrawnForAsAPlant()
	{
		assertEquals("plant", LOOT.draw(race("Treant"), "Treant", "8", false, 0).creatureType());
	}

	@Test
	void aCodexNpcDrawsNothing()
	{
		PlayerCharacter killer = forged(new MonsterSource.CharacterFile(CONTRACT_KILLER));

		assertEquals(new LootDraw("Contract Killer", "humanoid", TreasureType.NPC_GEAR, 0),
			LOOT.draw(killer, "Contract Killer", "10", true, 0));
	}

	@Test
	void carriedGoodsAreDeductedAndNeverBelowZero()
	{
		assertEquals(0, LOOT.draw(race("Treant"), "Treant", "8", false, 5_000).amount());
	}
}
