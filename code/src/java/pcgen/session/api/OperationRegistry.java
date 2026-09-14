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
package pcgen.session.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import pcgen.session.PcgenSession;
import pcgen.session.service.CharacterBuildService;
import pcgen.session.service.CharacterLifecycleService;
import pcgen.session.service.SourceService;

/**
 * Everything a caller can ask PCGen to do, gathered once from the services and
 * handed to whichever transport is serving: the MCP server, the HTTP server, or
 * a test.
 */
public final class OperationRegistry
{
	private final Map<String, Operation> byName;
	private final List<Operation> ordered;

	private OperationRegistry(List<Operation> operations)
	{
		Map<String, Operation> indexed = new LinkedHashMap<>();
		for (Operation operation : operations)
		{
			Operation clash = indexed.put(operation.name(), operation);
			if (clash != null)
			{
				throw new IllegalStateException("Two operations answer to " + operation.name());
			}
		}
		this.byName = Map.copyOf(indexed);
		this.ordered = List.copyOf(operations);
	}

	public static OperationRegistry forSession(PcgenSession session)
	{
		List<Operation> operations = new ArrayList<>();
		operations.addAll(SourceOperations.of(new SourceService(session)));
		operations.addAll(CharacterOperations.of(
			new CharacterLifecycleService(session), new CharacterBuildService(session)));
		return new OperationRegistry(operations);
	}

	public Optional<Operation> find(String name)
	{
		return Optional.ofNullable(byName.get(name));
	}

	public List<Operation> all()
	{
		return ordered;
	}
}
