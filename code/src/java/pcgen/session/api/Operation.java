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

import java.util.function.Function;

import pcgen.session.service.ServiceResult;

/**
 * One thing a caller can ask PCGen to do, described once and offered to every
 * transport: its name, the JSON schema of its arguments, and the call itself.
 *
 * @param name what callers ask for, in snake_case
 * @param description one line, shown to an agent choosing between operations
 * @param inputSchema JSON Schema of the argument object
 * @param needsExclusiveAccess whether the call touches PCGen's shared state and must
 *     therefore run alone; false only for calls that merely read or complete a pending
 *     choice, which must stay reachable while another call is blocked waiting on one
 * @param invoke the call, which decodes its own arguments and returns a typed result
 */
public record Operation(
	String name,
	String description,
	String inputSchema,
	boolean needsExclusiveAccess,
	Function<Arguments, ServiceResult<?>> invoke)
{
	/** An operation that touches character state, which is nearly all of them. */
	public static Operation exclusive(String name, String description, String inputSchema,
		Function<Arguments, ServiceResult<?>> invoke)
	{
		return new Operation(name, description, inputSchema, true, invoke);
	}

	/** An operation safe to run while another one is in flight. */
	public static Operation concurrent(String name, String description, String inputSchema,
		Function<Arguments, ServiceResult<?>> invoke)
	{
		return new Operation(name, description, inputSchema, false, invoke);
	}

	public ServiceResult<?> call(Arguments arguments)
	{
		return invoke.apply(arguments);
	}
}
