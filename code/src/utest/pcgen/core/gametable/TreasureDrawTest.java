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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import pcgen.core.gametable.TreasureTables.Lot;
import pcgen.core.gametable.TreasureTables.Part;
import pcgen.core.gametable.TreasureTables.PoolEntry;

import org.junit.jupiter.api.Test;

class TreasureDrawTest
{
	private static final PoolEntry CLOAK = new PoolEntry("Cloak of resistance +1", 1_000, 1, false, Optional.empty());
	private static final PoolEntry RING = new PoolEntry("Ring of protection +1", 2_000, 1, false, Optional.empty());

	private static final TreasureTables TABLES = new TreasureTables(
		Map.of("humanoid", List.of("A", "D"), "fey", List.of("C")),
		Map.of("A", List.of(new Lot(100, List.of(Part.pool(1, "cheap")))),
			"C", List.of(new Lot(1_000, List.of(Part.pool(1, "cloak")))),
			"D", List.of(new Lot(1_000, List.of(Part.pool(1, "cloak"))), new Lot(2_000, List.of(Part.pool(1, "ring"))))),
		Map.of("cloak", List.of(CLOAK), "ring", List.of(RING),
			"cheap", List.of(new PoolEntry("Potion of light", 25, 1, false, Optional.empty()))));

	private static List<LootLine> draw(TreasureTables tables, String creatureType, long amount)
	{
		return new TreasureDraw(tables, new Random(42)).draw(new LootDraw("Vrock 1", creatureType, TreasureType.STANDARD,
			amount), true);
	}

	private static LootLine drawn(String name, int quantity, long value, boolean resoldInFull)
	{
		return new LootLine(name, quantity, "Vrock 1", Optional.of(TreasureType.STANDARD), value, resoldInFull, true);
	}

	@Test
	void theLargestLotThatFitsIsTaken()
	{
		assertEquals(List.of(drawn("Ring of protection +1", 1, 2_000, false)), draw(TABLES, "humanoid", 2_000));
	}

	@Test
	void theRemainderBecomesGold()
	{
		assertEquals(List.of(drawn("Cloak of resistance +1", 1, 1_000, false), drawn("Gold pieces", 50, 50, true)),
			draw(TABLES, "humanoid", 1_050));
	}

	@Test
	void onlyTheCreaturesFamiliesAreDrawn()
	{
		assertEquals(List.of(drawn("Cloak of resistance +1", 1, 1_000, false)), draw(TABLES, "fey", 1_000));
	}

	@Test
	void identicalItemsShareALine()
	{
		assertEquals(List.of(drawn("Cloak of resistance +1", 2, 2_000, false)), draw(TABLES, "fey", 2_000));
	}

	@Test
	void aZeroAmountDrawsNothing()
	{
		assertEquals(List.of(), draw(TABLES, "humanoid", 0));
	}

	@Test
	void rolledCoinsAreCountedInTheirCoin()
	{
		TreasureTables silver = new TreasureTables(Map.of("ooze", List.of("A")),
			Map.of("A", List.of(new Lot(2, List.of(new Part("coins", 1, 2, 1, 10, "sp", ""))))), Map.of());

		assertEquals(List.of(drawn("Silver pieces", 20, 2, true)), draw(silver, "ooze", 2));
	}

	@Test
	void aGemAddsItsRolledValue()
	{
		PoolEntry moonstone = new PoolEntry("Moonstone", 25, 1, true, Optional.of(new TreasureTables.Dice(2, 1, 5)));
		TreasureTables gems = new TreasureTables(Map.of("ooze", List.of("B")),
			Map.of("B", List.of(new Lot(50, List.of(Part.pool(1, "gem"))))), Map.of("gem", List.of(moonstone)));

		assertEquals(List.of(drawn("Moonstone", 1, 35, true)), draw(gems, "ooze", 50));
	}

	@Test
	void anUnknownCreatureTypeDrawsLikeAHumanoid()
	{
		assertEquals(draw(TABLES, "humanoid", 2_000), draw(TABLES, "Swarm", 2_000));
	}

	@Test
	void theSameSeedDrawsTheSameLines()
	{
		assertEquals(draw(TreasureTables.bundled(), "outsider", 15_000), draw(TreasureTables.bundled(), "outsider", 15_000));
	}

	@Test
	void everyCreatureTypeHasFamilies()
	{
		assertEquals(13, TreasureTables.bundled().familiesByCreatureType().size());
	}

	@Test
	void everyPoolALotNamesHoldsItems()
	{
		TreasureTables bundled = TreasureTables.bundled();

		assertTrue(bundled.lotsByFamily().values().stream().flatMap(List::stream).flatMap(lot -> lot.reward().stream())
			.filter(part -> part.kind().equals("pool"))
			.allMatch(part -> !bundled.pools().getOrDefault(part.pool(), List.of()).isEmpty()));
	}
}
