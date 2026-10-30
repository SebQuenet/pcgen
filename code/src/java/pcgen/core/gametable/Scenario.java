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
import java.util.stream.Stream;

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
 * @param images     the scenario's illustrations and general maps, in the order they are shown
 */
public record Scenario(String gameMode, List<String> campaigns, List<PartyMember> party, List<Encounter> encounters,
	Combat combat, List<LootBlock> loot, List<ScenarioImage> images, List<UniqueCreature> uniques)
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
		images = List.copyOf(images);
		uniques = List.copyOf(uniques);
	}

	/**
	 * A scenario without unique creatures, as scenarios were before the bestiary kept any.
	 */
	public Scenario(String gameMode, List<String> campaigns, List<PartyMember> party, List<Encounter> encounters,
		Combat combat, List<LootBlock> loot, List<ScenarioImage> images)
	{
		this(gameMode, campaigns, party, encounters, combat, loot, images, List.of());
	}

	/**
	 * @return a scenario with nothing in it.
	 */
	public static Scenario empty()
	{
		return new Scenario("", List.of(), List.of(), List.of(), Combat.empty(), List.of(), List.of());
	}

	public Scenario withSources(String newGameMode, List<String> newCampaigns)
	{
		return new Scenario(newGameMode, newCampaigns, party, encounters, combat, loot, images, uniques);
	}

	public Scenario withParty(List<PartyMember> newParty)
	{
		return new Scenario(gameMode, campaigns, newParty, encounters, combat, loot, images, uniques);
	}

	public Scenario withEncounters(List<Encounter> newEncounters)
	{
		return new Scenario(gameMode, campaigns, party, newEncounters, combat, loot, images, uniques);
	}

	public Scenario withCombat(Combat newCombat)
	{
		return new Scenario(gameMode, campaigns, party, encounters, newCombat, loot, images, uniques);
	}

	public Scenario withLoot(List<LootBlock> newLoot)
	{
		return new Scenario(gameMode, campaigns, party, encounters, combat, newLoot, images, uniques);
	}

	/**
	 * @return the number the next fight's loot takes.
	 */
	public int nextLootId()
	{
		return loot.stream().mapToInt(LootBlock::id).max().orElse(0) + 1;
	}

	public Scenario withImages(List<ScenarioImage> newImages)
	{
		return new Scenario(gameMode, campaigns, party, encounters, combat, loot, newImages, uniques);
	}

	public Scenario withUniques(List<UniqueCreature> newUniques)
	{
		return new Scenario(gameMode, campaigns, party, encounters, combat, loot, images, newUniques);
	}

	/**
	 * @param added an illustration of the scenario or a general map.
	 * @return the scenario showing it after the images of its category.
	 */
	public Scenario withImage(ScenarioImage added)
	{
		return withImages(ScenarioImage.shelved(images, added, true));
	}

	/**
	 * @return the number the next image takes, unique across the scenario and its encounters.
	 */
	public int nextImageId()
	{
		return Stream.concat(images.stream(), encounters.stream().flatMap(found -> found.images().stream()))
			.mapToInt(ScenarioImage::id).max().orElse(0) + 1;
	}
}
