/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package pcgen.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Searches one rules site and opens its first result: DuckDuckGo jumps straight
 * to the first result of a query that starts with a backslash.
 */
public final class FirstResultSearch
{
	public static final String NETHYS = "aonprd.com";
	public static final String PATHFINDER_FR = "pathfinder-fr.org";
	public static final String PATHFINDER_WIKI = "pathfinderwiki.com";
	public static final String D20PFSRD = "d20pfsrd.com";
	/** Pathfinder-fr also hosts Pathfinder 2 and setting pages; this term favours the Pathfinder 1 rules. */
	public static final String PATHFINDER_FR_RULES_TERM = "Pathfinder-RPG";

	private static final String SEARCH = "https://duckduckgo.com/?q=";
	/** Without this word, searching a creature lands on a homonym, such as a spell or a deity. */
	private static final String MONSTER_TERM = "monster";

	/**
	 * @param label   the site's name, as a link shows it
	 * @param address where the link opens
	 */
	public record Link(String label, String address)
	{
	}

	private FirstResultSearch()
	{
	}

	/**
	 * @param query the terms to search for
	 * @param site  the site to search, such as {@link #D20PFSRD}
	 * @return the address that opens the site's first result for the query.
	 */
	public static String addressOf(String query, String site)
	{
		return SEARCH + URLEncoder.encode("\\" + query + " site:" + site, StandardCharsets.UTF_8).replace("+", "%20");
	}

	/**
	 * @param englishName  a creature's name in the data
	 * @param localizedName its name in the user's language
	 * @return the links opening its page on Pathfinder-fr, by its localized name, then on Archives of Nethys
	 *         and d20PFSRD, by its English name.
	 */
	public static List<Link> creatureLinks(String englishName, String localizedName)
	{
		String englishQuery = englishName + " " + MONSTER_TERM;
		return List.of(new Link("Pathfinder-fr", addressOf(localizedName + " " + PATHFINDER_FR_RULES_TERM, PATHFINDER_FR)),
			new Link("Archives of Nethys", addressOf(englishQuery, NETHYS)),
			new Link("d20PFSRD", addressOf(englishQuery, D20PFSRD)));
	}
}
