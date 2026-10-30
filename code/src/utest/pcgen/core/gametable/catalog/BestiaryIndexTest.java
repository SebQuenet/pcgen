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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The bestiary's index kept on disk.
 */
class BestiaryIndexTest
{
	private static final IndexKey ABOLETH = new IndexKey("race", "Aboleth");
	private static final EntryFacts FACTS = new EntryFacts(Map.of(CatalogField.CHALLENGE_RATING, new FieldValue.Number(1.0 / 3),
		CatalogField.NAME, new FieldValue.Text("Aboleth"), CatalogField.SUBTYPES, new FieldValue.Items(Set.of("Aquatic")),
		CatalogField.SKILLS, new FieldValue.Scores(Map.of("Swim", 24)), CatalogField.TELEPATHY, new FieldValue.Flag(false)),
		Optional.empty());

	@Test
	void anIndexComesBackFromItsFile(@TempDir Path folder) throws IOException
	{
		Path file = folder.resolve("index.json");
		BestiaryIndex index = BestiaryIndex.empty().with(ABOLETH, FACTS, 42)
			.with(new IndexKey("file", "/pnj/broken.pcg"), new EntryFacts(Map.of(), Optional.of("unreadable")), 7);

		index.write(file);

		assertEquals(index, BestiaryIndex.read(file));
	}

	@Test
	void aMissingFileReadsAsAnEmptyIndex(@TempDir Path folder)
	{
		assertEquals(BestiaryIndex.empty(), BestiaryIndex.read(folder.resolve("absent.json")));
	}

	@Test
	void aCorruptFileReadsAsAnEmptyIndex(@TempDir Path folder) throws IOException
	{
		Path file = folder.resolve("index.json");
		Files.writeString(file, "{ not json");

		assertEquals(BestiaryIndex.empty(), BestiaryIndex.read(file));
	}

	@Test
	void anEntryNeverIndexedHasNoStamp()
	{
		assertEquals(List.of(Optional.empty(), Optional.of(42L)), List.of(BestiaryIndex.empty().stamp(ABOLETH),
			BestiaryIndex.empty().with(ABOLETH, FACTS, 42).stamp(ABOLETH)));
	}
}
