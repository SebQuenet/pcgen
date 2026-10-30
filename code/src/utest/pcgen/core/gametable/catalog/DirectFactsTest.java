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
package pcgen.core.gametable.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import pcgen.cdom.enumeration.FactKey;

import org.junit.jupiter.api.Test;

/**
 * What a bestiary entry tells without its creature being built.
 */
class DirectFactsTest
{
	@Test
	void aCharacterFileIsListedBeforeAnySourceDefinesTheRaceFacts()
	{
		FactKey.clearConstants();

		assertEquals(new FieldValue.Text("Ogre"), DirectFacts.ofFile(Path.of("/npcs/Ogre.pcg")).get(CatalogField.NAME));
	}
}
