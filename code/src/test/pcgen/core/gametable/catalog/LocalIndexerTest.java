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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

import pcgen.core.Campaign;
import pcgen.core.PlayerCharacter;
import pcgen.core.SettingsHandler;
import pcgen.core.conjurations.BestiarySources;
import pcgen.core.gametable.CombatantSheets;
import pcgen.core.gametable.ForgedMonster;
import pcgen.core.gametable.MonsterForge;
import pcgen.core.gametable.MonsterRecipe;
import pcgen.core.gametable.MonsterSource;
import pcgen.io.PCGIOHandler;
import pcgen.session.HeadlessUIDelegate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The index of the character files, kept on disk and brought up to date file by file.
 */
class LocalIndexerTest
{
	private static MonsterForge forge;
	private static CombatantSheets sheets;

	@TempDir
	Path folder;

	@BeforeAll
	static void loadTheBestiary()
	{
		forge = new MonsterForge(BestiarySources.campaigns(), new HeadlessUIDelegate());
		sheets = new CombatantSheets(forge.builder());
	}

	private Path savedWolf(String name)
	{
		ForgedMonster wolf = forge.forge(MonsterRecipe.of(new MonsterSource.BestiaryRace("Wolf")));
		PlayerCharacter character = assertInstanceOf(ForgedMonster.Forged.class, wolf).character();
		Path file = folder.resolve(name + ".pcg");
		new PCGIOHandler().write(character, SettingsHandler.getGameAsProperty().get(),
			new ArrayList<Campaign>(BestiarySources.campaigns()), file.toFile());
		return file;
	}

	private LocalIndexer indexer()
	{
		return new LocalIndexer(forge, sheets, folder.resolve("index.json"), new Object());
	}

	@Test
	void anUnchangedFileIsNotBuiltAgain()
	{
		Path wolf = savedWolf("wolf");
		indexer().run(List.of(wolf), done -> {
		});

		IndexRun second = indexer().run(List.of(wolf), done -> {
		});

		assertEquals(new IndexRun(0, 1, false), second);
	}

	@Test
	void aChangedFileIsBuiltAgain() throws IOException
	{
		Path wolf = savedWolf("wolf");
		indexer().run(List.of(wolf), done -> {
		});
		Files.setLastModifiedTime(wolf, FileTime.fromMillis(Files.getLastModifiedTime(wolf).toMillis() + 60_000));

		IndexRun second = indexer().run(List.of(wolf), done -> {
		});

		assertEquals(new IndexRun(1, 0, false), second);
	}

	@Test
	void aFileKeepsItsComputedValues()
	{
		Path wolf = savedWolf("wolf");
		LocalIndexer indexer = indexer();

		indexer.run(List.of(wolf), done -> {
		});

		assertTrue(indexer.current().facts(new IndexKey(IndexKey.FILE, wolf.toAbsolutePath().toString()))
			.flatMap(facts -> facts.value(CatalogField.ARMOR_CLASS)).isPresent());
	}

	@Test
	void anUnreadableFileIsKeptWithItsFailure() throws IOException
	{
		Path broken = Files.writeString(folder.resolve("broken.pcg"), "");
		Path wolf = savedWolf("wolf");
		LocalIndexer indexer = indexer();

		indexer.run(List.of(broken, wolf), done -> {
		});

		assertEquals(List.of(true, true), List.of(
			indexer.current().facts(new IndexKey(IndexKey.FILE, broken.toAbsolutePath().toString())).orElseThrow()
				.buildFailure().isPresent(),
			indexer.current().facts(new IndexKey(IndexKey.FILE, wolf.toAbsolutePath().toString())).orElseThrow()
				.buildFailure().isEmpty()));
	}

	@Test
	void cancellingStopsBeforeTheNextFile()
	{
		List<Path> wolves = List.of(savedWolf("first"), savedWolf("second"));
		LocalIndexer indexer = indexer();

		IndexRun run = indexer.run(wolves, done -> indexer.cancel());

		assertEquals(new IndexRun(1, 0, true), run);
	}
}
