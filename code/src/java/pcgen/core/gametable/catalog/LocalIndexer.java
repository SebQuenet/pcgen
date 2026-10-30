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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import pcgen.core.gametable.CombatantSheets;
import pcgen.core.gametable.ForgedMonster;
import pcgen.core.gametable.MonsterForge;
import pcgen.core.gametable.MonsterRecipe;
import pcgen.core.gametable.MonsterSource;
import pcgen.util.Logging;

/**
 * Indexes the character files of the NPC folders one at a time, on its own thread, and keeps the index on disk: a
 * file unchanged since it was indexed is not built again.
 */
public final class LocalIndexer
{
	private static final int FILES_BETWEEN_SAVES = 25;
	private static final long PAUSE_BETWEEN_BUILDS = 20;

	private final MonsterForge forge;
	private final CombatantSheets sheets;
	private final Path cacheFile;
	private final Object buildLock;
	private final AtomicInteger generation = new AtomicInteger();
	private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
		Thread thread = new Thread(task, "bestiary-index");
		thread.setDaemon(true);
		return thread;
	});
	private volatile BestiaryIndex current;

	/**
	 * @param forge     what builds the creatures.
	 * @param sheets    what reads their numbers.
	 * @param cacheFile where the index is kept between sessions.
	 * @param buildLock held while a creature is built and read: PCGen's state is shared, so whatever else builds
	 *                  creatures holds it too.
	 */
	public LocalIndexer(MonsterForge forge, CombatantSheets sheets, Path cacheFile, Object buildLock)
	{
		this.forge = Objects.requireNonNull(forge);
		this.sheets = Objects.requireNonNull(sheets);
		this.cacheFile = Objects.requireNonNull(cacheFile);
		this.buildLock = Objects.requireNonNull(buildLock);
		this.current = BestiaryIndex.read(cacheFile);
	}

	/**
	 * @return the index as it stands now.
	 */
	public BestiaryIndex current()
	{
		return current;
	}

	/**
	 * Stops the pass under way, then indexes the files on this indexer's thread.
	 *
	 * @param files      the character files to index.
	 * @param onFileDone told the number of files done so far, after each file.
	 * @param onPassDone told what the pass did.
	 */
	public void start(List<Path> files, IntConsumer onFileDone, Consumer<IndexRun> onPassDone)
	{
		cancel();
		List<Path> toIndex = List.copyOf(files);
		worker.execute(() -> onPassDone.accept(run(toIndex, onFileDone)));
	}

	/**
	 * Stops the pass under way before its next file.
	 */
	public void cancel()
	{
		generation.incrementAndGet();
	}

	/**
	 * Indexes the files on the calling thread.
	 *
	 * @param files      the character files to index.
	 * @param onFileDone told the number of files done so far, after each file.
	 * @return what the pass did.
	 */
	public IndexRun run(List<Path> files, IntConsumer onFileDone)
	{
		int pass = generation.get();
		int built = 0;
		int skipped = 0;
		for (Path file : files)
		{
			if (generation.get() != pass)
			{
				save();
				return new IndexRun(built, skipped, true);
			}
			IndexKey key = new IndexKey(IndexKey.FILE, file.toAbsolutePath().toString());
			long stamp = stampOf(file);
			if (current.stamp(key).filter(kept -> kept == stamp).isPresent())
			{
				skipped++;
			}
			else
			{
				current = current.with(key, factsOf(file), stamp);
				built++;
				letOthersBuild();
				if (built % FILES_BETWEEN_SAVES == 0)
				{
					save();
				}
			}
			onFileDone.accept(built + skipped);
		}
		save();
		return new IndexRun(built, skipped, generation.get() != pass);
	}

	private EntryFacts factsOf(Path file)
	{
		return factsOf(MonsterRecipe.of(new MonsterSource.CharacterFile(file.toAbsolutePath().toString())),
			DirectFacts.ofFile(file));
	}

	/**
	 * Builds a creature on the calling thread and reads its values.
	 *
	 * @param recipe how the creature is built.
	 * @param direct what is known of it without building it.
	 * @return its values, or why it could not be built.
	 */
	public EntryFacts factsOf(MonsterRecipe recipe, Map<CatalogField, FieldValue> direct)
	{
		synchronized (buildLock)
		{
			return switch (forge.forge(recipe))
			{
				case ForgedMonster.Forged forged ->
				{
					EntryFacts facts = new EntryFacts(direct, Optional.empty()).with(ComputedFacts.of(forged.character(), sheets));
					MonsterForge.release(forged.character());
					yield facts;
				}
				case ForgedMonster.Missing missing -> new EntryFacts(direct, Optional.of(missing.reason()));
			};
		}
	}

	/**
	 * The build lock is not fair: a short pause lets the game table's own builds in between two files.
	 */
	private static void letOthersBuild()
	{
		try
		{
			Thread.sleep(PAUSE_BETWEEN_BUILDS);
		}
		catch (InterruptedException interrupted)
		{
			Thread.currentThread().interrupt();
		}
	}

	private static long stampOf(Path file)
	{
		try
		{
			return Files.getLastModifiedTime(file).toMillis();
		}
		catch (IOException gone)
		{
			return -1;
		}
	}

	private void save()
	{
		try
		{
			current.write(cacheFile);
		}
		catch (IOException unwritable)
		{
			Logging.errorPrint("The bestiary index cannot be kept in " + cacheFile, unwritable);
		}
	}
}
