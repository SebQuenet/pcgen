/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package pcgen.core.conjurations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** RPGBOT's ratings for the spell and level in which a creature appears. */
public final class PathfinderSummonRankings
{
	private static final String RESOURCE = "/pcgen/conjurations/pathfinder1-rankings.tsv";
	private static final Map<String, Ranking> RANKINGS = readRankings();

	public record Ranking(String color, String source)
	{
		public int score()
		{
			return switch (color)
			{
				case "red" -> 1;
				case "orange" -> 2;
				case "green" -> 3;
				case "blue" -> 4;
				default -> 0;
			};
		}
	}

	private PathfinderSummonRankings()
	{
	}

	public static Ranking forOption(String family, int level, String raceKey)
	{
		return RANKINGS.get(key(family, level, raceKey));
	}

	private static String key(String family, int level, String raceKey)
	{
		return family + '\t' + level + '\t' + raceKey;
	}

	private static Map<String, Ranking> readRankings()
	{
		try (InputStream source = PathfinderSummonRankings.class.getResourceAsStream(RESOURCE))
		{
			if (source == null)
			{
				throw new IllegalStateException("Missing Pathfinder summon rankings: " + RESOURCE);
			}
			Map<String, Ranking> rankings = new HashMap<>();
			try (BufferedReader lines = new BufferedReader(new InputStreamReader(source, StandardCharsets.UTF_8)))
			{
				for (String line; (line = lines.readLine()) != null;)
				{
					if (line.isBlank() || line.startsWith("#"))
					{
						continue;
					}
					String[] fields = line.split("\\t", -1);
					if (fields.length != 5)
					{
						throw new IllegalStateException("Invalid Pathfinder summon ranking: " + line);
					}
					rankings.put(key(fields[0], Integer.parseInt(fields[1]), fields[2]),
						new Ranking(fields[3], fields[4]));
				}
			}
			return Map.copyOf(rankings);
		}
		catch (IOException error)
		{
			throw new UncheckedIOException("Could not read Pathfinder summon rankings", error);
		}
	}
}
