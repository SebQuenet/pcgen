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

import java.util.List;

import pcgen.session.service.ConjurationService;

/**
 * The operations that write, read and clear a character's catalog of
 * conjurations. The fight itself is played in the Conjurations tab, never
 * through an operation.
 */
public final class ConjurationOperations
{
	private static final String CHARACTER_ID = "character_id";

	private static final String CHARACTER_ONLY_SCHEMA = """
		{
			"type": "object",
			"properties": {
				"character_id": { "type": "string", "description": "Character ID" }
			},
			"required": ["character_id"]
		}
		""";

	private static final String SYNTAX = """
		A catalog is line oriented text.

		'## title' opens a group, usually one summoning spell level. At column zero,
		'summon: <race key>' names a creature by the key of its race in the loaded sources,
		such as 'summon: Archon (Hound)'. Under it, indented two spaces, an optional
		  template: <template key>, <template key>
		lays templates on that race, such as 'template: Celestial Creature'.
		An optional 'role: tank, scout' and handwritten fields such as 'note:' may
		also describe the creature. Older entries with 'hp:' keep their authored
		statistics and names instead of being built from a race.

		PCGen builds each creature without an authored 'hp:' from its race, the Bestiary's default kit for that race
		and its templates, and computes every number; Augment Summoning is laid by itself
		when the character has the feat. A creature is named after its race followed by its
		templates in parentheses, and no two creatures share that name. A race or template
		the loaded sources lack is refused, naming its line. Bonuses laid on creatures in
		play are set by the player on the page and read back by get_conjuration_sheet.
		""";

	private ConjurationOperations()
	{
	}

	public static List<Operation> of(ConjurationService conjurations)
	{
		return List.of(setConjurationSheet(conjurations), getConjurationSheet(conjurations),
			clearConjurationSheet(conjurations));
	}

	private static Operation setConjurationSheet(ConjurationService service)
	{
		return Operation.exclusive("set_conjuration_sheet",
			"Write the catalog of conjurations of a character: the creatures it can conjure, grouped, each "
				+ "named by its race and templates, as source text. Refuses text that does not read or names "
				+ "what the loaded sources lack, naming the offending lines.\n\n" + SYNTAX,
			"""
				{
					"type": "object",
					"properties": {
						"character_id": { "type": "string", "description": "Character ID" },
						"source": { "type": "string", "description": "The catalog's source text" }
					},
					"required": ["character_id", "source"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.setConjurationSheet(
				arguments.requiredString(CHARACTER_ID),
				arguments.optionalString("source", ""))));
	}

	private static Operation getConjurationSheet(ConjurationService service)
	{
		return Operation.exclusive("get_conjuration_sheet",
			"Get the catalog of conjurations of a character as source text, together with the creatures "
				+ "it has conjured into the fight: their wounds, the rounds they have left and the bonuses "
				+ "laid on each, together with the bonuses laid on the whole group.",
			CHARACTER_ONLY_SCHEMA,
			arguments -> arguments.decodeThen(
				() -> service.getConjurationSheet(arguments.requiredString(CHARACTER_ID))));
	}

	private static Operation clearConjurationSheet(ConjurationService service)
	{
		return Operation.exclusive("clear_conjuration_sheet",
			"Remove the catalog of conjurations of a character, and the creatures it has in play.",
			CHARACTER_ONLY_SCHEMA,
			arguments -> arguments.decodeThen(
				() -> service.clearConjurationSheet(arguments.requiredString(CHARACTER_ID))));
	}
}
