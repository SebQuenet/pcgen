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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import pcgen.cdom.base.Constants;
import pcgen.core.Equipment;
import pcgen.core.Globals;
import pcgen.core.PCClass;
import pcgen.core.PlayerCharacter;
import pcgen.core.character.CharacterSpell;
import pcgen.core.character.SpellInfo;
import pcgen.core.display.CharacterDisplay;
import pcgen.core.tactics.SpellSource;
import pcgen.io.exporttoken.WeaponToken;

/**
 * Reads a tactical reference off the character it points at, so a sheet
 * follows a level up without being rewritten.
 *
 * <p>
 * Every answer is an {@link Optional}: a reference that no longer resolves —
 * a weapon sold, a variable renamed — comes back empty, and the renderer shows
 * the block with a warning rather than dropping it.
 */
public final class TacticalResolver
{
	private final PlayerCharacter character;
	private final WeaponToken weaponToken = new WeaponToken();

	/**
	 * @param character the character whose numbers the sheet borrows. Never
	 *                  null.
	 */
	public TacticalResolver(PlayerCharacter character)
	{
		if (character == null)
		{
			throw new IllegalArgumentException("A tactical resolver needs a character");
		}
		this.character = character;
	}

	/**
	 * @param name the weapon's name, matched without regard to case
	 * @return what the character's data says about it, empty when the
	 *         character carries no such weapon
	 */
	public Optional<ResolvedWeapon> weapon(String name)
	{
		List<Equipment> weapons = character.getExpandedWeapons(Constants.MERGE_ALL);
		for (int index = 0; index < weapons.size(); index++)
		{
			Equipment weapon = weapons.get(index);
			if (weapon.getName().equalsIgnoreCase(name.strip()))
			{
				return Optional.of(new ResolvedWeapon(weapon.getName(), field(index, "TOTALHIT"),
					field(index, "DAMAGE"), field(index, "CRIT")));
			}
		}
		return Optional.empty();
	}

	private String field(int index, String name)
	{
		String value = weaponToken.getToken("WEAPON." + index + '.' + name, character, null);
		return (value == null) ? "" : value;
	}

	/**
	 * @param variableName the variable PCGen computes
	 * @return its value with no trailing zeroes, empty when the character
	 *         defines no such variable
	 */
	public Optional<String> number(String variableName)
	{
		String name = variableName.strip();
		if (!character.hasVariable(name))
		{
			return Optional.empty();
		}
		float value = character.getVariableValue(name, Constants.EMPTY_STRING);
		return Optional.of((value == Math.rint(value)) ? Integer.toString((int) value) : Float.toString(value));
	}

	/**
	 * The temporary bonuses the character holds, which is what a buff may hand
	 * its arithmetic to PCGen by naming.
	 *
	 * @return their names, empty when the character holds none
	 */
	public List<String> temporaryBonusNames()
	{
		return List.copyOf(character.getNamedTempBonusList());
	}

	/**
	 * @param name the temporary bonus's name
	 * @return true when the character holds a temporary bonus of that name
	 */
	public boolean hasTemporaryBonus(String name)
	{
		if (name == null || name.isBlank())
		{
			return false;
		}
		return character.getNamedTempBonusList().stream().anyMatch(held -> held.equalsIgnoreCase(name.strip()));
	}

	/**
	 * The spells the character holds, in level then name order.
	 *
	 * @param source which list to read
	 * @return the spells, empty when the character holds none of that kind
	 */
	public List<ResolvedSpell> spells(SpellSource source)
	{
		String knownBook = Globals.getDefaultSpellBook();
		CharacterDisplay display = character.getDisplay();
		List<ResolvedSpell> spells = new ArrayList<>();
		for (PCClass pcClass : display.getClassSet())
		{
			for (CharacterSpell characterSpell : display.getCharacterSpells(pcClass))
			{
				for (SpellInfo info : characterSpell.getInfoList())
				{
					boolean isKnown = knownBook.equals(info.getBook());
					if (isKnown == (source == SpellSource.KNOWN))
					{
						spells.add(new ResolvedSpell(characterSpell.getSpell().getDisplayName(),
							info.getActualLevel(), info.getBook(), info.getTimes()));
					}
				}
			}
		}
		spells.sort(Comparator.comparingInt(ResolvedSpell::level)
			.thenComparing(spell -> spell.name().toLowerCase(Locale.ROOT)));
		return List.copyOf(spells);
	}
}
