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

import java.util.List;

import pcgen.system.LanguageBundle;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModelException;
import org.junit.jupiter.api.Test;

/**
 * Behaviour of the i18n function a character sheet uses for its labels.
 */
class I18nFunctionTest
{
	@Test
	void returnsTheLabelInTheConfiguredLanguage() throws TemplateModelException
	{
		Object label = new I18nFunction().exec(List.of(new SimpleScalar("in_cancel")));

		assertEquals(LanguageBundle.getString("in_cancel"), label);
	}

	@Test
	void fillsTheLabelPlaceholders() throws TemplateModelException
	{
		Object label = new I18nFunction().exec(List.of(new SimpleScalar("in_exportTitle"), new SimpleScalar("Bob")));

		assertEquals(LanguageBundle.getFormattedString("in_exportTitle", "Bob"), label);
	}

	@Test
	void refusesACallWithoutAKey()
	{
		assertThrows(TemplateModelException.class, () -> new I18nFunction().exec(List.of()));
	}
}
