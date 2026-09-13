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
package pcgen.io;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import freemarker.template.Configuration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The output sheets that print a tactical sheet still parse as FreeMarker.
 *
 * <p>
 * Parsing is what a typo in a directive breaks, and these files are far too
 * large to spot one by eye.
 */
class TacticalOutputSheetSyntaxTest
{

	private static final Path OUTPUT_SHEETS = Path.of("outputsheets");

	@ParameterizedTest
	@ValueSource(strings = {"base.xml.ftl", "d20/fantasy/htmlxml/csheet_fantasy_std.htm.ftl"})
	void parsesAsFreeMarker(String sheet)
	{
		File directory = OUTPUT_SHEETS.toFile();
		assertTrue(directory.isDirectory(), "output sheets not found at " + directory.getAbsolutePath());

		Configuration configuration = new Configuration(Configuration.VERSION_2_3_34);
		assertDoesNotThrow(() -> {
			configuration.setDirectoryForTemplateLoading(directory);
			configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
			configuration.getTemplate(sheet);
		});
	}
}
