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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The bestiary treasure type of each creature, found from a PCGen race. The PCGen data
 * do not hold it; scripts/treasure_types.py extracts it from pcfinder's creatures.
 */
public final class TreasureTypes
{
	private static final String BUNDLED = "/pcgen/core/gametable/treasure-types.json";
	private static final Pattern QUALIFIED = Pattern.compile("^(.*?)\\s*\\((.*)\\)$");
	private static final Set<String> TREASURELESS_TYPES = Set.of("animal", "vermin");

	private final Map<String, String> byCreatureName;

	/**
	 * @param byCreatureName each creature's bestiary treasure line, keyed by its lowercased name.
	 */
	public TreasureTypes(Map<String, String> byCreatureName)
	{
		this.byCreatureName = Map.copyOf(byCreatureName);
	}

	/**
	 * @return the types shipped with PCGen.
	 */
	public static TreasureTypes bundled()
	{
		try (InputStream input = TreasureTypes.class.getResourceAsStream(BUNDLED))
		{
			if (input == null)
			{
				throw new IllegalStateException("PCGen ships " + BUNDLED);
			}
			return new TreasureTypes(new ObjectMapper().readValue(input, new TypeReference<Map<String, String>>()
			{
			}));
		}
		catch (IOException unreadable)
		{
			throw new UncheckedIOException(unreadable);
		}
	}

	/**
	 * @param raceKey      a race's key, such as "Demon (Vrock)".
	 * @param raceName     its name in the language shown.
	 * @param creatureType its creature type, such as "Outsider".
	 * @return its treasure type; none for an unknown animal or vermin, standard for any other.
	 */
	public TreasureType of(String raceKey, String raceName, String creatureType)
	{
		return candidates(raceKey, raceName).stream().map(byCreatureName::get).filter(found -> found != null)
			.findFirst().map(TreasureType::parse).orElseGet(() -> fallback(creatureType));
	}

	private static List<String> candidates(String raceKey, String raceName)
	{
		String key = raceKey.strip().toLowerCase(Locale.ROOT);
		List<String> names = new ArrayList<>(List.of(key));
		Matcher qualified = QUALIFIED.matcher(key);
		if (qualified.matches())
		{
			String head = qualified.group(1);
			String qualifier = qualified.group(2);
			names.addAll(List.of(qualifier, qualifier + " " + head, head + ", " + qualifier));
		}
		names.add(raceName.strip().toLowerCase(Locale.ROOT));
		return names;
	}

	private static TreasureType fallback(String creatureType)
	{
		return Optional.of(creatureType.strip().toLowerCase(Locale.ROOT)).filter(TREASURELESS_TYPES::contains)
			.map(type -> TreasureType.NONE).orElse(TreasureType.STANDARD);
	}
}
