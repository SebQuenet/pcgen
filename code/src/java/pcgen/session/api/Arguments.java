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
import java.util.function.Supplier;

import pcgen.session.service.ServiceError;
import pcgen.session.service.ServiceResult;

/**
 * The arguments a transport handed over, and the only place they become typed
 * values. A missing or ill-shaped argument stops the call here, before any
 * service sees it.
 *
 * <p>
 * Read them inside {@link #decodeThen}: a {@code requiredX} reader that cannot
 * satisfy the request abandons the call from where it stands, so the service is
 * never reached with a null in its hands.
 */
public final class Arguments
{
	private final Map<String, Object> raw;

	private Arguments(Map<String, Object> raw)
	{
		this.raw = raw == null ? Map.of() : raw;
	}

	public static Arguments of(Map<String, Object> raw)
	{
		return new Arguments(raw);
	}

	/**
	 * Run a call whose arguments are read from this object, turning the first
	 * unreadable argument into a failed result.
	 */
	public ServiceResult<?> decodeThen(Supplier<ServiceResult<?>> call)
	{
		try
		{
			return call.get();
		}
		catch (UnreadableArgument e)
		{
			return ServiceResult.failure(e.error());
		}
	}

	public boolean has(String field)
	{
		return raw.get(field) != null;
	}

	public String requiredString(String field)
	{
		Object value = raw.get(field);
		if (value instanceof String text && !text.isBlank())
		{
			return text;
		}
		throw unreadable(field, value == null ? "is required" : "must be a non-empty string");
	}

	public String optionalString(String field, String fallback)
	{
		Object value = raw.get(field);
		return value instanceof String text && !text.isBlank() ? text : fallback;
	}

	public int requiredInt(String field)
	{
		Integer parsed = asInt(raw.get(field));
		if (parsed == null)
		{
			throw unreadable(field, raw.get(field) == null ? "is required" : "must be a whole number");
		}
		return parsed;
	}

	public int optionalInt(String field, int fallback)
	{
		Integer parsed = asInt(raw.get(field));
		return parsed == null ? fallback : parsed;
	}

	public boolean optionalBoolean(String field, boolean fallback)
	{
		Object value = raw.get(field);
		if (value instanceof Boolean flag)
		{
			return flag;
		}
		if (value instanceof String text)
		{
			return Boolean.parseBoolean(text);
		}
		return fallback;
	}

	public List<String> requiredStringList(String field)
	{
		Object value = raw.get(field);
		if (value == null)
		{
			throw unreadable(field, "is required");
		}
		if (!(value instanceof List<?> items))
		{
			throw unreadable(field, "must be an array of strings");
		}
		List<String> texts = new ArrayList<>(items.size());
		for (Object item : items)
		{
			if (!(item instanceof String text))
			{
				throw unreadable(field, "must be an array of strings");
			}
			texts.add(text);
		}
		return List.copyOf(texts);
	}

	public List<String> optionalStringList(String field)
	{
		Object value = raw.get(field);
		if (!(value instanceof List<?> items))
		{
			return List.of();
		}
		List<String> texts = new ArrayList<>(items.size());
		for (Object item : items)
		{
			if (item instanceof String text)
			{
				texts.add(text);
			}
		}
		return List.copyOf(texts);
	}

	/** An object whose values are whole numbers, such as a set of ability scores. */
	public Map<String, Integer> requiredIntMap(String field)
	{
		Object value = raw.get(field);
		if (value == null)
		{
			throw unreadable(field, "is required");
		}
		if (!(value instanceof Map<?, ?> entries))
		{
			throw unreadable(field, "must be an object of whole numbers");
		}
		Map<String, Integer> numbers = new LinkedHashMap<>();
		for (Map.Entry<?, ?> entry : entries.entrySet())
		{
			Integer number = asInt(entry.getValue());
			if (!(entry.getKey() instanceof String key) || number == null)
			{
				throw unreadable(field, "must be an object of whole numbers");
			}
			numbers.put(key, number);
		}
		return Map.copyOf(numbers);
	}

	/**
	 * The entries of an array of objects, each readable in its own right. The batch
	 * operations use it: their payload is one argument set per item.
	 */
	public List<Arguments> requiredEntryList(String field)
	{
		Object value = raw.get(field);
		if (value == null)
		{
			throw unreadable(field, "is required");
		}
		if (!(value instanceof List<?> items))
		{
			throw unreadable(field, "must be an array of objects");
		}
		List<Arguments> entries = new ArrayList<>(items.size());
		for (Object item : items)
		{
			if (!(item instanceof Map<?, ?> fields))
			{
				throw unreadable(field, "must be an array of objects");
			}
			Map<String, Object> typed = new LinkedHashMap<>();
			for (Map.Entry<?, ?> entry : fields.entrySet())
			{
				if (entry.getKey() instanceof String key)
				{
					typed.put(key, entry.getValue());
				}
			}
			entries.add(new Arguments(typed));
		}
		return List.copyOf(entries);
	}

	private static Integer asInt(Object value)
	{
		if (value instanceof Number number)
		{
			return number.intValue();
		}
		if (value instanceof String text)
		{
			try
			{
				return Integer.valueOf(text.trim());
			}
			catch (NumberFormatException e)
			{
				return null;
			}
		}
		return null;
	}

	private static UnreadableArgument unreadable(String field, String reason)
	{
		return new UnreadableArgument(new ServiceError.InvalidArgument(field, reason));
	}

	/**
	 * Thrown by a reader that cannot satisfy the request, and caught by
	 * {@link #decodeThen}. It never leaves this package.
	 */
	private static final class UnreadableArgument extends RuntimeException
	{
		private final ServiceError error;

		private UnreadableArgument(ServiceError error)
		{
			super(error.message(), null, false, false);
			this.error = error;
		}

		private ServiceError error()
		{
			return error;
		}
	}
}
