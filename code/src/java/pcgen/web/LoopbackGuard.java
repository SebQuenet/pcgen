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

import java.util.List;
import java.util.Locale;

/**
 * Keeps the server answering only to the machine it runs on.
 *
 * <p>
 * Listening on the loopback address is not enough on its own: a page on any web
 * site can make a browser send requests to 127.0.0.1, and DNS rebinding lets that
 * page reach a server that trusts whatever name it was asked for. Checking that
 * the request names the loopback address, in Host and in Origin, is what closes
 * that door.
 */
public final class LoopbackGuard
{
	private static final List<String> LOOPBACK_NAMES = List.of("127.0.0.1", "localhost", "[::1]");

	private final int port;

	public LoopbackGuard(int port)
	{
		this.port = port;
	}

	public boolean accepts(String hostHeader, String originHeader)
	{
		return namesLoopback(hostHeader) && (originHeader == null || originIsLoopback(originHeader));
	}

	private boolean namesLoopback(String hostHeader)
	{
		if (hostHeader == null)
		{
			return false;
		}
		String host = hostHeader.trim().toLowerCase(Locale.ROOT);
		for (String name : LOOPBACK_NAMES)
		{
			if (host.equals(name + ":" + port) || host.equals(name))
			{
				return true;
			}
		}
		return false;
	}

	private boolean originIsLoopback(String originHeader)
	{
		String origin = originHeader.trim().toLowerCase(Locale.ROOT);
		if ("null".equals(origin))
		{
			return false;
		}
		for (String name : LOOPBACK_NAMES)
		{
			if (origin.startsWith("http://" + name + ":") || origin.equals("http://" + name))
			{
				return true;
			}
		}
		return false;
	}
}
