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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * How a service's answer is written out. Both transports use this one mapper, so
 * a field is spelled the same whether it reaches an agent over stdio or a browser
 * over HTTP: snake_case, matching the argument names callers send in.
 */
public final class JsonPayload
{
	private static final ObjectMapper MAPPER = new ObjectMapper()
		.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
		.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

	private JsonPayload()
	{
	}

	/**
	 * The value as JSON. A value Jackson cannot describe falls back to its own
	 * printed form rather than failing the call that produced it.
	 */
	public static String write(Object value)
	{
		try
		{
			return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
		}
		catch (JsonProcessingException e)
		{
			return String.valueOf(value);
		}
	}
}
