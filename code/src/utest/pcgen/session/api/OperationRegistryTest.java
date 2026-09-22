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
	/**
	 * The 77 tools the MCP server exposed when every operation still lived inside
	 * its own lambda. Losing one in a migration would be silent otherwise; gaining
	 * one is fine, which is why this is a floor rather than the whole list.
	 */
	private static final List<String> OPERATIONS_BEFORE_THE_SERVICE_LAYER = List.of(
		"add_ability", "add_class_level", "add_companion", "add_domain", "add_kit", "add_known_spell",
		"add_language", "add_prepared_spell", "add_temp_bonus", "add_template", "add_to_spellbook",
		"batch_add_abilities", "batch_add_prepared_spells", "batch_buy_equipment", "batch_invest_skills",
		"buy_equipment", "clear_tactical_sheet", "close_character", "create_character", "create_equipment_set",
		"customize_equipment", "equip_item", "equip_items", "export_character", "get_available_spells",
		"get_biography", "get_character", "get_character_details", "get_character_sheet", "get_companions",
		"get_equipped_items", "get_inventory", "get_known_spells", "get_languages", "get_pending_choices",
		"get_prepared_spells", "get_skill_summary", "get_spellbooks", "get_tactical_sheet", "get_templates",
		"get_todo_list", "invest_skill_points", "is_dirty", "is_qualified_for", "list_abilities",
		"list_ability_categories", "list_equipment_modifiers", "list_equipment_sets", "list_game_modes",
		"list_sources", "list_tactical_references", "list_temp_bonuses", "load_sources", "open_character",
		"remove_ability", "remove_companion", "remove_domain", "remove_known_spell", "remove_language",
		"remove_prepared_spell", "remove_temp_bonus", "remove_template", "resolve_choice", "roll_stats",
		"save_character", "sell_equipment", "set_ability_score", "set_alignment", "set_all_ability_scores",
		"set_biography", "set_deity", "set_funds", "set_name", "set_race", "set_tactical_sheet", "set_xp",
		"unequip_item");

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
	public void servesEveryOperationTheMcpServerUsedToServe()
	{
		OperationRegistry registry = OperationRegistry.forSession(new PcgenSession());

		List<String> served = registry.all().stream().map(Operation::name).toList();

		assertTrue(served.containsAll(OPERATIONS_BEFORE_THE_SERVICE_LAYER),
			() -> "lost: " + OPERATIONS_BEFORE_THE_SERVICE_LAYER.stream().filter(name -> !served.contains(name))
				.toList());
	}

	@Test
	public void namesEveryOperationOnlyOnce()
	{
		OperationRegistry registry = OperationRegistry.forSession(new PcgenSession());

		List<String> names = registry.all().stream().map(Operation::name).toList();

		assertEquals(names.size(), names.stream().distinct().count());
	}

	@Test
	public void letsACallerNameTheSubclassTakenWithAClassLevel()
	{
		OperationRegistry registry = OperationRegistry.forSession(new PcgenSession());

		String schema = registry.find("add_class_level").orElseThrow().inputSchema();

		// A wizard's arcane school is a subclass, and headless PCGen answers that
		// chooser itself unless the caller says which one.
		assertTrue(schema.contains("\"subclass\""), () -> schema);
	}

	@Test
	public void marksLoadingSourcesAsNeedingTheSessionToItself()
	{
		OperationRegistry registry = OperationRegistry.forSession(new PcgenSession());

		assertTrue(registry.find("load_sources").orElseThrow().needsExclusiveAccess());
		assertFalse(registry.find("list_game_modes").orElseThrow().needsExclusiveAccess());
	}
}
