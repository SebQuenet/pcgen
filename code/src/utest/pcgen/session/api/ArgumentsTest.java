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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import pcgen.session.service.ServiceError;
import pcgen.session.service.ServiceResult;

/**
 * What the boundary does with the arguments a transport hands over.
 */
public class ArgumentsTest
{
	@Test
	public void readsAStringThatIsThere()
	{
		Arguments arguments = Arguments.of(Map.of("game_mode", "Pathfinder_RPG"));

		ServiceResult<?> result = arguments.decodeThen(
			() -> ServiceResult.success(arguments.requiredString("game_mode")));

		assertEquals("Pathfinder_RPG", assertInstanceOf(ServiceResult.Success.class, result).value());
	}

	@Test
	public void refusesACallWhoseRequiredStringIsMissing()
	{
		Arguments arguments = Arguments.of(Map.of());

		ServiceResult<?> result = arguments.decodeThen(
			() -> ServiceResult.success(arguments.requiredString("game_mode")));

		ServiceError error = assertInstanceOf(ServiceResult.Failure.class, result).error();
		assertEquals(new ServiceError.InvalidArgument("game_mode", "is required"), error);
	}

	@Test
	public void refusesACallWhoseRequiredStringIsBlank()
	{
		Arguments arguments = Arguments.of(Map.of("name", "   "));

		ServiceResult<?> result = arguments.decodeThen(
			() -> ServiceResult.success(arguments.requiredString("name")));

		assertInstanceOf(ServiceResult.Failure.class, result);
	}

	@Test
	public void stopsAtTheFirstUnreadableArgument()
	{
		Arguments arguments = Arguments.of(Map.of("campaigns", List.of("Core Rulebook")));

		ServiceResult<?> result = arguments.decodeThen(() -> ServiceResult.success(
			arguments.requiredString("game_mode") + arguments.requiredStringList("campaigns")));

		ServiceError error = assertInstanceOf(ServiceResult.Failure.class, result).error();
		assertEquals(new ServiceError.InvalidArgument("game_mode", "is required"), error);
	}

	@Test
	public void takesAWholeNumberWrittenAsText()
	{
		Arguments arguments = Arguments.of(Map.of("levels", "3"));

		assertEquals(3, arguments.optionalInt("levels", 1));
	}

	@Test
	public void refusesAListThatHoldsSomethingOtherThanStrings()
	{
		Arguments arguments = Arguments.of(Map.of("campaigns", List.of(1, 2)));

		ServiceResult<?> result = arguments.decodeThen(
			() -> ServiceResult.success(arguments.requiredStringList("campaigns")));

		ServiceError error = assertInstanceOf(ServiceResult.Failure.class, result).error();
		assertEquals(new ServiceError.InvalidArgument("campaigns", "must be an array of strings"), error);
	}

	@Test
	public void readsEachEntryOfABatchInItsOwnRight()
	{
		Arguments arguments = Arguments.of(Map.of("abilities",
			List.of(Map.of("ability_key", "Power Attack"), Map.of("ability_key", "Cleave"))));

		List<Arguments> entries = arguments.requiredEntryList("abilities");

		assertEquals(2, entries.size());
		assertEquals("Power Attack", entries.get(0).requiredString("ability_key"));
		assertEquals("Cleave", entries.get(1).requiredString("ability_key"));
	}

	@Test
	public void fallsBackWhenAnOptionalArgumentIsAbsent()
	{
		Arguments arguments = Arguments.of(Map.of());

		assertEquals("markdown", arguments.optionalString("format", "markdown"));
		assertEquals(1, arguments.optionalInt("levels", 1));
		assertTrue(arguments.optionalStringList("choices").isEmpty());
		assertTrue(arguments.optionalBoolean("free", true));
	}
}
