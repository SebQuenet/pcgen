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

/**
 * Why a service call did not do what was asked. Every way an operation can fail
 * is one of these, so a caller that switches over them has covered the ground.
 */
public sealed interface ServiceError
{
	/** What to tell a human, and what the MCP transport sends back as its error text. */
	String message();

	/** No campaign data is loaded yet, so there is nothing to build a character from. */
	record NoSourcesLoaded() implements ServiceError
	{
		@Override
		public String message()
		{
			return "No sources loaded. Call load_sources first.";
		}
	}

	/** The session holds no character under that id. */
	record CharacterNotFound(String characterId) implements ServiceError
	{
		@Override
		public String message()
		{
			return "Character not found: " + characterId;
		}
	}

	/** The loaded data holds no entry of that kind under that key. */
	record EntryNotFound(String kind, String key) implements ServiceError
	{
		@Override
		public String message()
		{
			return kind + " not found: " + key;
		}
	}

	/** An argument is missing, of the wrong type, or out of range. */
	record InvalidArgument(String field, String reason) implements ServiceError
	{
		@Override
		public String message()
		{
			return "Invalid argument '" + field + "': " + reason;
		}
	}

	/** The arguments are well formed but the rules refuse the action. */
	record NotAllowed(String reason) implements ServiceError
	{
		@Override
		public String message()
		{
			return reason;
		}
	}

	/** Loading or writing game data failed. */
	record DataFailure(String detail) implements ServiceError
	{
		@Override
		public String message()
		{
			return detail;
		}
	}
}
