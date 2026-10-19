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
package pcgen.facade.core;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import pcgen.core.conjurations.SummonParseError;
import pcgen.core.conjurations.CreatureStats;

/**
 * The conjurations of a character, between the user interface and the core.
 *
 * <p>
 * Two different acts live here. Changing the catalog rewrites its source text,
 * which is what the character stores. Playing the fight brings creatures in,
 * wounds them and sends them away, which is play state kept apart from the
 * catalog. A creature in play is named by its stat line and its number.
 */
public interface ConjurationSheetFacade
{

	/**
	 * @return the catalog as written, empty when the character has none.
	 */
	String getCatalogSource();

	/**
	 * Replaces the catalog with what the writer has typed, whether or not it
	 * reads: losing someone's typing because of a half finished line would be
	 * worse than storing it.
	 *
	 * @param source the catalog's source text. Blank removes the catalog, and
	 *               the fight with it.
	 */
	void setCatalogSource(String source);

	/**
	 * Reads a candidate catalog without storing it, so an editor can say what
	 * is wrong while it is being typed.
	 *
	 * @param source the catalog's source text.
	 * @return every error found, empty when the catalog reads.
	 */
	List<SummonParseError> errorsIn(String source);

	/**
	 * Computes one catalog entry from its race, templates and the conjurer's feats.
	 * The result is cached until that entry or Augment Summoning changes.
	 */
	Optional<CreatureStats> catalogStatsOf(String creature);

	/** A source block that records a selected published option, if selectable. */
	Optional<String> selectionSource(String creature);

	/**
	 * Brings a creature into the fight, unhurt, for the duration set.
	 *
	 * @param creature the name of its stat line in the catalog.
	 * @return its number: the lowest no creature of that name in play holds.
	 */
	int conjure(String creature);

	/**
	 * @param creature the name of a creature in play.
	 * @param number   its number.
	 * @param damage   the total damage it has taken; below zero counts as none.
	 */
	void setDamage(String creature, int number, int damage);

	/**
	 * @param creature the name of a creature in play.
	 * @param number   its number.
	 * @param rounds   how many rounds it has left; below zero counts as none.
	 */
	void setRoundsLeft(String creature, int number, int rounds);

	/**
	 * Takes a round from every creature whose rounds are counted.
	 */
	void nextRound();

	/**
	 * @param creature the name of a creature in play.
	 * @param number   its number.
	 */
	void dismiss(String creature, int number);

	/**
	 * Sends every creature away. The duration set stays.
	 */
	void dismissAll();

	/**
	 * @param rounds how many rounds a newly conjured creature stays; zero or
	 *               less not to count them.
	 */
	void setDuration(int rounds);

	/**
	 * @return the bonuses laid on every creature in play and to come, in the order laid.
	 */
	List<BonusLabel> groupBonuses();

	/**
	 * @return what may be laid on every creature: the bonuses anyone can lay on another.
	 */
	List<BonusChoice> groupBonusChoices();

	/**
	 * @param creature the creature's name
	 * @param number   its number
	 * @return what may be laid on that creature, its own abilities included; empty when
	 *         it is not in play or its catalog entry is gone.
	 */
	List<BonusChoice> bonusChoices(String creature, int number);

	/**
	 * Builds the creature the first time it is asked for, which takes seconds.
	 *
	 * @param creature the creature's name
	 * @param number   its number
	 * @return the creature as the page shows it, or nothing when it is not in play or
	 *         its catalog entry is gone.
	 */
	Optional<ConjuredCreatureView> viewOf(String creature, int number);

	/** Changes one creature's current form when its race has that ability. */
	boolean setForm(String creature, int number, String form);

	/** Resets the once-per-day skeletal variant after the character rests. */
	void resetSkeletonUse();

	/**
	 * @param key   the key of the bonus's source, such as "SPELL=Prayer"
	 * @param value the value chosen for a bonus that asks one
	 * @return every creature in play, refreshed; nothing when the key names no loaded source.
	 */
	List<ConjuredCreatureView> addGroupBonus(String key, OptionalInt value);

	/**
	 * @param key the key of a group bonus's source
	 * @return every creature in play, refreshed.
	 */
	List<ConjuredCreatureView> removeGroupBonus(String key);

	/**
	 * @param creature the creature's name
	 * @param number   its number
	 * @param key      the key of the bonus's source
	 * @param value    the value chosen for a bonus that asks one
	 * @return that creature, refreshed; nothing when the key names no loaded source.
	 */
	List<ConjuredCreatureView> addBonus(String creature, int number, String key, OptionalInt value);

	/**
	 * @param creature the creature's name
	 * @param number   its number
	 * @param key      the key of that creature's own bonus's source
	 * @return that creature, refreshed.
	 */
	List<ConjuredCreatureView> removeBonus(String creature, int number, String key);
}
