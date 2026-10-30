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
package pcgen.core.gametable.catalog;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * How a criterion compares a field's value with its operands. On a field of scores, the first operand names the score.
 */
public enum Operator
{
	EQUALS(EnumSet.of(FieldKind.NUMBER, FieldKind.TEXT, FieldKind.SCORES)),
	NOT_EQUALS(EnumSet.of(FieldKind.NUMBER, FieldKind.TEXT, FieldKind.SCORES)),
	AT_LEAST(EnumSet.of(FieldKind.NUMBER, FieldKind.SCORES)),
	AT_MOST(EnumSet.of(FieldKind.NUMBER, FieldKind.SCORES)),
	BETWEEN(EnumSet.of(FieldKind.NUMBER, FieldKind.SCORES)),
	CONTAINS(EnumSet.of(FieldKind.TEXT)),
	STARTS_WITH(EnumSet.of(FieldKind.TEXT)),
	MATCHES(EnumSet.of(FieldKind.TEXT)),
	ANY_OF(EnumSet.of(FieldKind.ITEMS, FieldKind.TEXT, FieldKind.SCORES)),
	ALL_OF(EnumSet.of(FieldKind.ITEMS, FieldKind.SCORES)),
	NONE_OF(EnumSet.of(FieldKind.ITEMS, FieldKind.TEXT, FieldKind.SCORES)),
	IS_TRUE(EnumSet.of(FieldKind.FLAG)),
	IS_FALSE(EnumSet.of(FieldKind.FLAG));

	private final Set<FieldKind> kinds;

	Operator(Set<FieldKind> kinds)
	{
		this.kinds = kinds;
	}

	/**
	 * @param kind a field's kind.
	 * @return whether this operator compares values of that kind.
	 */
	public boolean appliesTo(FieldKind kind)
	{
		return kinds.contains(kind);
	}

	/**
	 * @return the key of the operator's label in the language bundle.
	 */
	public String labelKey()
	{
		return "in_gametable_operator_" + name().toLowerCase(Locale.ROOT);
	}
}
