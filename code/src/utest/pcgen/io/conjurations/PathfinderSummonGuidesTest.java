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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class PathfinderSummonGuidesTest
{
	@Test
	void describesEveryCreatureInTheCurrentPathfinderCatalog() throws Exception
	{
		Locale previous = Locale.getDefault();
		try
		{
			for (Locale locale : new Locale[] {Locale.ENGLISH, Locale.FRENCH})
			{
				Locale.setDefault(locale);
				assertCompleteCatalog();
			}
		}
		finally
		{
			Locale.setDefault(previous);
		}
	}

	private void assertCompleteCatalog() throws Exception
	{
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
					PathfinderSummonGuides.Guide guide = PathfinderSummonGuides.forRace(race);
					assertNotNull(guide, race);
					assertFalse(guide.interest().isBlank(), race);
					assertFalse(guide.bestiary().isBlank(), race);
					assertTrue(guide.roles().size() >= 1 && guide.roles().size() <= 3, race);
					guide.roles().forEach((role, usage) -> {
						assertTrue(PathfinderSummonGuides.ROLES.contains(role), race + ": " + role);
						assertFalse(usage.isBlank(), race + ": " + role);
					});
				}
			}
		}
		assertNull(PathfinderSummonGuides.forRace("Unknown Race"));
	}

	@Test
	void guideTextFollowsTheInterfaceLanguage()
	{
		Locale previous = Locale.getDefault();
		try
		{
			Locale.setDefault(Locale.ENGLISH);
			assertTrue(PathfinderSummonGuides.forRace("Archon (Hound)").interest()
				.contains("Durable defender"));
			Locale.setDefault(Locale.FRENCH);
			assertTrue(PathfinderSummonGuides.forRace("Archon (Hound)").interest()
				.contains("Combattant solide"));
		}
		finally
		{
			Locale.setDefault(previous);
		}
	}

	@Test
	void bothLanguagesGiveTheSameRolesInTheSameOrder() throws Exception
	{
		Locale previous = Locale.getDefault();
		try
		{
			for (String race : catalogRaces())
			{
				Locale.setDefault(Locale.ENGLISH);
				List<String> english = List.copyOf(PathfinderSummonGuides.forRace(race).roles().keySet());
				Locale.setDefault(Locale.FRENCH);
				List<String> french = List.copyOf(PathfinderSummonGuides.forRace(race).roles().keySet());
				assertEquals(english, french, race);
			}
		}
		finally
		{
			Locale.setDefault(previous);
		}
	}

	private List<String> catalogRaces() throws Exception
	{
		List<String> races = new ArrayList<>();
		try (var source = getClass().getResourceAsStream("/pcgen/conjurations/pathfinder1-options.tsv");
			var lines = new BufferedReader(new InputStreamReader(source, StandardCharsets.UTF_8)))
		{
			for (String line; (line = lines.readLine()) != null; )
			{
				if (!line.startsWith("#") && !line.isBlank())
				{
					String[] fields = line.split("\\t", -1);
					races.add(fields[3].isBlank() ? fields[2] : fields[3]);
				}
			}
		}
		return races;
	}
}
