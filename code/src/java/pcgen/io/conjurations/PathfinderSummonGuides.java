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
package pcgen.io.conjurations;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Tactical advice for Pathfinder 1 summons, kept apart from character-authored notes. */
public final class PathfinderSummonGuides
{
	/** The closed role vocabulary, in the order the page offers it. */
	public static final List<String> ROLES = List.of("bruiser", "ranged", "area", "maneuvers", "control", "support",
		"blocker", "scout", "utility");

	private static final Map<String, Guide> FRENCH = readGuides("fr");
	private static final Map<String, Guide> ENGLISH = readGuides("en");

	private PathfinderSummonGuides()
	{
	}

	public static Guide forRace(String raceKey)
	{
		return ("fr".equals(Locale.getDefault().getLanguage()) ? FRENCH : ENGLISH).get(raceKey);
	}

	private static Map<String, Guide> readGuides(String language)
	{
		String resource = "/pcgen/conjurations/pathfinder1-guides-" + language + ".json";
		try (InputStream source = PathfinderSummonGuides.class.getResourceAsStream(resource))
		{
			if (source == null)
			{
				throw new IllegalStateException("Missing Pathfinder summon guides: " + resource);
			}
			return Map.copyOf(new ObjectMapper().readValue(source, new TypeReference<Map<String, Guide>>() { }));
		}
		catch (IOException error)
		{
			throw new UncheckedIOException("Could not read Pathfinder summon guides", error);
		}
	}

	/**
	 * @param roles each role key of {@link #ROLES} the creature fills, in
	 *              order, with how to play it in that role
	 */
	public record Guide(String interest, String usage, String limits, String bestiary, Map<String, String> roles)
	{
		public Guide
		{
			roles = (roles == null) ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(roles));
		}
	}
}
