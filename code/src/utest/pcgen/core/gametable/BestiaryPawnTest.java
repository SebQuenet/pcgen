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

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * The pawns drawn by {@code scripts/generate_bestiary_pawns.py}, found from the monster they show.
 */
class BestiaryPawnTest
{
	@Test
	void aBestiaryRaceIsShownByThePawnNamedAfterItsKey()
	{
		Optional<BestiaryPawn> pawn = BestiaryPawn.of(new MonsterSource.BestiaryRace("Giant (Stone)"));

		assertEquals(Optional.of("giant_stone.jpg"), pawn.map(BestiaryPawn::fileName));
	}

	@Test
	void aMonsterBuiltFromACharacterIsShownByThePawnNamedAfterItsFile()
	{
		Optional<BestiaryPawn> pawn = BestiaryPawn.of(new MonsterSource.CharacterFile("/npcs/Bestiary/Aboleth.pcg"));

		assertEquals(Optional.of("aboleth.jpg"), pawn.map(BestiaryPawn::fileName));
	}

	@Test
	void aRaceWithoutPawnHasNone()
	{
		assertEquals(Optional.empty(), BestiaryPawn.of(new MonsterSource.BestiaryRace("Unknown Race")));
	}

	@Test
	void aPawnIsAJpeg()
	{
		Optional<BestiaryPawn> pawn = BestiaryPawn.of(new MonsterSource.BestiaryRace("Aboleth"));

		assertEquals(Optional.of(ImageFormat.JPEG), pawn.flatMap(found -> ImageFormat.detect(found.bytes())));
	}

	@Test
	void aPawnStandsOnTheTableAsAFigurine()
	{
		Optional<BestiaryPawn> pawn = BestiaryPawn.of(new MonsterSource.BestiaryRace("Aboleth"));

		assertEquals(Optional.of(TabletopPiece.FIGURINE), pawn.map(found -> found.forTabletop().piece()));
	}
}
