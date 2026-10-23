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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pcgen.cdom.util.CControl;
import pcgen.core.Equipment;
import pcgen.core.PlayerCharacter;
import pcgen.core.Race;
import pcgen.output.channel.ChannelUtilities;

/**
 * What a monster PCGen built leaves behind: what it carries, at the price PCGen computes,
 * and what is to be drawn for it.
 */
public final class CreatureLoot
{
	private static final List<String> RESOLD_IN_FULL = List.of("Gem", "Art", "Coin", "TradeGood", "Trade Good");
	private static final String COINS = "Gold pieces";
	private static final String UNKNOWN_TYPE = "humanoid";

	private final TreasureTypes types;

	/**
	 * @param types the bestiary treasure type of each creature.
	 */
	public CreatureLoot(TreasureTypes types)
	{
		this.types = types;
	}

	/**
	 * @param monster  a monster PCGen built.
	 * @param carrier  its name in the fight.
	 * @param selected whether its lines start in the loot.
	 * @return what it carries, natural weapons and worthless things left out, then its gold.
	 */
	public List<LootLine> carried(PlayerCharacter monster, String carrier, boolean selected)
	{
		List<LootLine> lines = new ArrayList<>();
		for (Equipment item : monster.getEquipmentMasterList())
		{
			int quantity = Math.max(1, (int) Math.round(item.qty()));
			long value = item.getCost(monster).multiply(BigDecimal.valueOf(quantity)).longValue();
			if (!item.isNatural() && value > 0)
			{
				lines.add(LootLine.carried(item.getDisplayName(), quantity, carrier, value, isResoldInFull(item), selected));
			}
		}
		long gold = new BigDecimal(ChannelUtilities.readControlledChannel(monster.getCharID(), CControl.GOLDINPUT)
			.toString()).longValue();
		if (gold > 0)
		{
			lines.add(LootLine.carried(COINS, Math.toIntExact(gold), carrier, gold, true, selected));
		}
		return lines;
	}

	/**
	 * @param monster           a monster PCGen built.
	 * @param carrier           its name in the fight.
	 * @param challengeRating   its challenge rating.
	 * @param fromCharacterFile whether it was built from a character file, as an NPC is.
	 * @param carriedValue      the value of what it carries.
	 * @return what to draw for it: its share of the treasure of its challenge rating, what it
	 *         carries deducted; nothing for an NPC, whose gear is its treasure.
	 */
	public LootDraw draw(PlayerCharacter monster, String carrier, String challengeRating, boolean fromCharacterFile,
		long carriedValue)
	{
		String creatureType = creatureTypeOf(monster);
		Race race = monster.getRace();
		TreasureType type = fromCharacterFile ? TreasureType.NPC_GEAR
			: types.of(race.getKeyName(), race.getDisplayName(), creatureType);
		long amount = Math.max(0, type.apply(TreasureBudget.forChallengeRating(challengeRating)) - carriedValue);
		return new LootDraw(carrier, creatureType, type, amount);
	}

	private static String creatureTypeOf(PlayerCharacter monster)
	{
		String raceType = monster.getDisplay().getRaceType();
		return (raceType == null || raceType.isBlank()) ? UNKNOWN_TYPE : raceType.strip().toLowerCase(Locale.ROOT);
	}

	private static boolean isResoldInFull(Equipment item)
	{
		return RESOLD_IN_FULL.stream().anyMatch(item::isType);
	}
}
