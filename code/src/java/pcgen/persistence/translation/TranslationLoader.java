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
package pcgen.persistence.translation;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import pcgen.core.Campaign;
import pcgen.rules.context.AbstractReferenceContext;

/**
 * Finds the translation table beside each loaded campaign, {@code <pcc name>.<language>.tsv},
 * and lays it over the loaded objects. Names shared by Pathfinder books are also found in
 * translation tables beside other Pathfinder campaigns.
 */
public final class TranslationLoader
{
	private static final String CAMPAIGN_EXTENSION = ".pcc";
	private static final String TABLE_EXTENSION = ".tsv";
	private static final String FILE_SCHEME = "file";

	private TranslationLoader()
	{
	}

	/**
	 * @return each entry left out, as {@code file:line: reason}
	 */
	public static List<String> load(Iterable<Campaign> campaigns, String language, AbstractReferenceContext references)
	{
		List<String> problems = new ArrayList<>();
		for (Campaign campaign : campaigns)
		{
			Path table = tableBeside(campaign.getSourceURI(), language);
			if (table == null || !Files.isRegularFile(table))
			{
				continue;
			}
			String tableName = table.getFileName().toString();
			TranslationTable parsed;
			try
			{
				parsed = TranslationTableParser.parse(Files.readString(table, StandardCharsets.UTF_8));
			}
			catch (IOException e)
			{
				problems.add(tableName + ": unreadable, " + e.getMessage());
				continue;
			}
			List<TranslationProblem> tableProblems = new ArrayList<>(parsed.problems());
			tableProblems.addAll(TranslationApplier.apply(parsed, references));
			tableProblems.forEach(problem -> problems.add(tableName + ":" + problem.line() + ": " + problem.message()));
		}
		applyPathfinderNames(campaigns, language, references);
		return problems;
	}

	private record NameKey(String type, String category, String key)
	{
	}

	private static void applyPathfinderNames(Iterable<Campaign> campaigns, String language,
		AbstractReferenceContext references)
	{
		Set<Path> roots = new HashSet<>();
		for (Campaign campaign : campaigns)
		{
			URI source = campaign.getSourceURI();
			if (source == null || !FILE_SCHEME.equals(source.getScheme()))
			{
				continue;
			}
			for (Path parent = Path.of(source).getParent(); parent != null; parent = parent.getParent())
			{
				if (parent.getFileName() != null && "pathfinder".equals(parent.getFileName().toString()))
				{
					roots.add(parent);
					break;
				}
			}
		}
		Map<NameKey, TranslationEntry> names = new HashMap<>();
		Set<NameKey> ambiguous = new HashSet<>();
		for (Path root : roots)
		{
			try (Stream<Path> files = Files.walk(root))
			{
				for (Path file : files.filter(path -> path.getFileName().toString().endsWith("." + language + TABLE_EXTENSION))
					.toList())
				{
					TranslationTable table = TranslationTableParser.parse(Files.readString(file, StandardCharsets.UTF_8));
					for (TranslationEntry entry : table.entries())
					{
						if (entry.field() != TranslatedField.NAME)
						{
							continue;
						}
						NameKey key = new NameKey(entry.type(), entry.category(), entry.key());
						TranslationEntry earlier = names.putIfAbsent(key, entry);
						if (earlier != null && !earlier.text().equals(entry.text()))
						{
							ambiguous.add(key);
						}
					}
				}
			}
			catch (IOException ignored)
			{
				// The tables beside loaded campaigns have already reported read failures.
			}
		}
		for (Map.Entry<NameKey, TranslationEntry> name : names.entrySet())
		{
			if (!ambiguous.contains(name.getKey()))
			{
				TranslationApplier.applyFallbackName(name.getValue(), references);
			}
		}
	}

	private static Path tableBeside(URI campaignSource, String language)
	{
		if (campaignSource == null || !FILE_SCHEME.equals(campaignSource.getScheme()))
		{
			return null;
		}
		Path campaignFile = Path.of(campaignSource);
		String campaignName = campaignFile.getFileName().toString();
		if (!campaignName.endsWith(CAMPAIGN_EXTENSION))
		{
			return null;
		}
		String baseName = campaignName.substring(0, campaignName.length() - CAMPAIGN_EXTENSION.length());
		return campaignFile.resolveSibling(baseName + "." + language + TABLE_EXTENSION);
	}
}
