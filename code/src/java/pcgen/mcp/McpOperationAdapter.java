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
package pcgen.mcp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import pcgen.session.api.Arguments;
import pcgen.session.api.JsonPayload;
import pcgen.session.api.Operation;
import pcgen.session.api.OperationRegistry;
import pcgen.session.service.ServiceResult;

/**
 * Dresses the registry's operations as MCP tools. Everything MCP-specific about
 * an operation lives here: the tool wrapper, and the fact that the protocol has
 * one way of saying "that did not work".
 */
public final class McpOperationAdapter
{
	private McpOperationAdapter()
	{
	}

	public static List<SyncToolSpecification> toolsFrom(OperationRegistry registry)
	{
		List<SyncToolSpecification> tools = new ArrayList<>();
		for (Operation operation : registry.all())
		{
			tools.add(asTool(operation));
		}
		return List.copyOf(tools);
	}

	private static SyncToolSpecification asTool(Operation operation)
	{
		return new SyncToolSpecification(
			new Tool(operation.name(), operation.description(), operation.inputSchema()),
			(exchange, args) -> callToolResult(operation, args));
	}

	private static CallToolResult callToolResult(Operation operation, Map<String, Object> args)
	{
		ServiceResult<?> result = operation.call(Arguments.of(args));
		return switch (result)
		{
			case ServiceResult.Success<?> success -> new CallToolResult(JsonPayload.write(success.value()), false);
			case ServiceResult.Failure<?> failure -> new CallToolResult(failure.error().message(), true);
		};
	}
}
