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
package pcgen.io.tactics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import pcgen.AbstractCharacterTestCase;
import pcgen.cdom.enumeration.VariableKey;
import pcgen.cdom.base.FormulaFactory;
import pcgen.core.Equipment;
import pcgen.core.Globals;
import pcgen.core.PCTemplate;
import pcgen.core.PlayerCharacter;
import pcgen.core.tactics.SpellSource;
import pcgen.util.TestHelper;

import org.junit.jupiter.api.Test;

/**
 * Behaviour of reading a tactical reference off the character it points at.
 */
public class TacticalResolverTest extends AbstractCharacterTestCase
{

	private Equipment equipWeapon(String name) throws Exception
	{
		TestHelper.makeEquipment(name + "\tTYPE:Weapon.Melee.Simple\tWT:5\tCOST:5\tDAMAGE:1d8\tCRITRANGE:1"
			+ "\tCRITMULT:x2\tPROFICIENCY:WEAPON|" + name + "\tSIZE:M");
		Equipment weapon = Globals.getContext().getReferenceContext()
			.silentlyGetConstructedCDOMObject(Equipment.class, name);
		getCharacter().addEquipment(weapon);
		getCharacter().setCalcEquipmentList();
		return weapon;
	}

	@Test
	public void resolvesTheNumbersOfAWeaponTheCharacterCarries() throws Exception
	{
		equipWeapon("Test Mace");
		TacticalResolver resolver = new TacticalResolver(getCharacter());

		Optional<ResolvedWeapon> resolved = resolver.weapon("Test Mace");

		assertTrue(resolved.isPresent(), "Expected the equipped weapon to resolve");
		assertEquals("Test Mace", resolved.get().name());
		assertTrue(resolved.get().toHit().startsWith("+") || resolved.get().toHit().startsWith("-"),
			"Expected an attack bonus, got '" + resolved.get().toHit() + "'");
		assertTrue(resolved.get().damage().contains("d8"),
			"Expected the weapon's damage dice, got '" + resolved.get().damage() + "'");
	}

	@Test
	public void findsAWeaponWhateverTheCaseOfTheNameWrittenOnTheSheet() throws Exception
	{
		equipWeapon("Test Mace");
		TacticalResolver resolver = new TacticalResolver(getCharacter());

		assertTrue(resolver.weapon("test mace").isPresent());
	}

	@Test
	public void reportsAWeaponTheCharacterDoesNotCarry()
	{
		TacticalResolver resolver = new TacticalResolver(getCharacter());

		assertEquals(Optional.empty(), resolver.weapon("Sceptre of Timeon"));
	}

	@Test
	public void resolvesAVariableTheCharacterDefines()
	{
		PlayerCharacter character = getCharacter();
		PCTemplate template = new PCTemplate();
		template.setName("Channeller");
		template.put(VariableKey.getConstant("ChannelUses"), FormulaFactory.getFormulaFor(7));
		character.addTemplate(template);

		assertEquals(Optional.of("7"), new TacticalResolver(character).number("ChannelUses"));
	}

	@Test
	public void reportsAVariableTheCharacterDoesNotDefine()
	{
		assertEquals(Optional.empty(), new TacticalResolver(getCharacter()).number("NoSuchVariable"));
	}

	@Test
	public void hasNoPreparedSpellsForACharacterWithNone()
	{
		assertEquals(List.<ResolvedSpell>of(), new TacticalResolver(getCharacter()).spells(SpellSource.PREPARED));
	}
}
