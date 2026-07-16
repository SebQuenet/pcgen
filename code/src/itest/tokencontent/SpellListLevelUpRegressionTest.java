/*
 * Copyright (c) 2026.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package tokencontent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import pcgen.cdom.base.CDOMList;
import pcgen.cdom.enumeration.ListKey;
import pcgen.cdom.enumeration.Type;
import pcgen.core.PCClass;
import pcgen.core.spell.Spell;
import pcgen.gui2.facade.MockUIDelegate;
import pcgen.rules.persistence.token.CDOMToken;
import pcgen.rules.persistence.token.ParseResult;
import pcgen.util.chooser.ChooserFactory;
import plugin.lsttokens.spell.ClassesToken;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tokenmodel.testsupport.AbstractTokenModelTest;
import util.TestURI;

/**
 * Regression test for the headless/console level-up path (used by the MCP
 * server), which increments a class level with prerequisites bypassed. Prior to
 * the fix in {@link PCClass#addLevel}, that path left the class level pre-set to
 * {@code newLevel} before {@code setLevel(newLevel)} ran, so the
 * {@code curLevel == 0} guard failed and the first-level-only
 * {@code setSpellLists()} call was skipped. The result was a spellcasting class
 * with no associated spell list (no available or castable spells).
 */
public class SpellListLevelUpRegressionTest extends AbstractTokenModelTest
{

	private static final ClassesToken CLASSES_TOKEN = new ClassesToken();
	private Spell classSpell;
	private PCClass caster;

	@Override
	@BeforeEach
	protected void setUp() throws Exception
	{
		super.setUp();
		classSpell = context.getReferenceContext().constructCDOMObject(Spell.class, "MySpell");
		caster = context.getReferenceContext().constructCDOMObject(PCClass.class, "Caster");
		caster.addToListFor(ListKey.TYPE, Type.MONSTER);
		ChooserFactory.setDelegate(new MockUIDelegate());
	}

	@Test
	public void testSpellListAssociatedWhenLevelUpBypassesPrereqs()
	{
		ParseResult result = CLASSES_TOKEN.parseToken(context, classSpell, "Caster=1");
		if (result != ParseResult.SUCCESS)
		{
			result.printMessages(TestURI.getURI());
			fail("Test Setup Failed");
		}
		finishLoad();

		// Headless/console/MCP level-up: prerequisites bypassed.
		pc.incrementClassLevel(1, caster, false, true);

		PCClass added = pc.getClassKeyed("Caster");
		assertNotNull(added, "Class should have been added");
		List<? extends CDOMList<Spell>> spellLists = pc.getSpellLists(added);
		assertFalse(spellLists.isEmpty(),
			"Spell list must be associated after a prereq-bypassing level-up");
		assertTrue(pc.getAllSpellsInLists(spellLists).contains(classSpell),
			"The class spell must be reachable through the associated spell list");
	}

	@Override
	public CDOMToken<?> getToken()
	{
		return CLASSES_TOKEN;
	}
}
