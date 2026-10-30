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
package pcgen.core.gametable;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.ref.WeakReference;

import pcgen.core.PlayerCharacter;
import pcgen.core.conjurations.BestiarySources;
import pcgen.session.HeadlessUIDelegate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * A monster built to be read and dropped is not kept alive by PCGen's shared caches.
 */
class MonsterReleaseTest
{
	private static final int COLLECTIONS = 10;

	private static MonsterForge forge;

	@BeforeAll
	static void loadTheBestiary()
	{
		forge = new MonsterForge(BestiarySources.campaigns(), new HeadlessUIDelegate());
	}

	private static WeakReference<PlayerCharacter> builtAndReleased()
	{
		PlayerCharacter wolf = ((ForgedMonster.Forged) forge.forge(MonsterRecipe.of(new MonsterSource.BestiaryRace("Wolf"))))
			.character();
		MonsterForge.release(wolf);
		return new WeakReference<>(wolf);
	}

	@Test
	void aReleasedMonsterIsCollected() throws InterruptedException
	{
		WeakReference<PlayerCharacter> wolf = builtAndReleased();

		for (int collection = 0; collection < COLLECTIONS && wolf.get() != null; collection++)
		{
			System.gc();
			Thread.sleep(50);
		}

		assertNull(wolf.get());
	}
}
