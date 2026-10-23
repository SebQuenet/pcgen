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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The images the game master keeps in the scenario, through the game table session.
 */
class GameTableImagesTest
{
	private GameTableSession session;

	@BeforeEach
	void openAScenario()
	{
		session = new GameTableSession(() -> 10);
		session.setEncounters(List.of(Encounter.named("Camp")));
	}

	@Test
	void aMapIsKeptInTheScenario()
	{
		session.addImage(Optional.empty(), "Kyonin", ImageCategory.GENERAL_MAP, ScenarioImageTest.PNG);

		assertEquals(List.of("Kyonin"), session.scenario().images().stream().map(ScenarioImage::name).toList());
	}

	@Test
	void aBattlemapIsKeptInItsEncounter()
	{
		session.addImage(Optional.of("Camp"), "Clearing", ImageCategory.BATTLEMAP, ScenarioImageTest.PNG);

		assertEquals(List.of("Clearing"),
			session.scenario().encounters().getFirst().images().stream().map(ScenarioImage::name).toList());
	}

	@Test
	void anImageIsFoundByItsId()
	{
		ScenarioImage added = session.addImage(Optional.of("Camp"), "Clearing", ImageCategory.BATTLEMAP,
			ScenarioImageTest.PNG);

		assertEquals(Optional.of(added), session.image(added.id()));
	}

	@Test
	void anImageIsRemoved()
	{
		ScenarioImage added = session.addImage(Optional.of("Camp"), "Clearing", ImageCategory.BATTLEMAP,
			ScenarioImageTest.PNG);

		session.removeImage(added.id());

		assertTrue(session.image(added.id()).isEmpty());
	}

	@Test
	void anUnknownEncounterIsRefused()
	{
		assertThrows(IllegalArgumentException.class, () -> session.addImage(Optional.of("Nowhere"), "Clearing",
			ImageCategory.BATTLEMAP, ScenarioImageTest.PNG));
	}
}
