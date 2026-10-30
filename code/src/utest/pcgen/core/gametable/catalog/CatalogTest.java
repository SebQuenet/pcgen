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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import pcgen.core.gametable.MonsterSource;

import org.junit.jupiter.api.Test;

/**
 * The bestiary's entries, filtered by the game master's criteria.
 */
class CatalogTest
{
	private static CatalogEntry entry(String name, Map<CatalogField, FieldValue> values, Optional<String> failure)
	{
		return new CatalogEntry(name, new MonsterSource.BestiaryRace(name), EntryKind.RACE, new EntryFacts(values, failure));
	}

	@Test
	void aFilterKeepsMatchesAndUnknownsInOrder()
	{
		CatalogEntry strong = entry("Ogre", Map.of(CatalogField.ARMOR_CLASS, new FieldValue.Number(17)), Optional.empty());
		CatalogEntry weak = entry("Goblin", Map.of(CatalogField.ARMOR_CLASS, new FieldValue.Number(16)), Optional.empty());
		CatalogEntry unbuilt = entry("Aboleth", Map.of(), Optional.empty());
		Criterion armored = new Criterion.Test(CatalogField.ARMOR_CLASS, Operator.AT_LEAST, List.of("17"));

		List<Catalog.Filtered> kept = Catalog.of(List.of(strong, weak, unbuilt)).filter(armored);

		assertEquals(List.of(new Catalog.Filtered(strong, Match.YES), new Catalog.Filtered(unbuilt, Match.UNKNOWN)), kept);
	}

	@Test
	void anEntryThatFailedToBuildStaysListed()
	{
		CatalogEntry broken = entry("Ugash-iram", Map.of(CatalogField.NAME, new FieldValue.Text("Ugash-iram")),
			Optional.of("Ugash-iram cannot be built"));
		Criterion named = new Criterion.Test(CatalogField.NAME, Operator.CONTAINS, List.of("ugash"));

		assertEquals(List.of(new Catalog.Filtered(broken, Match.YES)), Catalog.of(List.of(broken)).filter(named));
	}

	@Test
	void anEntrysOwnFactsWinOverItsIndexedOnes()
	{
		CatalogEntry listed = entry("Aboleth", Map.of(CatalogField.NAME, new FieldValue.Text("Aboleth")), Optional.empty());
		BestiaryIndex index = BestiaryIndex.empty().with(new IndexKey(IndexKey.RACE, "Aboleth"),
			new EntryFacts(Map.of(CatalogField.NAME, new FieldValue.Text("Aboleth (indexed in another language)")), Optional.empty()), 0);

		CatalogEntry indexed = Catalog.of(List.of(listed)).withIndex(index).entries().getFirst();

		assertEquals(new FieldValue.Text("Aboleth"), indexed.facts().value(CatalogField.NAME).orElseThrow());
	}

	@Test
	void anEntryIndexedAsUnbuildableKeepsWhy()
	{
		CatalogEntry listed = entry("Ugash-iram", Map.of(CatalogField.NAME, new FieldValue.Text("Ugash-iram")), Optional.empty());
		BestiaryIndex index = BestiaryIndex.empty().with(new IndexKey(IndexKey.RACE, "Ugash-iram"),
			new EntryFacts(Map.of(), Optional.of("Ugash-iram cannot be built")), 0);

		CatalogEntry indexed = Catalog.of(List.of(listed)).withIndex(index).entries().getFirst();

		assertEquals(Optional.of("Ugash-iram cannot be built"), indexed.facts().buildFailure());
	}

	@Test
	void anEntrysFactsAreCompletedByItsIndexedOnes()
	{
		CatalogEntry listed = entry("Aboleth", Map.of(CatalogField.NAME, new FieldValue.Text("Aboleth")), Optional.empty());
		BestiaryIndex index = BestiaryIndex.empty().with(new IndexKey(IndexKey.RACE, "Aboleth"),
			new EntryFacts(Map.of(CatalogField.ARMOR_CLASS, new FieldValue.Number(20)), Optional.empty()), 0);

		CatalogEntry indexed = Catalog.of(List.of(listed)).withIndex(index).entries().getFirst();

		assertEquals(List.of(new FieldValue.Text("Aboleth"), new FieldValue.Number(20)),
			List.of(indexed.facts().value(CatalogField.NAME).orElseThrow(), indexed.facts().value(CatalogField.ARMOR_CLASS).orElseThrow()));
	}
}
