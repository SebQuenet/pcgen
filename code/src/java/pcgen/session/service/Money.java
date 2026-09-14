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

import java.math.BigDecimal;

import pcgen.facade.util.ReferenceFacade;

/**
 * Reads an amount of money out of a character.
 *
 * <p>
 * PCGen declares funds and wealth as {@code ReferenceFacade<BigDecimal>} but a
 * freshly created character holds an {@code Integer} there. Reading one straight
 * into a BigDecimal therefore fails at runtime on a character nobody has spent
 * anything for yet, which is every new character. The reference is taken as a
 * wildcard so that no cast is compiled in, and whatever number is behind it is
 * converted here.
 */
public final class Money
{
	private Money()
	{
	}

	public static BigDecimal amountOf(ReferenceFacade<?> reference)
	{
		Object amount = reference == null ? null : reference.get();
		if (amount instanceof BigDecimal exact)
		{
			return exact;
		}
		if (amount instanceof Number number)
		{
			return BigDecimal.valueOf(number.doubleValue());
		}
		return BigDecimal.ZERO;
	}
}
