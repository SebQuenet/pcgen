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
package pcgen.session.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Locale;

import pcgen.facade.core.CharacterFacade;
import pcgen.io.ExportException;
import pcgen.io.ExportHandler;
import pcgen.io.ExportUtilities;
import pcgen.session.PcgenSession;
import pcgen.session.service.model.ExportedCharacter;
import pcgen.session.service.model.RenderedSheet;
import pcgen.system.BatchExporter;
import pcgen.system.ConfigurationSettings;

/**
 * Turning a character into a sheet, either written to a file or handed back inline.
 */
public final class ExportService
{
	private static final String DEFAULT_FORMAT = "markdown";

	private final CharacterLookup characters;

	public ExportService(PcgenSession session)
	{
		this.characters = new CharacterLookup(session);
	}

	public ServiceResult<ExportedCharacter> exportCharacter(String characterId, String templatePath,
		String outputPath)
	{
		return characters.byId(characterId).andThen(character -> {
			File template = new File(templatePath);
			if (!template.isFile())
			{
				return ServiceResult.failure(new ServiceError.EntryNotFound("Template file", templatePath));
			}
			File output = new File(outputPath);
			boolean written = ExportUtilities.isPdfTemplate(template)
				? BatchExporter.exportCharacterToPDF(character, output, template)
				: BatchExporter.exportCharacterToNonPDF(character, output, template);
			return written
				? ServiceResult.success(new ExportedCharacter(output.getAbsolutePath()))
				: ServiceResult.failure(
					new ServiceError.DataFailure("Export failed. Check template and output paths."));
		});
	}

	public ServiceResult<RenderedSheet> getCharacterSheet(String characterId, String format)
	{
		return characters.byId(characterId).andThen(character -> {
			File template = builtInTemplate(format);
			if (!template.isFile())
			{
				return ServiceResult.failure(new ServiceError.EntryNotFound("Template for format", format));
			}
			try
			{
				return ServiceResult.success(new RenderedSheet(format, render(character, template)));
			}
			catch (IOException | ExportException e)
			{
				return ServiceResult.failure(new ServiceError.DataFailure("Export error: " + e.getMessage()));
			}
		});
	}

	private static String render(CharacterFacade character, File template) throws IOException, ExportException
	{
		ExportHandler handler = ExportHandler.createExportHandler(template);
		StringWriter rendered = new StringWriter();
		BufferedWriter out = new BufferedWriter(rendered);
		character.export(handler, out);
		out.flush();
		return rendered.toString();
	}

	/**
	 * The template behind a format name. A name that is not one of the built-in
	 * formats is taken as a template file name in its own right.
	 */
	private static File builtInTemplate(String format)
	{
		String templateName = switch (format.toLowerCase(Locale.ROOT))
		{
			case "markdown", "md" -> "csheet_llm.md.ftl";
			case "toon" -> "csheet_llm.toon.ftl";
			case "json" -> "csheet_llm.json.ftl";
			case "xml" -> "base.xml.ftl";
			default -> format;
		};

		String outputSheets = ConfigurationSettings.getOutputSheetsDir();
		File perGameMode = new File(outputSheets + "/d20/fantasy/htmlxml/" + templateName);
		if (perGameMode.exists())
		{
			return perGameMode;
		}
		File shared = new File(outputSheets + "/" + templateName);
		return shared.exists() ? shared : new File(templateName);
	}

	public static String defaultFormat()
	{
		return DEFAULT_FORMAT;
	}
}
