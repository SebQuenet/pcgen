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
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import pcgen.io.conjurations.PathfinderSummonPortraits;

/**
 * The tactical plan of a bestiary creature: how the game master plays it, written as the
 * source text of a tactical sheet and named after the race key, such as giant_stone.txt.
 */
public final class BestiaryTactics
{
	private static final String DIRECTORY = "/pcgen/bestiary/tactics/";
	private static final String EXTENSION = ".txt";

	private BestiaryTactics()
	{
	}

	/**
	 * @param raceKey the race the monster is built from.
	 * @return the source text of its tactical plan; empty when none was written for it.
	 */
	public static Optional<String> of(String raceKey)
	{
		String fileName = PathfinderSummonPortraits.slugOf(raceKey) + EXTENSION;
		try (InputStream plan = BestiaryTactics.class.getResourceAsStream(DIRECTORY + fileName))
		{
			if (plan == null)
			{
				return Optional.empty();
			}
			return Optional.of(new String(plan.readAllBytes(), StandardCharsets.UTF_8));
		}
		catch (IOException error)
		{
			throw new UncheckedIOException("Could not read the tactical plan " + fileName, error);
		}
	}
}
