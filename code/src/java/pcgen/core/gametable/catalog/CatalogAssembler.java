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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import pcgen.core.Globals;
import pcgen.core.Kit;
import pcgen.core.Race;
import pcgen.core.gametable.MonsterSource;
import pcgen.util.Logging;

/**
 * Lists the bestiary's entries: the races and NPC kits of the loaded sources, and the character files of the NPC folders.
 */
public final class CatalogAssembler
{
	private static final String CHARACTER_EXTENSION = ".pcg";

	private CatalogAssembler()
	{
	}

	/**
	 * @return the races and NPC kits of the loaded sources, with what their data say of them.
	 */
	public static List<CatalogEntry> ofLoadedData()
	{
		if (Globals.getContext() == null)
		{
			return List.of();
		}
		List<CatalogEntry> entries = new ArrayList<>();
		Globals.getContext().getReferenceContext().getConstructedCDOMObjects(Race.class).stream()
			.filter(race -> !race.isUnselected())
			.forEach(race -> entries.add(new CatalogEntry(race.getLocalizedName(),
				new MonsterSource.BestiaryRace(race.getKeyName()), EntryKind.RACE,
				new EntryFacts(DirectFacts.ofRace(race), Optional.empty()))));
		Globals.getContext().getReferenceContext().getConstructedCDOMObjects(Kit.class).stream()
			.filter(kit -> kit.isType("NPC"))
			.forEach(kit -> entries.add(new CatalogEntry(kit.getLocalizedName(), new MonsterSource.NpcKit(kit.getKeyName()),
				EntryKind.KIT, new EntryFacts(DirectFacts.ofKit(kit), Optional.empty()))));
		entries.sort(Comparator.comparing(CatalogEntry::name, String.CASE_INSENSITIVE_ORDER));
		return entries;
	}

	/**
	 * @param folders the NPC folders, searched with their subfolders.
	 * @return their character files, each once, by name.
	 */
	public static List<Path> characterFiles(List<Path> folders)
	{
		List<Path> files = new ArrayList<>();
		for (Path folder : folders)
		{
			if (!Files.isDirectory(folder))
			{
				continue;
			}
			try (Stream<Path> found = Files.walk(folder))
			{
				found.filter(file -> file.getFileName().toString().endsWith(CHARACTER_EXTENSION) && Files.isRegularFile(file))
					.map(file -> file.toAbsolutePath().normalize()).filter(file -> !files.contains(file)).forEach(files::add);
			}
			catch (IOException | RuntimeException unreadable)
			{
				Logging.errorPrint("The NPC folder " + folder + " cannot be read", unreadable);
			}
		}
		files.sort(Comparator.comparing(file -> file.getFileName().toString(), String.CASE_INSENSITIVE_ORDER));
		return files;
	}

	/**
	 * @param files character files.
	 * @return their entries, with what is known of them without reading them.
	 */
	public static List<CatalogEntry> ofFiles(List<Path> files)
	{
		return files.stream().map(file -> new CatalogEntry(
			((FieldValue.Text) DirectFacts.ofFile(file).get(CatalogField.NAME)).value(),
			new MonsterSource.CharacterFile(file.toString()), EntryKind.FILE,
			new EntryFacts(DirectFacts.ofFile(file), Optional.empty()))).toList();
	}
}
