/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package pcgen.gui2.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;

import pcgen.core.spell.Spell;
import pcgen.system.LanguageBundle;

import org.junit.jupiter.api.Test;

class TempBonusFacadeImplTest
{
	@Test
	void appliedBonusShowsItsTranslatedNameWithoutChangingItsKey()
	{
		Spell spell = new Spell();
		spell.setName("Bless");
		spell.setLocalizedName("Bénédiction");
		TempBonusFacadeImpl bonus = new TempBonusFacadeImpl(spell, null, "Bless [NONE]");

		assertEquals("Bénédiction [" + LanguageBundle.getString("in_none") + "]", bonus.toString());
		assertEquals("Bless", bonus.getKeyName());
	}
}
