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
import static org.junit.jupiter.api.Assertions.assertTrue;

import pcgen.AbstractCharacterTestCase;
import pcgen.cdom.base.FormulaFactory;
import pcgen.cdom.enumeration.VariableKey;
import pcgen.core.PCTemplate;
import pcgen.core.PlayerCharacter;

import org.junit.jupiter.api.Test;

/**
 * What happens to a tactical plan a caller sends in: it is stored as written, and
 * the names a plan may point at are the character's own.
 */
public class TacticalSheetServiceTest extends AbstractCharacterTestCase
{

	private static final String A_PLAN = """
		## Before the fight
		resource: Mythic power | 11 | immediate
		attack: Spear +1 | +13/+8 | 1d8+4 | 20/x3
		""";

	@Test
	public void storesAPlanExactlyAsItWasSent()
	{
		PlayerCharacter character = getCharacter();

		character.setTacticalPlan(A_PLAN);

		assertEquals(A_PLAN, character.getDisplay().getTacticalPlan().orElseThrow());
	}

	@Test
	public void offersTheNamesOfTheVariablesTheCharacterDefines()
	{
		PlayerCharacter character = getCharacter();
		PCTemplate template = new PCTemplate();
		template.setName("Channeller");
		template.put(VariableKey.getConstant("ChannelUses"), FormulaFactory.getFormulaFor(7));
		character.addTemplate(template);

		assertTrue(character.getVariableNames().contains("ChannelUses"),
			"Expected ChannelUses among " + character.getVariableNames());
	}
}
