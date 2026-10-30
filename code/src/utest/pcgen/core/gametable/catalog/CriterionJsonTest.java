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
package pcgen.core.gametable.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;

import pcgen.system.PropertyContext;

import org.junit.jupiter.api.Test;

/**
 * Criteria and presets kept as text in the preferences.
 */
class CriterionJsonTest
{
	private static final Criterion FLYING_UNDEAD = new Criterion.Group(Joiner.ALL, List.of(
		new Criterion.Test(CatalogField.TYPE, Operator.ANY_OF, List.of("Undead")),
		new Criterion.Group(Joiner.ANY, List.of(new Criterion.Test(CatalogField.FLY_SPEED, Operator.AT_LEAST, List.of("1")),
			new Criterion.Test(CatalogField.CHALLENGE_RATING, Operator.BETWEEN, List.of("6", "8"))))));

	private static BestiaryPreferences preferences()
	{
		return new BestiaryPreferences(new PropertyContext("test")
		{
		}, () -> List.of(Path.of("/campaign/pnj")));
	}

	@Test
	void aNestedCriterionComesBack()
	{
		assertEquals(new CriterionReading(FLYING_UNDEAD, List.of()), CriterionJson.read(CriterionJson.write(FLYING_UNDEAD)));
	}

	@Test
	void aFieldThatNoLongerExistsIsIgnoredAndReported()
	{
		String json = CriterionJson.write(FLYING_UNDEAD).replace("\"FLY_SPEED\"", "\"ENVIRONMENT\"");

		CriterionReading reading = CriterionJson.read(json);

		assertEquals(List.of("ENVIRONMENT"), reading.ignoredFields());
	}

	@Test
	void presetsComeBackFromThePreferences()
	{
		BestiaryPreferences preferences = preferences();
		List<Preset> presets = List.of(new Preset("Sous-sol FP 6-8 volants", FLYING_UNDEAD));

		preferences.savePresets(presets);

		assertEquals(presets, preferences.presets());
	}

	@Test
	void theDefaultColumnsAreTheSpecs()
	{
		assertEquals(List.of(CatalogField.NAME, CatalogField.CHALLENGE_RATING, CatalogField.TYPE, CatalogField.SIZE,
			CatalogField.ARMOR_CLASS, CatalogField.HIT_POINTS, CatalogField.SOURCE_BOOK), preferences().columns());
	}

	@Test
	void savedNpcFoldersReplaceTheDefaults()
	{
		BestiaryPreferences preferences = preferences();

		preferences.saveNpcFolders(List.of(Path.of("/a"), Path.of("/b")));

		assertEquals(List.of(Path.of("/a"), Path.of("/b")), preferences.npcFolders());
	}
}
