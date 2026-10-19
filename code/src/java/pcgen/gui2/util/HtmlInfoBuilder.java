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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import pcgen.cdom.base.CDOMObject;
import pcgen.cdom.enumeration.StringKey;
import pcgen.core.Ability;
import pcgen.core.Race;
import pcgen.core.spell.Spell;
import pcgen.system.LanguageBundle;

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
	private static final String PATHFINDER_FR_SEARCH = "https://www.pathfinder-fr.org/Wiki/Search.aspx?Query=";
	private static final String PATHFINDER_WIKI_SEARCH = "https://pathfinderwiki.com/w/index.php?search=";
	private static final String D20PFSRD_SEARCH = "https://www.d20pfsrd.com/?s=";

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
			if (label == null)
			{
				link = paizoLink(sourceObject);
				label = (link == null) ? null : NETHYS_LABEL;
			}
			if (label != null)
			{
				appendLink(link, label);
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
		appendSearchLink(PATHFINDER_FR_LABEL, PATHFINDER_FR_SEARCH, source.getLocalizedName(), primaryLabel);
		appendSearchLink(PATHFINDER_WIKI_LABEL, PATHFINDER_WIKI_SEARCH, englishName, primaryLabel);
		appendSearchLink(D20PFSRD_LABEL, D20PFSRD_SEARCH, englishName, primaryLabel);
	}

	private void appendSearchLink(String label, String searchUrl, String name, String primaryLabel)
	{
		if (!label.equals(primaryLabel))
		{
			appendLink(searchUrl + encode(name), label);
		}
	}

	private static String encode(String name)
	{
		return URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
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

	private static String paizoLink(CDOMObject source)
	{
		URI sourceUri = source.getSourceURI();
		if (sourceUri == null || sourceUri.getPath() == null)
		{
			return null;
		}
		String path = sourceUri.getPath().replace('\\', '/');
		if (!path.contains("/pathfinder/paizo/") && !path.contains("/pathfinder_pfs/paizo/"))
		{
			return null;
		}
		String name = source.getDisplayName();
		if (name == null || name.isBlank())
		{
			return null;
		}
		String encodedName = encode(name);
		String page = null;
		if (source instanceof Spell)
		{
			page = "SpellDisplay.aspx?ItemName=";
		}
		else if (source instanceof Ability ability && ability.getCDOMCategory() != null)
		{
			page = switch (ability.getCDOMCategory().getKeyName().toUpperCase(Locale.ROOT))
			{
				case "FEAT" -> "FeatDisplay.aspx?ItemName=";
				case "TRAIT" -> "TraitDisplay.aspx?ItemName=";
				default -> null;
			};
		}
		else if (source instanceof Race)
		{
			page = path.contains("/bestiary") ? "MonsterDisplay.aspx?ItemName=" : "RacesDisplay.aspx?ItemName=";
		}
		if (page == null || name.contains("~") || name.contains("("))
		{
			page = "Search.aspx?Query=";
		}
		return "https://aonprd.com/" + page + encodedName;
	}
}
