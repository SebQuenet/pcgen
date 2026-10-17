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
package pcgen.gui2.tabs.conjurations;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;

import pcgen.facade.core.BonusChoice;
import pcgen.facade.core.ConjurationSheetFacade;
import pcgen.util.Logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * What the conjurations page can call back into. Planted on the page's window
 * object so that conjuring, wounding and dismissing a creature reaches the
 * character.
 *
 * <p>
 * Public, with public methods, because JavaScript running in the WebView
 * reaches it by reflection. The same page opened in an ordinary browser finds
 * no bridge and keeps the fight to itself.
 */
public final class ConjurationBridge
{

	/** What the page passes for a bonus that asks no value. */
	public static final int NO_VALUE = -1;

	private static final ObjectMapper JSON = new ObjectMapper();

	private final ConjurationSheetFacade sheet;

	/**
	 * @param sheet the conjurations to write through. Never null.
	 */
	public ConjurationBridge(ConjurationSheetFacade sheet)
	{
		if (sheet == null)
		{
			throw new IllegalArgumentException("A conjuration bridge needs conjurations to write through");
		}
		this.sheet = sheet;
	}

	/**
	 * Called from the page when a creature of the catalog is conjured.
	 *
	 * @param creature the name of its stat line.
	 * @return the number it was given.
	 */
	public int conjure(String creature)
	{
		return sheet.conjure(creature);
	}

	/**
	 * Called from the page when a creature in play is harmed or healed.
	 *
	 * @param creature its name.
	 * @param number   its number.
	 * @param damage   the total damage it has taken.
	 */
	public void setDamage(String creature, int number, int damage)
	{
		sheet.setDamage(creature, number, damage);
	}

	/**
	 * Called from the page when the rounds left to a creature are corrected.
	 *
	 * @param creature its name.
	 * @param number   its number.
	 * @param rounds   the rounds it has left.
	 */
	public void setRoundsLeft(String creature, int number, int rounds)
	{
		sheet.setRoundsLeft(creature, number, rounds);
	}

	/**
	 * Called from the page when the fight moves on a round.
	 */
	public void nextRound()
	{
		sheet.nextRound();
	}

	/**
	 * Called from the page when a creature is sent away.
	 *
	 * @param creature its name.
	 * @param number   its number.
	 */
	public void dismiss(String creature, int number)
	{
		sheet.dismiss(creature, number);
	}

	/**
	 * Called from the page when every creature is sent away.
	 */
	public void dismissAll()
	{
		sheet.dismissAll();
	}

	/**
	 * Called from the page when the duration given to new creatures changes.
	 *
	 * @param rounds how many rounds a new creature stays, 0 not to count them.
	 */
	public void setDuration(int rounds)
	{
		sheet.setDuration(rounds);
	}

	/**
	 * @return the group bonuses, as JSON: a list of {key, label}.
	 */
	public String groupBonuses()
	{
		return json(sheet.groupBonuses());
	}

	/**
	 * @return what may be laid on every creature, as JSON: a list of {key, label, range}.
	 */
	public String groupBonusChoices()
	{
		return json(sheet.groupBonusChoices().stream().map(ConjurationBridge::choiceOf).toList());
	}

	/**
	 * @param creature the creature's name
	 * @param number   its number
	 * @return what may be laid on that creature, as JSON: a list of {key, label, range}.
	 */
	public String bonusChoices(String creature, int number)
	{
		return json(sheet.bonusChoices(creature, number).stream().map(ConjurationBridge::choiceOf).toList());
	}

	/**
	 * @param creature the creature's name
	 * @param number   its number
	 * @return the creature as JSON, or "null" when it is not in play.
	 */
	public String view(String creature, int number)
	{
		return json(sheet.viewOf(creature, number).orElse(null));
	}

	/**
	 * @param key   the key of the bonus's source
	 * @param value the value chosen, or {@value #NO_VALUE} for a bonus that asks none
	 * @return every creature in play, refreshed, as JSON.
	 */
	public String addGroupBonus(String key, int value)
	{
		return json(sheet.addGroupBonus(key, valueOf(value)));
	}

	/**
	 * @param key the key of a group bonus's source
	 * @return every creature in play, refreshed, as JSON.
	 */
	public String removeGroupBonus(String key)
	{
		return json(sheet.removeGroupBonus(key));
	}

	/**
	 * @param creature the creature's name
	 * @param number   its number
	 * @param key      the key of the bonus's source
	 * @param value    the value chosen, or {@value #NO_VALUE} for a bonus that asks none
	 * @return that creature, refreshed, as JSON.
	 */
	public String addBonus(String creature, int number, String key, int value)
	{
		return json(sheet.addBonus(creature, number, key, valueOf(value)));
	}

	/**
	 * @param creature the creature's name
	 * @param number   its number
	 * @param key      the key of that creature's own bonus's source
	 * @return that creature, refreshed, as JSON.
	 */
	public String removeBonus(String creature, int number, String key)
	{
		return json(sheet.removeBonus(creature, number, key));
	}

	private static OptionalInt valueOf(int value)
	{
		return (value == NO_VALUE) ? OptionalInt.empty() : OptionalInt.of(value);
	}

	private static Map<String, Object> choiceOf(BonusChoice choice)
	{
		Map<String, Object> written = new LinkedHashMap<>();
		written.put("key", choice.key());
		written.put("label", choice.label());
		written.put("range", choice.range().orElse(null));
		return written;
	}

	private static String json(Object value)
	{
		try
		{
			return JSON.writeValueAsString(value);
		}
		catch (JsonProcessingException e)
		{
			Logging.errorPrint("Could not write conjurations for the page", e);
			return "null";
		}
	}
}
