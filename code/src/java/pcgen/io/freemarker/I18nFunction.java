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

import java.util.List;

import pcgen.system.LanguageBundle;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateMethodModelEx;

/**
 * Gives a character sheet its labels in the configured language, from the language bundle:
 * {@code ${i18n("in_sheet_skills")}}, or with placeholders {@code ${i18n("in_sheet_clericOf", deity)}}.
 */
public class I18nFunction implements TemplateMethodModelEx
{
	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException
	{
		if (arguments.isEmpty())
		{
			throw new TemplateModelException("Wrong arguments. A language bundle key is required");
		}
		String key = arguments.getFirst().toString();
		Object[] placeholders = arguments.subList(1, arguments.size()).stream().map(Object::toString).toArray();
		return placeholders.length == 0
			? LanguageBundle.getString(key)
			: LanguageBundle.getFormattedString(key, placeholders);
	}
}
