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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.random.RandomGenerator;

import pcgen.core.gametable.TreasureTables.Lot;
import pcgen.core.gametable.TreasureTables.Part;
import pcgen.core.gametable.TreasureTables.PoolEntry;

/**
 * Draws a monster's treasure as Ultimate Equipment does: while the amount allows, the
 * largest reward of the creature's treasure types that fits, its rolls made; what is left
 * becomes gold pieces.
 */
public final class TreasureDraw
{
	private static final String DEFAULT_CREATURE_TYPE = "humanoid";
	private static final String COINS = "coins";
	/** The coins, largest first, with their name and how many make one gold piece, or what one is worth. */
	private static final List<Coin> COIN_KINDS = List.of(new Coin("pp", "Platinum pieces", 10, 1),
		new Coin("gp", "Gold pieces", 1, 1), new Coin("sp", "Silver pieces", 1, 10), new Coin("cp", "Copper pieces", 1, 100));

	private final TreasureTables tables;
	private final RandomGenerator dice;

	private record Coin(String code, String name, long worth, long perGoldPiece)
	{
	}

	private record Found(String name, long unitValue, boolean resoldInFull)
	{
	}

	/**
	 * @param tables the treasure generation tables.
	 * @param dice   what the rolls are made with.
	 */
	public TreasureDraw(TreasureTables tables, RandomGenerator dice)
	{
		this.tables = tables;
		this.dice = dice;
	}

	/**
	 * @param request  the monster and the gold pieces to draw for it.
	 * @param selected whether its lines start in the loot.
	 * @return the lines drawn, items first, then coins.
	 */
	public List<LootLine> draw(LootDraw request, boolean selected)
	{
		List<Lot> lots = lotsOf(request.creatureType());
		Map<Found, Integer> items = new LinkedHashMap<>();
		Map<String, Long> coins = new LinkedHashMap<>();
		long remaining = request.amount();
		while (true)
		{
			long left = remaining;
			long largest = lots.stream().mapToLong(Lot::value).filter(value -> value <= left).max().orElse(0);
			if (largest <= 0)
			{
				break;
			}
			List<Lot> fitting = lots.stream().filter(lot -> lot.value() == largest).toList();
			open(fitting.get(dice.nextInt(fitting.size())), items, coins);
			remaining -= largest;
		}
		coins.merge("gp", remaining, Long::sum);

		List<LootLine> lines = new ArrayList<>();
		Optional<TreasureType> drawnAs = Optional.of(request.type());
		items.forEach((found, quantity) -> lines.add(new LootLine(found.name(), quantity, request.carrier(), drawnAs,
			found.unitValue() * quantity, found.resoldInFull(), selected)));
		for (Coin coin : COIN_KINDS)
		{
			long count = coins.getOrDefault(coin.code(), 0L);
			if (count > 0)
			{
				lines.add(new LootLine(coin.name(), Math.toIntExact(count), request.carrier(), drawnAs,
					count * coin.worth() / coin.perGoldPiece(), true, selected));
			}
		}
		return lines;
	}

	private List<Lot> lotsOf(String creatureType)
	{
		List<String> families = tables.familiesByCreatureType().getOrDefault(creatureType.strip().toLowerCase(Locale.ROOT),
			tables.familiesByCreatureType().getOrDefault(DEFAULT_CREATURE_TYPE, List.of()));
		return families.stream().flatMap(family -> tables.lotsByFamily().getOrDefault(family, List.of()).stream()).toList();
	}

	private void open(Lot lot, Map<Found, Integer> items, Map<String, Long> coins)
	{
		for (Part part : lot.reward())
		{
			if (part.kind().equals(COINS))
			{
				coins.merge(part.coin(), roll(part.dice(), part.sides()) * part.multiplier(), Long::sum);
				continue;
			}
			for (int index = 0; index < part.count(); index++)
			{
				pick(tables.pools().getOrDefault(part.pool(), List.of())).ifPresent(entry -> {
					long value = entry.price() + entry.added().map(added -> roll(added.count(), added.sides())
						* added.multiplier()).orElse(0L);
					items.merge(new Found(entry.name(), value, entry.resoldInFull()), 1, Integer::sum);
				});
			}
		}
	}

	private Optional<PoolEntry> pick(List<PoolEntry> pool)
	{
		long total = pool.stream().mapToLong(PoolEntry::weight).sum();
		if (total <= 0)
		{
			return Optional.empty();
		}
		long roll = dice.nextLong(total);
		for (PoolEntry entry : pool)
		{
			roll -= entry.weight();
			if (roll < 0)
			{
				return Optional.of(entry);
			}
		}
		return Optional.empty();
	}

	private long roll(int count, int sides)
	{
		long total = 0;
		for (int die = 0; die < count; die++)
		{
			total += dice.nextInt(sides) + 1;
		}
		return total;
	}
}
