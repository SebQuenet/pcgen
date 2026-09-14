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

import java.util.function.Function;

/**
 * What a service call gives back: either the value that was asked for, or the
 * reason it could not be produced. Failure is part of the signature, so no
 * service throws across a layer boundary.
 *
 * @param <T> the type of the value a successful call produces
 */
public sealed interface ServiceResult<T>
{
	record Success<T>(T value) implements ServiceResult<T>
	{
	}

	record Failure<T>(ServiceError error) implements ServiceResult<T>
	{
	}

	static <T> ServiceResult<T> success(T value)
	{
		return new Success<>(value);
	}

	static <T> ServiceResult<T> failure(ServiceError error)
	{
		return new Failure<>(error);
	}

	/**
	 * Carry a failure over to another value type, so one call's error can be
	 * returned by a caller that produces something else.
	 */
	default <U> ServiceResult<U> mapValue(Function<T, U> mapping)
	{
		return switch (this)
		{
			case Success<T> success -> new Success<>(mapping.apply(success.value()));
			case Failure<T> failure -> new Failure<>(failure.error());
		};
	}
}
