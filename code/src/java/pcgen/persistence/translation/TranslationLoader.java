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
import java.util.List;

import pcgen.core.Campaign;
import pcgen.rules.context.AbstractReferenceContext;

/**
 * Finds the translation table beside each loaded campaign, {@code <pcc name>.<language>.tsv},
 * and lays it over the loaded objects. A campaign without one stays in English.
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
		return problems;
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
