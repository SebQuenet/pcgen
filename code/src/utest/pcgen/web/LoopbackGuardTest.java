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
package pcgen.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Which requests the server agrees to answer.
 */
public class LoopbackGuardTest
{
	private static final int PORT = 8420;

	private final LoopbackGuard guard = new LoopbackGuard(PORT);

	@Test
	public void acceptsTheLoopbackAddressOnItsOwnPort()
	{
		assertTrue(guard.accepts("127.0.0.1:" + PORT, null));
		assertTrue(guard.accepts("localhost:" + PORT, null));
		assertTrue(guard.accepts("[::1]:" + PORT, null));
	}

	@Test
	public void refusesAHostItIsNotServing()
	{
		assertFalse(guard.accepts("pcgen.example.com", null));
		assertFalse(guard.accepts("127.0.0.1:9999", null));
		assertFalse(guard.accepts(null, null));
	}

	@Test
	public void refusesAPageServedFromSomewhereElse()
	{
		assertFalse(guard.accepts("127.0.0.1:" + PORT, "https://evil.example.com"));
		assertFalse(guard.accepts("127.0.0.1:" + PORT, "null"));
	}

	@Test
	public void acceptsAPageServedFromTheSameMachine()
	{
		assertTrue(guard.accepts("127.0.0.1:" + PORT, "http://localhost:5173"));
		assertTrue(guard.accepts("localhost:" + PORT, "http://127.0.0.1:" + PORT));
	}
}
