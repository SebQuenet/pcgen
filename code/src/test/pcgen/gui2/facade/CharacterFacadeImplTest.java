/*
 * Copyright James Dempsey, 2013
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
package pcgen.gui2.facade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;

import pcgen.AbstractCharacterTestCase;
import pcgen.cdom.enumeration.ListKey;
import pcgen.cdom.enumeration.ObjectKey;
import pcgen.core.Ability;
import pcgen.core.AbilityCategory;
import pcgen.core.Globals;
import pcgen.core.PlayerCharacter;
import pcgen.core.SettingsHandler;
import pcgen.core.bonus.Bonus;
import pcgen.core.character.EquipSet;
import pcgen.facade.core.AbilityFacade;
import pcgen.facade.util.event.ListEvent;
import pcgen.facade.util.event.ListListener;
import pcgen.rules.context.LoadContext;
import pcgen.util.TestHelper;
import pcgen.util.enumeration.Visibility;
import plugin.lsttokens.testsupport.BuildUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The Class {@code CharacterFacadeImplTest} verifies the behaviour of
 * CharacterFacadeImpl.
 *
 * <br/>
 * 
 */

public class CharacterFacadeImplTest extends AbstractCharacterTestCase
{

	private MockDataSetFacade dataset;
	private MockUIDelegate uiDelegate;

	/**
	 * Check the default equipment set created by CharacterFacadeImpl. 
	 */
	@Test
	public void testDefaultEquipSet()
	{
		PlayerCharacter pc = new PlayerCharacter();
		CharacterFacadeImpl charFacade =
				new CharacterFacadeImpl(pc, uiDelegate, dataset);
		assertNotNull("Unable to create CharacterFacadeImpl", charFacade);
		EquipSet defaultEquipSet =
				pc.getEquipSetByIdPath(EquipSet.DEFAULT_SET_PATH);
		assertNotNull("Unable to find default equip set", defaultEquipSet);
		assertEquals("Incorrect id of the default equip set",
			EquipSet.DEFAULT_SET_PATH, defaultEquipSet.getIdPath());
	}

	/**
	 * An ability that opens the pool of a category shown only when it has a
	 * pool makes that category appear at once, as the mythic path does for its
	 * path abilities.
	 */
	@Test
	public void testAddAbilityShowsTheCategoryItOpens()
	{
		LoadContext context = Globals.getContext();
		AbilityCategory gatedCategory =
				context.getReferenceContext().constructCDOMObject(AbilityCategory.class, "Gated Pool");
		gatedCategory.setVisible(Visibility.QUALIFY);
		dataset.addAbilityCategory(gatedCategory);
		Ability poolOpener = TestHelper.makeAbility("Pool Opener", BuildUtilities.getFeatCat(), "General");
		poolOpener.put(ObjectKey.SELECTION_COST, BigDecimal.ZERO);
		poolOpener.addToListFor(ListKey.BONUS, Bonus.newBonus(context, "ABILITYPOOL|Gated Pool|1"));
		CharacterFacadeImpl charFacade = new CharacterFacadeImpl(getCharacter(), uiDelegate, dataset);
		charFacade.getAbilities(BuildUtilities.getFeatCat()).addListListener(new ReadingListener(charFacade));

		charFacade.addAbility(BuildUtilities.getFeatCat(), poolOpener);

		assertTrue("The category opened by the ability should be shown",
			charFacade.getActiveAbilityCategories().containsElement(gatedCategory));
	}

	/**
	 * Reads the character back when its abilities change, as the ability tab does.
	 */
	private static final class ReadingListener implements ListListener<AbilityFacade>
	{
		private final CharacterFacadeImpl charFacade;

		private ReadingListener(CharacterFacadeImpl charFacade)
		{
			this.charFacade = charFacade;
		}

		@Override
		public void elementAdded(ListEvent<AbilityFacade> e)
		{
			charFacade.getActiveAbilityCategories();
		}

		@Override
		public void elementRemoved(ListEvent<AbilityFacade> e)
		{
			charFacade.getActiveAbilityCategories();
		}

		@Override
		public void elementsChanged(ListEvent<AbilityFacade> e)
		{
			charFacade.getActiveAbilityCategories();
		}

		@Override
		public void elementModified(ListEvent<AbilityFacade> e)
		{
			charFacade.getActiveAbilityCategories();
		}
	}

	@BeforeEach
	@Override
	public void setUp() throws Exception
	{
		super.setUp();
		dataset = new MockDataSetFacade(SettingsHandler.getGameAsProperty().get());
		dataset.addAbilityCategory(BuildUtilities.getFeatCat());
		uiDelegate = new MockUIDelegate();
	}

}
