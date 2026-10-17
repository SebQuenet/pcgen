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

import pcgen.AbstractCharacterTestCase;
import pcgen.cdom.enumeration.StringKey;
import pcgen.core.Globals;
import pcgen.core.PCCheck;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import plugin.exporttokens.deprecated.CheckToken;

/**
 * The CHECK export token gives a save's key apart from its displayed name, which a translation changes.
 */
class CheckTokenTest extends AbstractCharacterTestCase
{
	@BeforeEach
	@Override
	public void setUp() throws Exception
	{
		super.setUp();
		PCCheck reflex = new PCCheck();
		reflex.setName("Réflexes");
		reflex.setKeyName("Reflex");
		reflex.put(StringKey.SORT_KEY, "1");
		Globals.getContext().getReferenceContext().importObject(reflex);
	}

	@Test
	void givesTheKeyOfTheSave()
	{
		String key = new CheckToken().getToken("CHECK.0.KEY", getCharacter(), null);

		assertEquals("Reflex", key);
	}

	@Test
	void givesTheDisplayedNameOfTheSave()
	{
		String name = new CheckToken().getToken("CHECK.0.NAME", getCharacter(), null);

		assertEquals("Réflexes", name);
	}
}
