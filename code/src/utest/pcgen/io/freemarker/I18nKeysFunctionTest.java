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
package pcgen.io.freemarker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModelException;
import org.junit.jupiter.api.Test;

/**
 * Behaviour of the i18nKeys function an XML export uses to publish a sheet's labels.
 */
class I18nKeysFunctionTest
{
	@Test
	void listsTheKeysStartingWithThePrefix() throws TemplateModelException
	{
		List<String> keys = new I18nKeysFunction().exec(List.of(new SimpleScalar("in_sheet_")));

		assertTrue(keys.contains("in_sheet_skills") && keys.stream().allMatch(key -> key.startsWith("in_sheet_")));
	}

	@Test
	void listsTheKeysInOrder() throws TemplateModelException
	{
		List<String> keys = new I18nKeysFunction().exec(List.of(new SimpleScalar("in_sheet_")));

		assertEquals(keys.stream().sorted().toList(), keys);
	}

	@Test
	void refusesACallWithoutAPrefix()
	{
		assertThrows(TemplateModelException.class, () -> new I18nKeysFunction().exec(List.of()));
	}
}
