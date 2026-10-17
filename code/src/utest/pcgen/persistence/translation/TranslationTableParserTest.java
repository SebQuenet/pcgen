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
package pcgen.persistence.translation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Behaviour of the translation table syntax: one tab separated line per translated field.
 */
class TranslationTableParserTest
{

	@Test
	void readsALineIntoAnEntry()
	{
		TranslationTable table = TranslationTableParser.parse("ABILITY\tFEAT\tPower Attack\tNAME\tAttaque en puissance\n");

		assertEquals(List.of(new TranslationEntry(1, "ABILITY", "FEAT", "Power Attack", TranslatedField.NAME,
			"Attaque en puissance")), table.entries());
	}

	@Test
	void leavesTheCategoryEmptyForObjectsWithoutOne()
	{
		TranslationTable table = TranslationTableParser.parse("SPELL\t\tMagic Missile\tNAME\tProjectile magique");

		assertEquals("", table.entries().getFirst().category());
	}

	@Test
	void skipsTheHeaderCommentsAndBlankLines()
	{
		TranslationTable table = TranslationTableParser.parse("""
			TYPE\tCATEGORY\tKEY\tFIELD\tTEXT
			# Core Rulebook

			SPELL\t\tMagic Missile\tNAME\tProjectile magique
			""");

		assertEquals(4, table.entries().getFirst().line());
	}

	@Test
	void reportsALineWithoutFiveColumns()
	{
		TranslationTable table = TranslationTableParser.parse("SPELL\tMagic Missile\tNAME\tProjectile magique");

		assertEquals(1, table.problems().getFirst().line());
	}

	@Test
	void reportsAnUnknownField()
	{
		TranslationTable table = TranslationTableParser.parse("SPELL\t\tMagic Missile\tASPECT\tProjectile magique");

		assertTrue(table.entries().isEmpty() && table.problems().size() == 1);
	}

	@Test
	void reportsAnEmptyText()
	{
		TranslationTable table = TranslationTableParser.parse("SPELL\t\tMagic Missile\tNAME\t");

		assertEquals(1, table.problems().size());
	}

	@Test
	void keepsTheGoodLinesAroundABadOne()
	{
		TranslationTable table = TranslationTableParser.parse("""
			SPELL\t\tMagic Missile\tNAME\tProjectile magique
			SPELL\tbroken
			SPELL\t\tFireball\tNAME\tBoule de feu
			""");

		assertEquals(2, table.entries().size());
	}
}
