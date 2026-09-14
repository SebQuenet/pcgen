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
package pcgen.session.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import pcgen.session.PcgenSession;

/**
 * What the registry offers a transport, before any game data is loaded.
 */
public class OperationRegistryTest
{
	@Test
	public void answersToTheNameOfAnOperationItHolds()
	{
		OperationRegistry registry = OperationRegistry.forSession(new PcgenSession());

		assertTrue(registry.find("load_sources").isPresent());
	}

	@Test
	public void answersNothingForANameItDoesNotHold()
	{
		OperationRegistry registry = OperationRegistry.forSession(new PcgenSession());

		assertFalse(registry.find("summon_dragon").isPresent());
	}

	@Test
	public void namesEveryOperationOnlyOnce()
	{
		OperationRegistry registry = OperationRegistry.forSession(new PcgenSession());

		List<String> names = registry.all().stream().map(Operation::name).toList();

		assertEquals(names.size(), names.stream().distinct().count());
	}

	@Test
	public void marksLoadingSourcesAsNeedingTheSessionToItself()
	{
		OperationRegistry registry = OperationRegistry.forSession(new PcgenSession());

		assertTrue(registry.find("load_sources").orElseThrow().needsExclusiveAccess());
		assertFalse(registry.find("list_game_modes").orElseThrow().needsExclusiveAccess());
	}
}
