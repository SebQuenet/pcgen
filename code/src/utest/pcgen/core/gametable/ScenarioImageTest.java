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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class ScenarioImageTest
{
	static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13};
	static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16};
	static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};

	static ScenarioImage image(int id, String name, ImageCategory category)
	{
		return ScenarioImage.of(id, name, category, PNG);
	}

	@Test
	void aPawnStandsOnTheTableAsAFigurine()
	{
		assertEquals(TabletopPiece.FIGURINE, image(1, "Goblin", ImageCategory.PAWN).forTabletop().piece());
	}

	@Test
	void aBattlemapLiesOnTheTableAsATile()
	{
		assertEquals(TabletopPiece.TILE, image(1, "Clearing", ImageCategory.BATTLEMAP).forTabletop().piece());
	}

	@Test
	void aPngIsKnownByItsFirstBytes()
	{
		assertEquals(Optional.of(ImageFormat.PNG), ImageFormat.detect(PNG));
	}

	@Test
	void aJpegIsKnownByItsFirstBytes()
	{
		assertEquals(Optional.of(ImageFormat.JPEG), ImageFormat.detect(JPEG));
	}

	@Test
	void aWebpIsNotAccepted()
	{
		assertEquals(Optional.empty(), ImageFormat.detect(WEBP));
	}

	@Test
	void anImageGivesBackItsBytes()
	{
		assertArrayEquals(PNG, image(1, "Iadara", ImageCategory.SCENARIO_ILLUSTRATION).bytes());
	}

	@Test
	void anImageThatIsNoImageIsRefused()
	{
		assertThrows(IllegalArgumentException.class,
			() -> ScenarioImage.of(1, "notes.txt", ImageCategory.BATTLEMAP, "plain text".getBytes()));
	}

	@Test
	void theScenarioShowsItsIllustrationsBeforeItsMaps()
	{
		Scenario scenario = Scenario.empty().withImage(image(1, "Kyonin", ImageCategory.GENERAL_MAP))
			.withImage(image(2, "Iadara", ImageCategory.SCENARIO_ILLUSTRATION));

		assertEquals(List.of("Iadara", "Kyonin"), scenario.images().stream().map(ScenarioImage::name).toList());
	}

	@Test
	void aBattlemapDoesNotBelongToTheScenario()
	{
		assertThrows(IllegalArgumentException.class,
			() -> Scenario.empty().withImage(image(1, "Clearing", ImageCategory.BATTLEMAP)));
	}

	@Test
	void aGeneralMapDoesNotBelongToAnEncounter()
	{
		assertThrows(IllegalArgumentException.class,
			() -> Encounter.named("Camp").withImage(image(1, "Kyonin", ImageCategory.GENERAL_MAP)));
	}

	@Test
	void anEncounterKeepsItsImagesWhenRenamed()
	{
		Encounter camp = Encounter.named("Camp").withImage(image(1, "Clearing", ImageCategory.BATTLEMAP));

		assertEquals(camp.images(), camp.withName("Ritualist's camp").images());
	}

	@Test
	void imageIdsRunAcrossTheScenarioAndItsEncounters()
	{
		Scenario scenario = Scenario.empty().withImage(image(1, "Iadara", ImageCategory.SCENARIO_ILLUSTRATION))
			.withEncounters(List.of(Encounter.named("Camp").withImage(image(4, "Clearing", ImageCategory.BATTLEMAP))));

		assertEquals(5, scenario.nextImageId());
	}

	@Test
	void aFileNameLosesWhatAFileSystemRefusesAndGainsItsExtension()
	{
		assertEquals("Carte_ Kyonin_Nord.png", image(1, "Carte: Kyonin/Nord", ImageCategory.GENERAL_MAP).fileName());
	}

	@Test
	void aFileNameKeepsAnExtensionAlreadyThere()
	{
		assertEquals("Iadara.PNG", image(1, "Iadara.PNG", ImageCategory.SCENARIO_ILLUSTRATION).fileName());
	}
}
