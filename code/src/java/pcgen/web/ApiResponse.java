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

import pcgen.session.service.ServiceError;

/**
 * The shape every HTTP answer takes, so a caller reads one envelope whether the
 * call worked or not.
 */
public final class ApiResponse
{
	private ApiResponse()
	{
	}

	public record Envelope(boolean ok, Object data, Failure error)
	{
	}

	/** Which error it was, and what to tell the person reading. */
	public record Failure(String kind, String message)
	{
	}

	public static Envelope of(Object data)
	{
		return new Envelope(true, data, null);
	}

	public static Envelope of(ServiceError error)
	{
		return new Envelope(false, null, new Failure(error.getClass().getSimpleName(), error.message()));
	}

	/**
	 * The status that says what went wrong: what the caller sent, what it asked
	 * for, what state the session is in, or what the rules refuse.
	 */
	public static int statusFor(ServiceError error)
	{
		return switch (error)
		{
			case ServiceError.InvalidArgument ignored -> 400;
			case ServiceError.CharacterNotFound ignored -> 404;
			case ServiceError.EntryNotFound ignored -> 404;
			case ServiceError.NoSourcesLoaded ignored -> 409;
			case ServiceError.NotAllowed ignored -> 422;
			case ServiceError.DataFailure ignored -> 500;
		};
	}
}
