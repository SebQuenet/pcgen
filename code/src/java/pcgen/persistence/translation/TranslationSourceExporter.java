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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pcgen.cdom.base.CDOMObject;
import pcgen.cdom.enumeration.ListKey;
import pcgen.cdom.enumeration.ObjectKey;
import pcgen.cdom.enumeration.StringKey;
import pcgen.core.Ability;
import pcgen.core.Campaign;
import pcgen.core.Description;
import pcgen.core.SubClass;
import pcgen.core.SubstitutionClass;
import pcgen.rules.context.AbstractReferenceContext;
import pcgen.util.StringPClassUtil;
import pcgen.util.enumeration.Visibility;

/**
 * Lists, campaign by campaign, the English text of the loaded objects that a translation table
 * can replace. PCGen resolves the keys, so the list names objects exactly as the table must.
 */
public final class TranslationSourceExporter
{
	private static final Comparator<TranslatableText> TABLE_ORDER = Comparator.comparing(TranslatableText::type)
		.thenComparing(TranslatableText::category)
		.thenComparing(TranslatableText::key)
		.thenComparing(TranslatableText::field);

	private static final String SUBCLASS_TYPE = "SUBCLASS";
	private static final String INTERNAL_CATEGORY = "Internal";

	private TranslationSourceExporter()
	{
	}

	/**
	 * @return the texts of every object a player can see, under the campaign that defines it
	 */
	public static Map<Campaign, List<TranslatableText>> textsByCampaign(AbstractReferenceContext references)
	{
		Map<Campaign, List<TranslatableText>> byCampaign = new HashMap<>();
		for (Object loaded : references.getAllConstructedObjects())
		{
			if (!(loaded instanceof CDOMObject object))
			{
				continue;
			}
			String type = typeOf(object);
			Campaign campaign = object.get(ObjectKey.SOURCE_CAMPAIGN);
			if (type == null || campaign == null || !isSeen(object))
			{
				continue;
			}
			byCampaign.computeIfAbsent(campaign, unused -> new ArrayList<>()).addAll(textsOf(type, object));
		}
		byCampaign.values().forEach(texts -> texts.sort(TABLE_ORDER));
		return byCampaign;
	}

	/**
	 * Whether a player can ever see the object. A hidden category is not enough to tell: Special
	 * Ability is hidden from the GUI tabs, yet its abilities are on the character sheet. Only the
	 * Internal category holds bookkeeping abilities nobody reads.
	 */
	private static boolean isSeen(CDOMObject object)
	{
		if (object.getSafe(ObjectKey.VISIBILITY) == Visibility.HIDDEN)
		{
			return false;
		}
		return !(object instanceof Ability ability && INTERNAL_CATEGORY.equals(ability.getCDOMCategory().getKeyName()));
	}

	/**
	 * @return the type a translation table names the object by, or null for a type it cannot name
	 */
	private static String typeOf(CDOMObject object)
	{
		if (object instanceof SubClass)
		{
			return SUBCLASS_TYPE;
		}
		// StringPClassUtil names a substitution class CLASS, yet the table cannot find it as one
		return object instanceof SubstitutionClass ? null : StringPClassUtil.getStringFor(object.getClass());
	}

	private static List<TranslatableText> textsOf(String type, CDOMObject object)
	{
		String category = switch (object)
		{
			case Ability ability -> ability.getCDOMCategory().getKeyName();
			case SubClass subClass -> subClass.getCDOMCategory().getKeyName();
			default -> "";
		};
		List<TranslatableText> texts = new ArrayList<>();
		addIfPresent(texts, type, category, object, TranslatedField.NAME, object.getDisplayName());
		addIfPresent(texts, type, category, object, TranslatedField.OUTPUTNAME, object.get(StringKey.OUTPUT_NAME));
		addIfPresent(texts, type, category, object, TranslatedField.TEMPDESC, object.get(StringKey.TEMP_DESCRIPTION));
		addIfPresent(texts, type, category, object, TranslatedField.ABB, object.get(StringKey.ABB_KR));
		addIfPresent(texts, type, category, object, TranslatedField.DESC, onlyTemplate(object, ListKey.DESCRIPTION));
		addIfPresent(texts, type, category, object, TranslatedField.BENEFIT, onlyTemplate(object, ListKey.BENEFIT));
		return texts;
	}

	/**
	 * @return the template of the object's one description, or null: several cannot be translated
	 */
	private static String onlyTemplate(CDOMObject object, ListKey<Description> key)
	{
		List<Description> descriptions = object.getSafeListFor(key);
		return descriptions.size() == 1 ? descriptions.getFirst().getTemplate() : null;
	}

	private static void addIfPresent(List<TranslatableText> texts, String type, String category, CDOMObject object,
		TranslatedField field, String english)
	{
		if (english != null && !english.isBlank())
		{
			texts.add(new TranslatableText(type, category, object.getKeyName(), field, english));
		}
	}
}
