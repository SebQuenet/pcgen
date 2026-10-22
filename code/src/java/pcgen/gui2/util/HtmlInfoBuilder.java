/*
 * Copyright 2007 (C) Koen Van Daele
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
 *
 *
 */
package pcgen.gui2.util;

import java.net.URI;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import pcgen.cdom.base.CDOMObject;
import pcgen.cdom.enumeration.StringKey;
import pcgen.core.Ability;
import pcgen.core.Deity;
import pcgen.core.Domain;
import pcgen.core.PCClass;
import pcgen.core.Race;
import pcgen.core.Skill;
import pcgen.core.spell.Spell;
import pcgen.system.LanguageBundle;
import pcgen.util.FirstResultSearch;

/**
 * {@code HtmlInfoBuilder} is a helper class for the various
 * setInfoLabelText methods in the gui tabs.
 *
 *
 */
public class HtmlInfoBuilder
{
	/** Constant for 3 spaces in HTML */
	public static final String THREE_SPACES = " &nbsp; "; //$NON-NLS-1$

	private static final String NETHYS_LABEL = "Archives of Nethys";
	private static final String PATHFINDER_FR_LABEL = "Pathfinder-fr";
	private static final String PATHFINDER_WIKI_LABEL = "PathfinderWiki";
	private static final String D20PFSRD_LABEL = "d20PFSRD";

	private final StringBuilder buffer = new StringBuilder(300);
	private final boolean fullDocument;
	private CDOMObject sourceObject;

	public HtmlInfoBuilder withSource(CDOMObject sourceObject)
	{
		this.sourceObject = sourceObject;
		return this;
	}

	public HtmlInfoBuilder()
	{
		this(null, true);
	}

	/**
	 * @param title Element that will be added as the start of the string and emphasized.
	 */
	public HtmlInfoBuilder(String title)
	{
		this(title, true);
	}

	/**
	 * @param title Element that will be added as the start of the string and emphasized.
	 * @param fullDocument Should html tags be added to make this a full html document
	 */
	public HtmlInfoBuilder(String title, boolean fullDocument)
	{
		this.fullDocument = fullDocument;
		if (fullDocument)
		{
			buffer.append("<html>");
		}
		if (title != null)
		{
			appendTitleElement(title);
		}
	}

	/**
	 * Adds a string to the LabelText.
	 * @param string String to add
	 * @return HtmlInfoBuilder
	 */
	public HtmlInfoBuilder append(final String string)
	{
		buffer.append(string);
		return this;
	}

	/**
	 * Adds a character to the LabelText.
	 * @param ch Char to add
	 * @return HtmlInfoBuilder
	 */
	public HtmlInfoBuilder append(final char ch)
	{
		buffer.append(ch);
		return this;
	}

	/**
	 * Adds a line break to the LabelText.
	 * @return HtmlInfoBuilder
	 */
	public HtmlInfoBuilder appendLineBreak()
	{
		buffer.append("<br>");
		return this;
	}

	/**
	 * Adds a spacer to the LabelText.
	 * @return HtmlInfoBuilder
	 */
	public HtmlInfoBuilder appendSpacer()
	{
		buffer.append(THREE_SPACES);
		return this;
	}

	/**
	 * Append a title element with a regular sized font.
	 * 
	 * @param title the title
	 */
	public void appendSmallTitleElement(final String title)
	{
		buffer.append("<b>").append(title).append("</b>");
	}

	/**
	 * Append a title element with a larger sized font.
	 * 
	 * @param title the title
	 */
	public void appendTitleElement(final String title)
	{
		buffer.append("<b><font size=+1>").append(title).append("</font></b>");
	}

	/**
	 * Adds an element to the labelText. The key will be put in bold-face.
	 * @param key The string that will be used as the key in the LabelText, e.g. SOURCE. 
	 * @param value The value that belongs to the key.
	 * @return HtmlInfoBuilder
	 */
	public HtmlInfoBuilder appendElement(final String key, final String value)
	{
		buffer.append("<b>").append(key).append(":</b>&nbsp;").append(value);
		return this;
	}

	/**
	 * Used for internationalisation. Looks up the property through the 
	 * {@code ProperyFactory} and uses that as the key.
	 * @param propertyKey The name of a property in the LanguageProperties file.
	 * @param value The value that belongs to the key.
	 * @return HtmlInfoBuilder
	 */
	public HtmlInfoBuilder appendI18nElement(final String propertyKey, final String value)
	{
		return appendElement(LanguageBundle.getString(propertyKey), value);
	}

	/**
	 * Used for internationalisation. Looks up the property through the 
	 * {@code ProperyFactory} and uses that as the key.
	 * @param propertyKey The name of a property in the LanguageProperties file.
	 * @param value The values that should be added as parameters to the property.
	 * @return HtmlInfoBuilder
	 */
	public HtmlInfoBuilder appendI18nFormattedElement(final String propertyKey, final String... value)
	{
		buffer.append(LanguageBundle.getFormattedString(propertyKey, (Object[]) value));
		return this;
	}

	/**
	 * Append an image to the label text.
	 * 
	 * @param iconPath the URL of the icon
	 */
	public void appendIconElement(final String iconPath)
	{
		buffer.append("<img src=\"").append(iconPath).append("\" >&nbsp;");
	}

	@Override
	public String toString()
	{
		if (sourceObject != null)
		{
			String link = sourceObject.get(StringKey.SOURCE_LINK);
			String label = sourceLabel(link);
			if (label != null)
			{
				appendLink(link, label);
			}
			if (label != null || isPaizoData(sourceObject))
			{
				appendSearchLinks(sourceObject, label);
			}
		}
		if (fullDocument)
		{
			buffer.append("</html>");
		}
		return buffer.toString();
	}

	private void appendLink(String link, String label)
	{
		buffer.append("<br><a href=\"")
			.append(link.trim().replace("&", "&amp;").replace("\"", "&quot;"))
			.append("\">").append(label).append("</a>");
	}

	private void appendSearchLinks(CDOMObject source, String primaryLabel)
	{
		String englishName = source.getDisplayName();
		if (englishName == null || englishName.isBlank())
		{
			return;
		}
		SearchKeyword keyword = SearchKeyword.of(source);
		String englishQuery = joinTerms(englishName, keyword.english());
		String frenchQuery = joinTerms(source.getLocalizedName(), keyword.french(),
			FirstResultSearch.PATHFINDER_FR_RULES_TERM);
		appendSearchLink(NETHYS_LABEL, FirstResultSearch.NETHYS, englishQuery, primaryLabel);
		appendSearchLink(PATHFINDER_FR_LABEL, FirstResultSearch.PATHFINDER_FR, frenchQuery, primaryLabel);
		appendSearchLink(PATHFINDER_WIKI_LABEL, FirstResultSearch.PATHFINDER_WIKI, englishQuery, primaryLabel);
		appendSearchLink(D20PFSRD_LABEL, FirstResultSearch.D20PFSRD, englishQuery, primaryLabel);
	}

	private void appendSearchLink(String label, String site, String query, String primaryLabel)
	{
		if (!label.equals(primaryLabel))
		{
			appendLink(FirstResultSearch.addressOf(query, site), label);
		}
	}

	private static String joinTerms(String... terms)
	{
		return Stream.of(terms).filter(term -> !term.isEmpty()).collect(Collectors.joining(" "));
	}

	private static String sourceLabel(String link)
	{
		if (link == null || link.isBlank())
		{
			return null;
		}
		try
		{
			URI uri = URI.create(link.trim());
			String host = uri.getHost();
			if (host == null || !("http".equalsIgnoreCase(uri.getScheme())
				|| "https".equalsIgnoreCase(uri.getScheme())))
			{
				return null;
			}
			return switch (host.toLowerCase(Locale.ROOT).replaceFirst("^www\\.", ""))
			{
				case "d20pfsrd.com" -> D20PFSRD_LABEL;
				case "aonprd.com" -> NETHYS_LABEL;
				case "pathfinderwiki.com" -> PATHFINDER_WIKI_LABEL;
				default -> null;
			};
		}
		catch (IllegalArgumentException ignored)
		{
			return null;
		}
	}

	private static boolean isPaizoData(CDOMObject source)
	{
		String path = sourcePath(source);
		return path.contains("/pathfinder/paizo/") || path.contains("/pathfinder_pfs/paizo/");
	}

	private static String sourcePath(CDOMObject source)
	{
		URI sourceUri = source.getSourceURI();
		if (sourceUri == null || sourceUri.getPath() == null)
		{
			return "";
		}
		return sourceUri.getPath().replace('\\', '/');
	}

	/**
	 * The word naming the kind of entry in each language, so the search lands on the rules page
	 * rather than on a homonym.
	 */
	private record SearchKeyword(String english, String french)
	{
		private static final SearchKeyword NONE = new SearchKeyword("", "");

		static SearchKeyword of(CDOMObject source)
		{
			if (source instanceof Spell)
			{
				return new SearchKeyword("spell", "sort");
			}
			if (source instanceof Ability ability && ability.getCDOMCategory() != null)
			{
				return switch (ability.getCDOMCategory().getKeyName().toUpperCase(Locale.ROOT))
				{
					case "FEAT" -> new SearchKeyword("feat", "don");
					case "TRAIT" -> new SearchKeyword("trait", "trait");
					default -> NONE;
				};
			}
			if (source instanceof Race)
			{
				// "monstre" sends pathfinder-fr searches to monster lists instead of the creature itself
				return sourcePath(source).contains("/bestiary") ? new SearchKeyword("monster", "")
					: new SearchKeyword("race", "race");
			}
			if (source instanceof PCClass)
			{
				return new SearchKeyword("class", "classe");
			}
			if (source instanceof Deity)
			{
				return new SearchKeyword("deity", "divinité");
			}
			if (source instanceof Domain)
			{
				return new SearchKeyword("domain", "domaine");
			}
			if (source instanceof Skill)
			{
				return new SearchKeyword("skill", "compétence");
			}
			return NONE;
		}
	}
}
