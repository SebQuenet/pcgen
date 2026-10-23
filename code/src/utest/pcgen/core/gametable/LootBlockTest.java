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

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class LootBlockTest
{
	private static final LootLine BREASTPLATE = LootLine.carried("+2 Breastplate", 1, "Vrock ritualist", 4_350, false, true);
	private static final LootLine CLOAK = LootLine.carried("Cloak of Resistance +3", 1, "Vrock ritualist", 9_000, false, true);
	private static final LootLine COINS = new LootLine("Coins", 1, "Vrock 2", Optional.of(TreasureType.STANDARD), 3_210,
		true, true);

	private static LootBlock blockOf(LootLine... lines)
	{
		return new LootBlock(1, "The ritualist's camp", "14", 38_400, List.of(lines), List.of());
	}

	@Test
	void aMagicItemResellsForHalf()
	{
		assertEquals(4_500, CLOAK.resale());
	}

	@Test
	void coinsResellInFull()
	{
		assertEquals(3_210, COINS.resale());
	}

	@Test
	void anUncheckedLineLeavesTheTotals()
	{
		LootBlock block = blockOf(BREASTPLATE, CLOAK).selecting(1, false);

		assertEquals(4_350, block.selectedValue());
	}

	@Test
	void theShareIsTheResaleSplitAmongThePlayers()
	{
		LootBlock block = blockOf(BREASTPLATE, CLOAK, COINS);

		assertEquals(9_885 / 4, block.sharePerPlayer(4));
	}

	@Test
	void theBudgetIsTheTreasureValueOfTheFight()
	{
		assertEquals(15_000, blockOf().budget());
	}

	@Test
	void eachPlayerEarnsTheirShareOfTheExperience()
	{
		assertEquals(9_600, blockOf().experiencePerPlayer(4));
	}

	@Test
	void aRerollKeepsTheCarriedLinesAndTheirBoxes()
	{
		LootLine gem = new LootLine("Moonstone", 3, "Vrock 1", Optional.of(TreasureType.STANDARD), 150, true, true);
		LootBlock block = blockOf(BREASTPLATE, COINS).selecting(0, false);

		LootBlock rerolled = block.withDrawnLines(List.of(gem));

		assertEquals(List.of(BREASTPLATE.selecting(false), gem), rerolled.lines());
	}
}
