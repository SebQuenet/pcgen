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
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

/**
 * Illustrations of Pathfinder 1 summons, one 250x250 JPEG per race, drawn by
 * {@code scripts/generate_summon_portraits.py}. They reach the page as data
 * URIs because the page is loaded without a base address.
 */
public final class PathfinderSummonPortraits
{
	private static final String DIRECTORY = "/pcgen/conjurations/portraits/";
	private static final String JPEG_DATA_URI = "data:image/jpeg;base64,";

	private PathfinderSummonPortraits()
	{
	}

	public static Optional<String> dataUriFor(String raceKey)
	{
		try (InputStream portrait = PathfinderSummonPortraits.class.getResourceAsStream(DIRECTORY + fileNameOf(raceKey)))
		{
			if (portrait == null)
			{
				return Optional.empty();
			}
			return Optional.of(JPEG_DATA_URI + Base64.getEncoder().encodeToString(portrait.readAllBytes()));
		}
		catch (IOException error)
		{
			throw new UncheckedIOException("Could not read the portrait of " + raceKey, error);
		}
	}

	/** Same name as {@code portrait_name} in the generation script. */
	private static String fileNameOf(String raceKey)
	{
		String slug = raceKey.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
		return slug + ".jpg";
	}
}
