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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import pcgen.core.tactics.TacticalParseFailure;
import pcgen.core.tactics.TacticalPlanParser;

import org.junit.jupiter.api.Test;

/**
 * The tactical plans written for the bestiary's monsters, found from the race they play.
 */
class BestiaryTacticsTest
{
	@Test
	void anAbolethIsPlayedByThePlanNamedAfterItsKey()
	{
		assertTrue(BestiaryTactics.of("Aboleth").isPresent());
	}

	@Test
	void aRaceWithoutPlanHasNone()
	{
		assertEquals(Optional.empty(), BestiaryTactics.of("Unknown Race"));
	}

	@Test
	void everyPlanReads() throws IOException, URISyntaxException
	{
		Path directory = Path.of(Objects.requireNonNull(BestiaryTactics.class.getResource("/pcgen/bestiary/tactics/")).toURI());
		List<String> unreadable;
		try (Stream<Path> plans = Files.list(directory))
		{
			unreadable = plans
				.filter(plan -> TacticalPlanParser.parse(readPlan(plan)) instanceof TacticalParseFailure)
				.map(plan -> plan.getFileName() + ": " + TacticalPlanParser.parse(readPlan(plan)))
				.toList();
		}

		assertEquals(List.of(), unreadable);
	}

	private static String readPlan(Path plan)
	{
		try
		{
			return Files.readString(plan, StandardCharsets.UTF_8);
		}
		catch (IOException error)
		{
			throw new IllegalStateException(error);
		}
	}
}
