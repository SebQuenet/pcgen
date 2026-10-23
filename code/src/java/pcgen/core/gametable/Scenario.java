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

import java.util.List;
import java.util.Objects;

/**
 * A campaign scenario the game master runs: the sources its monsters need, the party,
 * the prepared encounters, and the fight in progress, if any.
 *
 * @param gameMode   the game mode of its sources; blank before any source is loaded
 * @param campaigns  the keys of its sources
 * @param party      the players' characters
 * @param encounters the prepared encounters, in the scenario's order
 * @param combat     the fight in progress; empty when there is none
 * @param loot       the loot of the finished fights, oldest first
 */
public record Scenario(String gameMode, List<String> campaigns, List<PartyMember> party, List<Encounter> encounters,
	Combat combat, List<LootBlock> loot)
{
	/** The extension of a scenario file. */
	public static final String FILE_EXTENSION = "pcgtable";

	public Scenario
	{
		gameMode = Objects.requireNonNull(gameMode).strip();
		campaigns = List.copyOf(campaigns);
		party = List.copyOf(party);
		encounters = List.copyOf(encounters);
		Objects.requireNonNull(combat);
		loot = List.copyOf(loot);
	}

	/**
	 * @return a scenario with nothing in it.
	 */
	public static Scenario empty()
	{
		return new Scenario("", List.of(), List.of(), List.of(), Combat.empty(), List.of());
	}

	public Scenario withSources(String newGameMode, List<String> newCampaigns)
	{
		return new Scenario(newGameMode, newCampaigns, party, encounters, combat, loot);
	}

	public Scenario withParty(List<PartyMember> newParty)
	{
		return new Scenario(gameMode, campaigns, newParty, encounters, combat, loot);
	}

	public Scenario withEncounters(List<Encounter> newEncounters)
	{
		return new Scenario(gameMode, campaigns, party, newEncounters, combat, loot);
	}

	public Scenario withCombat(Combat newCombat)
	{
		return new Scenario(gameMode, campaigns, party, encounters, newCombat, loot);
	}

	public Scenario withLoot(List<LootBlock> newLoot)
	{
		return new Scenario(gameMode, campaigns, party, encounters, combat, newLoot);
	}

	/**
	 * @return the number the next fight's loot takes.
	 */
	public int nextLootId()
	{
		return loot.stream().mapToInt(LootBlock::id).max().orElse(0) + 1;
	}
}
