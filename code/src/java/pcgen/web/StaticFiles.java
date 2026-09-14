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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The front end's own files, served from one directory.
 *
 * <p>
 * A request path is only ever resolved inside that directory: a path that
 * escapes it, by {@code ..} or by a leading slash, is refused rather than read.
 */
public final class StaticFiles
{
	private static final String INDEX = "index.html";
	private static final String OCTET_STREAM = "application/octet-stream";

	private static final Map<String, String> TYPE_BY_EXTENSION = Map.ofEntries(
		Map.entry("html", "text/html; charset=utf-8"),
		Map.entry("css", "text/css; charset=utf-8"),
		Map.entry("js", "text/javascript; charset=utf-8"),
		Map.entry("mjs", "text/javascript; charset=utf-8"),
		Map.entry("json", "application/json; charset=utf-8"),
		Map.entry("svg", "image/svg+xml"),
		Map.entry("png", "image/png"),
		Map.entry("jpg", "image/jpeg"),
		Map.entry("jpeg", "image/jpeg"),
		Map.entry("gif", "image/gif"),
		Map.entry("ico", "image/x-icon"),
		Map.entry("webp", "image/webp"),
		Map.entry("woff", "font/woff"),
		Map.entry("woff2", "font/woff2"),
		Map.entry("txt", "text/plain; charset=utf-8"));

	private final Path root;

	public StaticFiles(Path root)
	{
		this.root = root.toAbsolutePath().normalize();
	}

	public boolean exists()
	{
		return Files.isDirectory(root);
	}

	/** One file to serve, or nothing when the path names none inside the root. */
	public Optional<Served> at(String requestPath)
	{
		String wanted = requestPath.isEmpty() || "/".equals(requestPath) ? INDEX : requestPath;
		Path file = root.resolve(wanted.startsWith("/") ? wanted.substring(1) : wanted).normalize();
		if (!file.startsWith(root) || !Files.isRegularFile(file))
		{
			return Optional.empty();
		}
		try
		{
			return Optional.of(new Served(Files.readAllBytes(file), contentTypeOf(file)));
		}
		catch (IOException e)
		{
			return Optional.empty();
		}
	}

	/** A file's bytes and the type to serve them as. */
	public record Served(byte[] bytes, String contentType)
	{
	}

	private static String contentTypeOf(Path file)
	{
		String name = file.getFileName().toString();
		int dot = name.lastIndexOf('.');
		if (dot < 0)
		{
			return OCTET_STREAM;
		}
		return TYPE_BY_EXTENSION.getOrDefault(name.substring(dot + 1).toLowerCase(Locale.ROOT), OCTET_STREAM);
	}
}
