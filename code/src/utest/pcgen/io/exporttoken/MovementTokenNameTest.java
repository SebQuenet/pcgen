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
package pcgen.io.exporttoken;

import static org.junit.jupiter.api.Assertions.assertEquals;

import pcgen.system.LanguageBundle;

import org.junit.jupiter.api.Test;

/**
 * Behaviour of the name shown for a movement type.
 */
class MovementTokenNameTest
{
	@Test
	void showsAKnownMovementTypeInTheConfiguredLanguage()
	{
		assertEquals(LanguageBundle.getString("in_move_fly"), MovementToken.getLocalizedMovementName("Fly"));
	}

	@Test
	void showsAnUnknownMovementTypeAsWritten()
	{
		assertEquals("Glide", MovementToken.getLocalizedMovementName("Glide"));
	}
}
