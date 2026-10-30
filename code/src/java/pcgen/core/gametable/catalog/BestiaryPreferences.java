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

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import pcgen.system.ConfigurationSettings;
import pcgen.system.PCGenSettings;
import pcgen.system.PropertyContext;

/**
 * What the game master sets once for the bestiary and finds again next time: presets, columns, NPC folders.
 */
public final class BestiaryPreferences
{
	private static final String PRESETS = "gametable.bestiary.presets";
	private static final String COLUMNS = "gametable.bestiary.columns";
	private static final String NPC_FOLDERS = "gametable.npcFolders";
	private static final String COLUMN_SEPARATOR = ",";
	private static final List<CatalogField> DEFAULT_COLUMNS = List.of(CatalogField.NAME, CatalogField.CHALLENGE_RATING,
		CatalogField.TYPE, CatalogField.SIZE, CatalogField.ARMOR_CLASS, CatalogField.HIT_POINTS, CatalogField.SOURCE_BOOK);

	private final PropertyContext options;
	private final Supplier<List<Path>> defaultNpcFolders;

	/**
	 * @param options           where the preferences are kept.
	 * @param defaultNpcFolders the NPC folders until the game master sets other folders.
	 */
	public BestiaryPreferences(PropertyContext options, Supplier<List<Path>> defaultNpcFolders)
	{
		this.options = Objects.requireNonNull(options);
		this.defaultNpcFolders = Objects.requireNonNull(defaultNpcFolders);
	}

	/**
	 * @return the preferences kept in PCGen's options, with the NPC folders of this installation by default.
	 */
	public static BestiaryPreferences ofSettings()
	{
		return new BestiaryPreferences(PCGenSettings.OPTIONS_CONTEXT, BestiaryPreferences::installedNpcFolders);
	}

	/**
	 * @return every pnj folder of a homebrew source, then the characters folder.
	 */
	static List<Path> installedNpcFolders()
	{
		List<Path> folders = new ArrayList<>();
		Path homebrew = Path.of(ConfigurationSettings.getPccFilesDir(), "homebrew");
		if (Files.isDirectory(homebrew))
		{
			try (DirectoryStream<Path> sources = Files.newDirectoryStream(homebrew, Files::isDirectory))
			{
				for (Path source : sources)
				{
					if (Files.isDirectory(source.resolve("pnj")))
					{
						folders.add(source.resolve("pnj"));
					}
				}
			}
			catch (IOException unreadable)
			{
				// No homebrew NPC folder then; the characters folder remains
			}
		}
		folders.sort(null);
		String characters = PCGenSettings.getPcgDir();
		if (characters != null)
		{
			folders.add(Path.of(characters));
		}
		return folders;
	}

	public List<Preset> presets()
	{
		String kept = options.getProperty(PRESETS);
		return kept == null ? List.of() : CriterionJson.readPresets(kept);
	}

	public void savePresets(List<Preset> presets)
	{
		options.setProperty(PRESETS, CriterionJson.writePresets(presets));
	}

	public List<CatalogField> columns()
	{
		String kept = options.getProperty(COLUMNS);
		if (kept == null || kept.isBlank())
		{
			return DEFAULT_COLUMNS;
		}
		List<CatalogField> columns = Arrays.stream(kept.split(COLUMN_SEPARATOR))
			.filter(name -> Arrays.stream(CatalogField.values()).anyMatch(field -> field.name().equals(name)))
			.map(CatalogField::valueOf).toList();
		return columns.isEmpty() ? DEFAULT_COLUMNS : columns;
	}

	public void saveColumns(List<CatalogField> columns)
	{
		options.setProperty(COLUMNS, columns.stream().map(CatalogField::name).collect(Collectors.joining(COLUMN_SEPARATOR)));
	}

	public List<Path> npcFolders()
	{
		String kept = options.getProperty(NPC_FOLDERS);
		if (kept == null || kept.isBlank())
		{
			return defaultNpcFolders.get();
		}
		return Arrays.stream(kept.split(File.pathSeparator)).filter(folder -> !folder.isBlank()).map(Path::of).toList();
	}

	public void saveNpcFolders(List<Path> folders)
	{
		options.setProperty(NPC_FOLDERS, folders.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator)));
	}
}
