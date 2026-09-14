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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

import pcgen.session.HeadlessBootstrap;
import pcgen.session.PcgenSession;
import pcgen.session.api.OperationRegistry;
import pcgen.util.GracefulExit;
import pcgen.util.Logging;

/**
 * Entry point for the PCGen HTTP server. Brings PCGen up headless and serves its
 * operations to a web front end running on the same machine.
 */
public final class WebMain
{
	private static final int DEFAULT_PORT = 8420;

	private WebMain()
	{
	}

	public static void main(String... args)
	{
		Logging.log(Level.INFO, "Starting PCGen HTTP server...");
		HeadlessBootstrap.start(HeadlessBootstrap.settingsDirFrom(args));

		PcgenSession session = new PcgenSession();
		try
		{
			HttpApiServer server = HttpApiServer.start(OperationRegistry.forSession(session), portFrom(args));
			if (!Files.isDirectory(Path.of("web")))
			{
				Logging.log(Level.INFO, "No front end in web/ — the API is served, the pages are not. "
					+ "Run ./gradlew buildWebapp to build one.");
			}
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				Logging.log(Level.INFO, "Shutting down PCGen HTTP server...");
				server.stop();
			}));
		}
		catch (IOException e)
		{
			Logging.errorPrint("Could not start the HTTP server", e);
			GracefulExit.exit(1);
		}
	}

	/**
	 * The port to listen on, from {@code -p} or {@code --port}. Zero lets the
	 * system pick one, which the startup line then reports.
	 */
	static int portFrom(String... args)
	{
		List<String> given = List.of(args);
		for (int i = 0; i < given.size() - 1; i++)
		{
			if ("-p".equals(given.get(i)) || "--port".equals(given.get(i)))
			{
				try
				{
					return Integer.parseInt(given.get(i + 1).trim());
				}
				catch (NumberFormatException e)
				{
					Logging.errorPrint("Not a port number: " + given.get(i + 1) + "; using " + DEFAULT_PORT);
					return DEFAULT_PORT;
				}
			}
		}
		return DEFAULT_PORT;
	}
}
