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
package pcgen.core.tactics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Behaviour of what a session has used up so far.
 */
class TacticalSessionStateTest
{

	@Test
	void anUntouchedSessionHasNoDamageAndNothingSpent()
	{
		TacticalSessionState state = TacticalSessionState.untouched();

		assertEquals(0, state.damageTaken());
		assertTrue(state.resourcesSpent().isEmpty());
	}

	@Test
	void keepsWhatItWasGiven()
	{
		TacticalSessionState state = new TacticalSessionState(19, Map.of("Mythic power", 4));

		assertEquals(19, state.damageTaken());
		assertEquals(4, state.spent("Mythic power"));
	}

	@Test
	void reportsNothingSpentOnAResourceItDoesNotKnow()
	{
		assertEquals(0, TacticalSessionState.untouched().spent("Mythic power"));
	}

	@Test
	void refusesNegativeDamage()
	{
		assertThrows(IllegalArgumentException.class, () -> new TacticalSessionState(-1, Map.of()));
	}

	@Test
	void refusesANegativeCount()
	{
		assertThrows(IllegalArgumentException.class, () -> new TacticalSessionState(0, Map.of("Channel", -1)));
	}

	@Test
	void cannotBeChangedThroughTheMapPassedIn()
	{
		Map<String, Integer> mutable = new HashMap<>(Map.of("Channel", 2));
		TacticalSessionState state = new TacticalSessionState(0, mutable);

		mutable.clear();

		assertEquals(2, state.spent("Channel"));
	}
}
