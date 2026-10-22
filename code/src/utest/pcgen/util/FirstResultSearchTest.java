/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package pcgen.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * A DuckDuckGo address that opens the first result of a search on one site.
 */
class FirstResultSearchTest
{
	@Test
	void asksForTheFirstResultOnTheSite()
	{
		assertEquals("https://duckduckgo.com/?q=%5CLegion%20Archon%20monster%20site%3Ad20pfsrd.com",
			FirstResultSearch.addressOf("Legion Archon monster", FirstResultSearch.D20PFSRD));
	}

	@Test
	void encodesAccentedNames()
	{
		assertEquals("https://duckduckgo.com/?q=%5CArchon%20l%C3%A9gion%20Pathfinder-RPG%20site%3Apathfinder-fr.org",
			FirstResultSearch.addressOf("Archon légion Pathfinder-RPG", FirstResultSearch.PATHFINDER_FR));
	}
}
