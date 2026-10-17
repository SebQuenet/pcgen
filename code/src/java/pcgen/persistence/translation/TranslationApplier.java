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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pcgen.cdom.base.CDOMObject;
import pcgen.cdom.base.Loadable;
import pcgen.cdom.enumeration.ListKey;
import pcgen.cdom.enumeration.ObjectKey;
import pcgen.cdom.enumeration.StringKey;
import pcgen.core.Ability;
import pcgen.core.AbilityCategory;
import pcgen.core.Description;
import pcgen.rules.context.AbstractReferenceContext;
import pcgen.util.StringPClassUtil;

/**
 * Lays a translation table over the loaded game objects. The translation is stored beside the
 * English text, never in its place: keys, prerequisites and saved characters keep reading English.
 */
public final class TranslationApplier
{
	private static final String ABILITY_TYPE = "ABILITY";
	private static final Pattern PLACEHOLDER = Pattern.compile("%%|%\\{?(\\d+)\\}?");
	private static final char VARIABLE_SEPARATOR = '|';

	private TranslationApplier()
	{
	}

	/**
	 * @return the entries left out, and why; every other entry is applied
	 */
	public static List<TranslationProblem> apply(TranslationTable table, AbstractReferenceContext references)
	{
		List<TranslationProblem> problems = new ArrayList<>();
		for (TranslationEntry entry : table.entries())
		{
			CDOMObject translated = objectOf(entry, references);
			if (translated == null)
			{
				problems.add(new TranslationProblem(entry.line(),
					"No loaded " + entry.type() + " with the key " + entry.key()));
				continue;
			}
			String refusal = store(entry, translated);
			if (refusal != null)
			{
				problems.add(new TranslationProblem(entry.line(), refusal));
			}
		}
		return problems;
	}

	private static CDOMObject objectOf(TranslationEntry entry, AbstractReferenceContext references)
	{
		if (ABILITY_TYPE.equals(entry.type()))
		{
			AbilityCategory category =
					references.silentlyGetConstructedCDOMObject(AbilityCategory.class, entry.category());
			if (category == null)
			{
				return null;
			}
			Ability ability = references.getManufacturerId(category).getActiveObject(entry.key());
			return ability;
		}
		Class<? extends Loadable> type = StringPClassUtil.getClassFor(entry.type());
		if (type == null)
		{
			return null;
		}
		Loadable loaded = references.silentlyGetConstructedCDOMObject(type, entry.key());
		return loaded instanceof CDOMObject object ? object : null;
	}

	/**
	 * @return why the entry was refused, or null when it was stored
	 */
	private static String store(TranslationEntry entry, CDOMObject translated)
	{
		switch (entry.field())
		{
			case NAME -> translated.setLocalizedName(entry.text());
			case OUTPUTNAME -> translated.put(StringKey.LOCALIZED_OUTPUT_NAME, entry.text());
			case TEMPDESC -> translated.put(StringKey.LOCALIZED_TEMP_DESCRIPTION, entry.text());
			case DESC ->
			{
				return storeDescription(entry, translated, ListKey.DESCRIPTION, ObjectKey.LOCALIZED_DESCRIPTION);
			}
			case BENEFIT ->
			{
				return storeDescription(entry, translated, ListKey.BENEFIT, ObjectKey.LOCALIZED_BENEFIT);
			}
		}
		return null;
	}

	private static String storeDescription(TranslationEntry entry, CDOMObject translated,
		ListKey<Description> englishKey, ObjectKey<Description> translatedKey)
	{
		List<Description> english = translated.getSafeListFor(englishKey);
		if (english.size() != 1)
		{
			return entry.key() + " has " + english.size() + " " + entry.field() + " entries; only one can be translated";
		}
		String englishTemplate = templateOf(english.getFirst());
		if (!placeholdersOf(englishTemplate).equals(placeholdersOf(entry.text())))
		{
			return "The translation does not name the same placeholders as \"" + englishTemplate + "\"";
		}
		translated.put(translatedKey, english.getFirst().withTemplate(entry.text()));
		return null;
	}

	private static String templateOf(Description description)
	{
		String text = description.getPCCText();
		int separator = text.indexOf(VARIABLE_SEPARATOR);
		return separator < 0 ? text : text.substring(0, separator);
	}

	private static Set<String> placeholdersOf(String template)
	{
		Set<String> placeholders = new TreeSet<>();
		Matcher matcher = PLACEHOLDER.matcher(template);
		while (matcher.find())
		{
			if (matcher.group(1) != null)
			{
				placeholders.add(matcher.group(1));
			}
		}
		return placeholders;
	}
}
