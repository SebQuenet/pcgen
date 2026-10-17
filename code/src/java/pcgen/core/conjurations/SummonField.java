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
package pcgen.core.conjurations;

import java.util.Arrays;
import java.util.Optional;

/**
 * One column of a conjuration's stat line, in the order the catalog shows them.
 * The key is what the source text writes before the colon.
 */
public enum SummonField
{
	SPEED("speed"),
	HIT_POINTS("hp"),
	ARMOR_CLASS("ac"),
	ATTACK("attack"),
	DAMAGE("damage"),
	SPECIAL("special"),
	CMB("cmb"),
	CMD("cmd"),
	REFLEX("ref"),
	FORTITUDE("fort"),
	WILL("will"),
	INITIATIVE("init"),
	SPELLS("spells"),
	NOTE("note");

	private final String key;

	SummonField(String key)
	{
		this.key = key;
	}

	/**
	 * @return the key the source text writes for this field
	 */
	public String key()
	{
		return key;
	}

	/**
	 * @param key a key as the source text writes it
	 * @return the field it names, empty when it names none
	 */
	public static Optional<SummonField> forKey(String key)
	{
		return Arrays.stream(values()).filter(field -> field.key.equals(key)).findFirst();
	}
}
