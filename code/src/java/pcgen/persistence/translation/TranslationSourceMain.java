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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import pcgen.core.Campaign;
import pcgen.core.Globals;
import pcgen.session.HeadlessBootstrap;
import pcgen.session.PcgenSession;
import pcgen.system.ConfigurationSettings;
import pcgen.util.GracefulExit;
import pcgen.util.Logging;

/**
 * Loads a game mode's sources and writes, beside a mirror of each campaign's place under the
 * data folder, {@code <pcc name>.en.tsv}: the English text a translation table can replace.
 * scripts/translate_data.py turns these files into translation tables.
 *
 * <p>
 * Arguments: {@code --gamemode <name> --campaigns <key>,<key>... --out <folder>}.
 */
public final class TranslationSourceMain
{
	private static final String HEADER = "TYPE\tCATEGORY\tKEY\tFIELD\tTEXT";
	private static final String CAMPAIGN_EXTENSION = ".pcc";
	private static final String SOURCE_EXTENSION = ".en.tsv";

	private TranslationSourceMain()
	{
	}

	public static void main(String... args)
	{
		HeadlessBootstrap.start(HeadlessBootstrap.settingsDirFrom(args));
		PcgenSession session = new PcgenSession();
		session.loadSources(valueAfter(args, "--gamemode"), Arrays.asList(valueAfter(args, "--campaigns").split(",")));

		Path dataFolder = Path.of(ConfigurationSettings.getPccFilesDir()).toAbsolutePath().normalize();
		Path outFolder = Path.of(valueAfter(args, "--out"));
		Map<Campaign, List<TranslatableText>> byCampaign =
				TranslationSourceExporter.textsByCampaign(Globals.getContext().getReferenceContext());
		try
		{
			for (Map.Entry<Campaign, List<TranslatableText>> campaign : byCampaign.entrySet())
			{
				if (!isCampaignFile(campaign.getKey()))
				{
					// Custom data lives in memory, with no campaign file to translate beside
					continue;
				}
				Path written = write(outFolder.resolve(sourceNameFor(campaign.getKey(), dataFolder)), campaign.getValue());
				Logging.log(Level.INFO, campaign.getValue().size() + " texts written to " + written);
			}
		}
		catch (IOException e)
		{
			Logging.errorPrint("Could not write the translation sources", e);
			GracefulExit.exit(1);
		}
		GracefulExit.exit(0);
	}

	private static boolean isCampaignFile(Campaign campaign)
	{
		return campaign.getSourceURI() != null && campaign.getSourceURI().getPath().endsWith(CAMPAIGN_EXTENSION);
	}

	private static String sourceNameFor(Campaign campaign, Path dataFolder)
	{
		String relative = dataFolder.relativize(Path.of(campaign.getSourceURI()).toAbsolutePath().normalize()).toString();
		return relative.substring(0, relative.length() - CAMPAIGN_EXTENSION.length()) + SOURCE_EXTENSION;
	}

	private static Path write(Path file, List<TranslatableText> texts) throws IOException
	{
		Files.createDirectories(file.getParent());
		StringBuilder table = new StringBuilder(HEADER).append('\n');
		for (TranslatableText text : texts)
		{
			table.append(String.join("\t", text.type(), text.category(), text.key(), text.field().name(),
				TableText.escape(text.english())))
				.append('\n');
		}
		return Files.writeString(file, table, StandardCharsets.UTF_8);
	}

	private static String valueAfter(String[] args, String flag)
	{
		for (int i = 0; i < args.length - 1; i++)
		{
			if (flag.equals(args[i]))
			{
				return args[i + 1];
			}
		}
		throw new IllegalArgumentException("Missing " + flag + " <value>");
	}
}
