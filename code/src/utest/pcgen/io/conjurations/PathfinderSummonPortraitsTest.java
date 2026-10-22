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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class PathfinderSummonPortraitsTest
{
	private static final String JPEG_DATA_URI = "data:image/jpeg;base64,";

	@Test
	void portraitIsAnEmbeddableJpeg()
	{
		Optional<String> portrait = PathfinderSummonPortraits.dataUriFor("Archon (Hound)");

		assertTrue(portrait.orElse("").startsWith(JPEG_DATA_URI), portrait.toString());
	}

	@Test
	void raceWithoutPortraitHasNone()
	{
		Optional<String> portrait = PathfinderSummonPortraits.dataUriFor("Unknown Race");

		assertEquals(Optional.empty(), portrait);
	}

	@Test
	void caninesInPlayCycleThroughTheirSixIllustrations()
	{
		Optional<String> first = PathfinderSummonPortraits.cardDataUriFor("Dog", 1);

		assertEquals(first, PathfinderSummonPortraits.cardDataUriFor("Dog", 7));
	}

	@Test
	void caninesInPlayDoNotAllShareOneIllustration()
	{
		Optional<String> first = PathfinderSummonPortraits.cardDataUriFor("Wolf (Dire)", 1);

		assertNotEquals(first, PathfinderSummonPortraits.cardDataUriFor("Wolf (Dire)", 2));
	}

	@Test
	void canineInPlayIsShownWithAnEmbeddableJpeg()
	{
		Optional<String> portrait = PathfinderSummonPortraits.cardDataUriFor("Wolf", 3);

		assertTrue(portrait.orElse("").startsWith(JPEG_DATA_URI), portrait.toString());
	}

	@Test
	void otherCreaturesInPlayHaveNoCardIllustration()
	{
		Optional<String> portrait = PathfinderSummonPortraits.cardDataUriFor("Archon (Hound)", 1);

		assertEquals(Optional.empty(), portrait);
	}

	@Test
	void everyRaceInThePathfinderCatalogHasAPortrait() throws Exception
	{
		List<String> withoutPortrait = new ArrayList<>();
		try (var source = getClass().getResourceAsStream("/pcgen/conjurations/pathfinder1-options.tsv"))
		{
			assertNotNull(source);
			try (var lines = new BufferedReader(new InputStreamReader(source, StandardCharsets.UTF_8)))
			{
				for (String line; (line = lines.readLine()) != null; )
				{
					if (line.startsWith("#") || line.isBlank())
					{
						continue;
					}
					String[] fields = line.split("\\t", -1);
					String race = fields[3].isBlank() ? fields[2] : fields[3];
					if (PathfinderSummonPortraits.dataUriFor(race).isEmpty() && !withoutPortrait.contains(race))
					{
						withoutPortrait.add(race);
					}
				}
			}
		}

		assertEquals(List.of(), withoutPortrait);
	}
}
