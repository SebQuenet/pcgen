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
package pcgen.system;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Game terms that are not loaded objects, such as race types and vision types, read their text from the bundle.
 */
class LanguageBundleTermTest
{
	@Test
	void givesTheBundleTextOfAKnownTerm()
	{
		String humanoid = LanguageBundle.getTerm("in_raceType_", "Humanoid");

		assertEquals(LanguageBundle.getString("in_raceType_humanoid"), humanoid);
	}

	@Test
	void ignoresCaseSpacesAndPunctuationOfTheTerm()
	{
		String lowLight = LanguageBundle.getTerm("in_vision_", "low-light vision");

		assertEquals(LanguageBundle.getString("in_vision_lowlightvision"), lowLight);
	}

	@Test
	void givesAnUnknownTermUnchanged()
	{
		String unknown = LanguageBundle.getTerm("in_raceType_", "Eidolon of Doom");

		assertEquals("Eidolon of Doom", unknown);
	}
}
