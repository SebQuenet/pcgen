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
package pcgen.core.conjurations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * The creatures a character has conjured into the fight, and how long a new
 * one stays.
 *
 * <p>
 * This is play state, kept apart from the catalog because a catalog is not a
 * counter. A fight survives a pause, so it is saved with the character.
 *
 * @param duration how many rounds a newly conjured creature stays, never
 *                 negative; 0 when rounds are not counted.
 * @param conjured the creatures in play, in the order they arrived. No two
 *                 share a name and a number, and the list is never modifiable
 *                 through the one passed in.
 * @param groupBonuses the temporary bonuses laid on every creature in play and
 *                     on every creature conjured after them, in the order laid.
 */
public record ConjurationSession(int duration, List<ConjuredCreature> conjured, List<AppliedBonus> groupBonuses)
{
	/**
	 * A fight with no group bonus.
	 */
	public ConjurationSession(int duration, List<ConjuredCreature> conjured)
	{
		this(duration, conjured, List.of());
	}

	public ConjurationSession
	{
		groupBonuses = (groupBonuses == null) ? List.of() : List.copyOf(groupBonuses);
		if (duration < 0)
		{
			throw new IllegalArgumentException("A duration cannot be negative, and " + duration + " is");
		}
		conjured = (conjured == null) ? List.of() : List.copyOf(conjured);
		Set<String> identities = new HashSet<>();
		for (ConjuredCreature creature : conjured)
		{
			if (!identities.add(identityOf(creature.creature(), creature.number())))
			{
				throw new IllegalArgumentException(
					"Two conjured creatures are both " + creature.creature() + ' ' + creature.number());
			}
		}
	}

	/**
	 * @return a session with nobody in play and no rounds counted
	 */
	public static ConjurationSession empty()
	{
		return new ConjurationSession(0, List.of());
	}

	/**
	 * @return true when there is nothing worth saving
	 */
	public boolean isEmpty()
	{
		return duration == 0 && conjured.isEmpty() && groupBonuses.isEmpty();
	}

	/**
	 * The creature a conjuration would bring now: the lowest number no creature
	 * of that name holds, unhurt, staying for the session's duration.
	 *
	 * @param creature the name of its stat line in the catalog
	 * @return the creature, not yet in play
	 */
	public ConjuredCreature arrival(String creature)
	{
		int number = 1;
		while (holds(creature, number))
		{
			number++;
		}
		OptionalInt rounds = (duration > 0) ? OptionalInt.of(duration) : OptionalInt.empty();
		return new ConjuredCreature(creature, number, 0, rounds);
	}

	/**
	 * @param arrived a creature arriving in the fight
	 * @return this session with it in play, last
	 */
	public ConjurationSession with(ConjuredCreature arrived)
	{
		List<ConjuredCreature> joined = new ArrayList<>(conjured);
		joined.add(arrived);
		return new ConjurationSession(duration, joined, groupBonuses);
	}

	/**
	 * @param creature the creature's name
	 * @param number   its number
	 * @param damage   the total damage it has taken, never negative
	 * @return this session with that creature's damage recorded; unchanged when
	 *         no such creature is in play
	 */
	public ConjurationSession withDamage(String creature, int number, int damage)
	{
		return changing(creature, number,
			found -> new ConjuredCreature(found.creature(), found.number(), damage, found.roundsLeft(),
				found.bonuses()));
	}

	/**
	 * @param creature the creature's name
	 * @param number   its number
	 * @param rounds   how many rounds it stays, never negative
	 * @return this session with that creature's rounds counted from there;
	 *         unchanged when no such creature is in play
	 */
	public ConjurationSession withRoundsLeft(String creature, int number, int rounds)
	{
		return changing(creature, number, found -> new ConjuredCreature(found.creature(), found.number(),
			found.damage(), OptionalInt.of(rounds), found.bonuses()));
	}

	/**
	 * @return this session one round later: every creature whose rounds are
	 *         counted has one fewer, and none goes below zero
	 */
	public ConjurationSession nextRound()
	{
		return new ConjurationSession(duration, conjured.stream().map(ConjurationSession::oneRoundLater).toList(),
			groupBonuses);
	}

	/**
	 * @param creature the creature's name
	 * @param number   its number
	 * @return this session without that creature
	 */
	public ConjurationSession dismiss(String creature, int number)
	{
		return new ConjurationSession(duration,
			conjured.stream().filter(found -> !found.is(creature, number)).toList(), groupBonuses);
	}

	/**
	 * @return this session with nobody in play and the same duration
	 */
	public ConjurationSession dismissAll()
	{
		return new ConjurationSession(duration, List.of(), groupBonuses);
	}

	/**
	 * @param rounds how many rounds a newly conjured creature stays; 0 not to
	 *               count them
	 * @return this session giving that duration to the creatures still to come
	 */
	public ConjurationSession withDuration(int rounds)
	{
		return new ConjurationSession(rounds, conjured, groupBonuses);
	}

	/**
	 * @param bonus a bonus to lay on every creature, now and to come; it replaces a group
	 *              bonus from the same source
	 * @return the same fight with that group bonus
	 */
	public ConjurationSession withGroupBonus(AppliedBonus bonus)
	{
		return new ConjurationSession(duration, conjured, AppliedBonus.adding(groupBonuses, bonus));
	}

	/**
	 * @param source the source of a group bonus to lift
	 * @return the same fight without it; a creature's own bonus from that source stays
	 */
	public ConjurationSession withoutGroupBonus(BonusSourceKey source)
	{
		return new ConjurationSession(duration, conjured, AppliedBonus.removing(groupBonuses, source));
	}

	/**
	 * @param creature the creature's name
	 * @param number   its number
	 * @param bonus    a bonus to lay on that creature alone
	 * @return the same fight with that bonus on that creature
	 */
	public ConjurationSession withBonus(String creature, int number, AppliedBonus bonus)
	{
		return changing(creature, number, found -> new ConjuredCreature(found.creature(), found.number(),
			found.damage(), found.roundsLeft(), AppliedBonus.adding(found.bonuses(), bonus)));
	}

	/**
	 * @param creature the creature's name
	 * @param number   its number
	 * @param source   the source of that creature's own bonus to lift
	 * @return the same fight without that bonus on that creature
	 */
	public ConjurationSession withoutBonus(String creature, int number, BonusSourceKey source)
	{
		return changing(creature, number, found -> new ConjuredCreature(found.creature(), found.number(),
			found.damage(), found.roundsLeft(), AppliedBonus.removing(found.bonuses(), source)));
	}

	private boolean holds(String creature, int number)
	{
		return conjured.stream().anyMatch(found -> found.is(creature, number));
	}

	private ConjurationSession changing(String creature, int number, UnaryOperator<ConjuredCreature> change)
	{
		return new ConjurationSession(duration,
			conjured.stream().map(found -> found.is(creature, number) ? change.apply(found) : found).toList(), groupBonuses);
	}

	private static ConjuredCreature oneRoundLater(ConjuredCreature creature)
	{
		if (creature.roundsLeft().isEmpty())
		{
			return creature;
		}
		int rounds = Math.max(creature.roundsLeft().getAsInt() - 1, 0);
		return new ConjuredCreature(creature.creature(), creature.number(), creature.damage(),
			OptionalInt.of(rounds), creature.bonuses());
	}

	private static String identityOf(String creature, int number)
	{
		return creature.toLowerCase(Locale.ROOT) + '#' + number;
	}
}
