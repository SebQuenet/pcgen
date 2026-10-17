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
		'summon: <name>' opens a creature. Its fields follow, each indented two spaces:
		  role: <role>, <role>   what it is for; the catalog filters on these
		  speed: hp: ac: attack: damage: special: cmb: cmd: ref: fort: will: init: spells: note:

		Every field is optional and written at most once per creature. No two creatures
		share a name, whatever its case. Values are free text, written as the player wants
		to read them; the two characters \\n in a value stand for a line break. The fight
		takes the first whole number of 'hp:' as the creature's maximum hit points.
		Nothing is computed from PCGen's data, so write the values out.
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
			"Write the catalog of conjurations of a character: the creatures it can conjure, grouped and "
				+ "with their stat lines, as source text. Refuses text that does not read, naming the "
				+ "offending lines.\n\n" + SYNTAX,
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
				+ "it has conjured into the fight: their wounds and the rounds they have left.",
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
