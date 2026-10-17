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
package pcgen.core.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import pcgen.AbstractCharacterTestCase;
import pcgen.cdom.enumeration.ObjectKey;
import pcgen.cdom.enumeration.StringKey;
import pcgen.cdom.reference.CDOMDirectSingleRef;
import pcgen.core.Skill;

import org.junit.jupiter.api.Test;

/**
 * A skill's key ability is shown with the stat's abbreviation in the configured language.
 */
class SkillKeyStatDisplayTest extends AbstractCharacterTestCase
{
	@Test
	void showsTheTranslatedAbbreviationOfTheKeyStat()
	{
		Skill climb = new Skill();
		climb.setName("Climb");
		climb.put(ObjectKey.KEY_STAT, CDOMDirectSingleRef.getRef(str));
		str.put(StringKey.LOCALIZED_ABB, "FOR");

		String keyStat = SkillInfoUtilities.getKeyStatFromStats(getCharacter(), climb);

		assertEquals("FOR", keyStat);
	}
}
