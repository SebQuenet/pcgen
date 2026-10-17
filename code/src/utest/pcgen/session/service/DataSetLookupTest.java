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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import pcgen.core.spell.Spell;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Behaviour of matching a name a caller used against a piece of loaded data.
 */
class DataSetLookupTest
{
	private Spell magicMissile;

	@BeforeEach
	void setUp()
	{
		magicMissile = new Spell();
		magicMissile.setName("Magic Missile");
		magicMissile.setLocalizedName("Projectile magique");
	}

	@Test
	void answersToItsKey()
	{
		assertTrue(DataSetLookup.answersTo(magicMissile, "magic missile"));
	}

	@Test
	void answersToItsTranslatedName()
	{
		assertTrue(DataSetLookup.answersTo(magicMissile, "projectile MAGIQUE"));
	}

	@Test
	void doesNotAnswerToAnotherName()
	{
		assertFalse(DataSetLookup.answersTo(magicMissile, "Fireball"));
	}

	@Test
	void showsTheTranslatedName()
	{
		assertEquals("Projectile magique", DataSetLookup.shownName(magicMissile));
	}
}
