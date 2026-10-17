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

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

/**
 * Lists, in order, the language bundle keys that start with a prefix, so that an XML export
 * can publish a sheet's labels for its stylesheet: {@code <#list i18nKeys("in_pdf_") as key>}.
 */
public class I18nKeysFunction implements TemplateMethodModelEx
{
	@SuppressWarnings("rawtypes")
	@Override
	public List<String> exec(List arguments) throws TemplateModelException
	{
		if (arguments.isEmpty())
		{
			throw new TemplateModelException("Wrong arguments. A language bundle key prefix is required");
		}
		String prefix = arguments.getFirst().toString();
		return LanguageBundle.getBundle().keySet().stream()
			.filter(key -> key.startsWith(prefix))
			.sorted()
			.toList();
	}
}
