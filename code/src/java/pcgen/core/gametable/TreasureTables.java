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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The treasure generation tables of Ultimate Equipment, as scripts/ue_treasure_tables.py
 * extracts them: the treasure types of each creature type, the rewards of each treasure
 * type, and the items a reward's random rolls turn into.
 *
 * @param familiesByCreatureType the treasure types (letters A to I) of each creature type, lowercased.
 * @param lotsByFamily           the rewards of each treasure type.
 * @param pools                  the items each kind of random roll can give, such as "potion/lesser minor".
 */
public record TreasureTables(Map<String, List<String>> familiesByCreatureType, Map<String, List<Lot>> lotsByFamily,
	Map<String, List<PoolEntry>> pools)
{
	private static final String BUNDLED = "/pcgen/core/gametable/treasure-tables.json";

	public TreasureTables
	{
		familiesByCreatureType = Map.copyOf(familiesByCreatureType);
		lotsByFamily = Map.copyOf(lotsByFamily);
		pools = Map.copyOf(pools);
	}

	/**
	 * A reward of a treasure type.
	 *
	 * @param value  what the book says it is worth, in gold pieces.
	 * @param reward what it holds.
	 */
	public record Lot(long value, List<Part> reward)
	{
		public Lot
		{
			reward = List.copyOf(reward);
		}
	}

	/**
	 * One part of a reward: rolled coins, or a number of random items from a pool.
	 *
	 * @param kind       "coins" or "pool".
	 * @param count      how many items of the pool.
	 * @param dice       how many dice the coins roll.
	 * @param sides      the sides of those dice.
	 * @param multiplier what the roll is multiplied by.
	 * @param coin       the coin: cp, sp, gp or pp.
	 * @param pool       the pool the items come from.
	 */
	public record Part(String kind, int count, int dice, int sides, long multiplier, String coin, String pool)
	{
		public static Part pool(int count, String pool)
		{
			return new Part("pool", count, 0, 0, 0, "", pool);
		}
	}

	/**
	 * Rolled gold pieces added to an item's price, as a gem's value.
	 */
	public record Dice(int count, int sides, long multiplier)
	{
	}

	/**
	 * An item a random roll can give.
	 *
	 * @param name         its name.
	 * @param price        its price, in gold pieces; the base value for a gem.
	 * @param weight       its chance among the pool's items.
	 * @param resoldInFull whether it sells for its whole value.
	 * @param added        the gold pieces rolled on top of its price, for a gem.
	 */
	public record PoolEntry(String name, long price, long weight, boolean resoldInFull, Optional<Dice> added)
	{
	}

	/**
	 * @return the tables shipped with PCGen.
	 */
	public static TreasureTables bundled()
	{
		try (InputStream input = TreasureTables.class.getResourceAsStream(BUNDLED))
		{
			if (input == null)
			{
				throw new IllegalStateException("PCGen ships " + BUNDLED);
			}
			JsonNode root = new ObjectMapper().readTree(input);
			return new TreasureTables(
				fields(root.get("familiesByCreatureType"), families -> list(families, JsonNode::asText)),
				fields(root.get("lotsByFamily"), lots -> list(lots, TreasureTables::lot)),
				fields(root.get("pools"), entries -> list(entries, TreasureTables::poolEntry)));
		}
		catch (IOException unreadable)
		{
			throw new UncheckedIOException(unreadable);
		}
	}

	private static Lot lot(JsonNode node)
	{
		return new Lot(node.get("value").asLong(), list(node.get("reward"), part -> new Part(part.get("kind").asText(),
			part.path("count").asInt(1), part.path("dice").asInt(), part.path("sides").asInt(),
			part.path("multiplier").asLong(), part.path("coin").asText(), part.path("pool").asText())));
	}

	private static PoolEntry poolEntry(JsonNode node)
	{
		Optional<Dice> added = Optional.ofNullable(node.get("dice")).map(dice -> new Dice(dice.get("count").asInt(),
			dice.get("sides").asInt(), dice.get("multiplier").asLong()));
		return new PoolEntry(node.get("name").asText(), node.get("price").asLong(), node.get("weight").asLong(),
			node.path("resoldInFull").asBoolean(), added);
	}

	private static <T> Map<String, T> fields(JsonNode object, Function<JsonNode, T> reader)
	{
		Map<String, T> values = new LinkedHashMap<>();
		object.properties().forEach(field -> values.put(field.getKey(), reader.apply(field.getValue())));
		return values;
	}

	private static <T> List<T> list(JsonNode array, Function<JsonNode, T> reader)
	{
		List<T> values = new ArrayList<>();
		array.forEach(element -> values.add(reader.apply(element)));
		return values;
	}
}
