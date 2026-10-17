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
			String type = StringPClassUtil.getStringFor(object.getClass());
			Campaign campaign = object.get(ObjectKey.SOURCE_CAMPAIGN);
			if (type == null || campaign == null || object.getSafe(ObjectKey.VISIBILITY) == Visibility.HIDDEN)
			{
				continue;
			}
			byCampaign.computeIfAbsent(campaign, unused -> new ArrayList<>()).addAll(textsOf(type, object));
		}
		byCampaign.values().forEach(texts -> texts.sort(TABLE_ORDER));
		return byCampaign;
	}

	private static List<TranslatableText> textsOf(String type, CDOMObject object)
	{
		String category = object instanceof Ability ability ? ability.getCDOMCategory().getKeyName() : "";
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
