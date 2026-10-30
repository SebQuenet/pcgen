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

import java.util.List;

import pcgen.core.PlayerCharacter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * A book's NPC kit built as a monster: Dungeons of Golarion's Black Claw Elite, a kobold warrior 4.
 */
class NpcKitForgeTest
{
	private static MonsterForge forge;

	@BeforeAll
	static void loadDungeonsOfGolarion()
	{
		forge = TestSources.forge(List.of("Core Rulebook", "Bestiary", "Dungeons of Golarion"));
	}

	@Test
	void aBookNpcKitBuildsItsCharacter()
	{
		ForgedMonster built = forge.forge(MonsterRecipe.of(new MonsterSource.NpcKit("Black Claw Elite")));
		PlayerCharacter npc = assertInstanceOf(ForgedMonster.Forged.class, built, built.toString()).character();
		assertEquals(List.of("Kobold", 4), List.of(npc.getRace().getKeyName(), npc.getDisplay().getTotalLevels()));
	}

	@Test
	void anUnknownKitIsMissing()
	{
		assertInstanceOf(ForgedMonster.Missing.class, forge.forge(MonsterRecipe.of(new MonsterSource.NpcKit("No Such Kit"))));
	}
}
