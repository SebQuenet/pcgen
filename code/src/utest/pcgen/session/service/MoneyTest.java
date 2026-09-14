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

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import pcgen.facade.util.AbstractReferenceFacade;
import pcgen.facade.util.ReferenceFacade;

/**
 * What reading a character's money has to survive.
 */
public class MoneyTest
{
	@Test
	public void readsAnAmountThatIsAlreadyExact()
	{
		assertEquals(new BigDecimal("12.50"), Money.amountOf(holding(new BigDecimal("12.50"))));
	}

	/**
	 * A character nobody has spent anything for holds an Integer behind a reference
	 * PCGen declares as BigDecimal, which is every character on the day it is made.
	 */
	@Test
	public void readsAnAmountPcgenLeftAsAWholeNumber()
	{
		assertEquals(BigDecimal.valueOf(150.0), Money.amountOf(holding(150)));
	}

	@Test
	public void readsNothingAsNothing()
	{
		assertEquals(BigDecimal.ZERO, Money.amountOf(holding(null)));
		assertEquals(BigDecimal.ZERO, Money.amountOf(null));
	}

	private static ReferenceFacade<?> holding(Object amount)
	{
		return new AbstractReferenceFacade<>()
		{
			@Override
			public Object get()
			{
				return amount;
			}
		};
	}
}
