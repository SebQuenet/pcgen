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

import pcgen.session.service.DataSetService;
import pcgen.session.service.SourceService;

/**
 * The source operations, each pairing the schema a caller sends with the
 * {@link SourceService} call it stands for.
 */
public final class SourceOperations
{
	private SourceOperations()
	{
	}

	public static List<Operation> of(SourceService service, DataSetService dataSet)
	{
		return List.of(
			listGameModes(service),
			listSources(service),
			loadSources(service),
			listDatasetEntries(dataSet));
	}

	private static Operation listDatasetEntries(DataSetService service)
	{
		return Operation.exclusive("list_dataset_entries",
			"List what the loaded data holds of one kind: race, class, skill, deity, alignment, stat, "
				+ "equipment, template or kit. Use it to offer a choice rather than guessing a key.",
			"""
				{
					"type": "object",
					"properties": {
						"kind": {
							"type": "string",
							"description": "race, class, skill, deity, alignment, stat, equipment, template or kit"
						},
						"name_contains": {
							"type": "string",
							"description": "Keep only entries whose key or name contains this (optional)"
						},
						"limit": {
							"type": "integer",
							"description": "How many to return at most (default: 200)",
							"default": 200
						}
					},
					"required": ["kind"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.listEntries(
				arguments.requiredString("kind"),
				arguments.optionalString("name_contains", null),
				arguments.optionalInt("limit", 0))));
	}

	private static Operation listGameModes(SourceService service)
	{
		return Operation.concurrent("list_game_modes",
			"List available game modes (e.g., D&D 3.5e, Pathfinder, D&D 5e)",
			"""
				{
					"type": "object",
					"properties": {},
					"required": []
				}
				""",
			arguments -> service.listGameModes());
	}

	private static Operation listSources(SourceService service)
	{
		return Operation.concurrent("list_sources",
			"List available campaign sources for a game mode",
			"""
				{
					"type": "object",
					"properties": {
						"game_mode": {
							"type": "string",
							"description": "Name of the game mode (from list_game_modes)"
						}
					},
					"required": ["game_mode"]
				}
				""",
			arguments -> arguments.decodeThen(
				() -> service.listSources(arguments.requiredString("game_mode"))));
	}

	private static Operation loadSources(SourceService service)
	{
		return Operation.exclusive("load_sources",
			"Load game data sources (campaigns) for a game mode. This is required before creating characters.",
			"""
				{
					"type": "object",
					"properties": {
						"game_mode": {
							"type": "string",
							"description": "Name of the game mode"
						},
						"campaigns": {
							"type": "array",
							"items": { "type": "string" },
							"description": "List of campaign keys to load (from list_sources)"
						}
					},
					"required": ["game_mode", "campaigns"]
				}
				""",
			arguments -> arguments.decodeThen(() -> service.loadSources(
				arguments.requiredString("game_mode"),
				arguments.requiredStringList("campaigns"))));
	}
}
