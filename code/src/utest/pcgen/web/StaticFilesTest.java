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
package pcgen.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Which files the server agrees to hand over.
 */
public class StaticFilesTest
{
	@TempDir
	private Path root;

	private StaticFiles files;

	@BeforeEach
	public void writeAFrontEnd() throws IOException
	{
		Files.writeString(root.resolve("index.html"), "<h1>PCGen</h1>");
		Files.createDirectory(root.resolve("assets"));
		Files.writeString(root.resolve("assets").resolve("app.js"), "console.log(1)");
		files = new StaticFiles(root);
	}

	@Test
	public void servesTheIndexForTheRootPath()
	{
		StaticFiles.Served served = files.at("/").orElseThrow();

		assertEquals("<h1>PCGen</h1>", new String(served.bytes(), StandardCharsets.UTF_8));
		assertEquals("text/html; charset=utf-8", served.contentType());
	}

	@Test
	public void servesAFileInASubdirectoryWithItsOwnType()
	{
		StaticFiles.Served served = files.at("/assets/app.js").orElseThrow();

		assertEquals("console.log(1)", new String(served.bytes(), StandardCharsets.UTF_8));
		assertEquals("text/javascript; charset=utf-8", served.contentType());
	}

	@Test
	public void refusesToReachOutsideItsOwnDirectory() throws IOException
	{
		Files.writeString(root.getParent().resolve("secret.txt"), "not yours");

		assertTrue(files.at("/../secret.txt").isEmpty());
		assertTrue(files.at("/assets/../../secret.txt").isEmpty());
	}

	@Test
	public void answersNothingForAFileThatIsNotThere()
	{
		assertTrue(files.at("/nope.html").isEmpty());
	}

	@Test
	public void knowsWhetherItHasAnythingToServe()
	{
		assertTrue(files.exists());
		assertFalse(new StaticFiles(root.resolve("no-such-directory")).exists());
	}
}
