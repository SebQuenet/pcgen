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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import pcgen.cdom.base.CDOMObject;
import pcgen.core.PlayerCharacter;
import pcgen.core.bonus.TemporaryBonusApplier;
import pcgen.core.bonus.TemporaryBonusSources;

/**
 * The creatures one conjurer has in play, each built once as a character and
 * kept, since building one takes seconds. Laying or lifting a bonus changes
 * the kept creature rather than building it again.
 */
public final class ConjuredCreatures
{
	private final SummonedCreatureBuilder builder;
	private final Map<String, Kept> kept = new HashMap<>();

	/**
	 * @param builder what builds a creature the first time it is asked for.
	 */
	public ConjuredCreatures(SummonedCreatureBuilder builder)
	{
		this.builder = builder;
	}

	/**
	 * @param summon       the catalog entry the creature was conjured from.
	 * @param augmented    whether its conjurer augments it.
	 * @param creature     the creature in play.
	 * @param groupBonuses the bonuses laid on every creature of the fight.
	 * @return the creature, carrying exactly the group bonuses and its own; where both
	 *         come from the same source, its own value is the one laid.
	 */
	public BuiltCreature creature(SummonTemplate summon, boolean augmented, ConjuredCreature creature,
		List<AppliedBonus> groupBonuses)
	{
		String identity = identityOf(creature.creature(), creature.number());
		Kept found = kept.get(identity);
		if (found == null || !found.summon().equals(summon) || found.augmented() != augmented)
		{
			PlayerCharacter built = builder.build(summon, augmented);
			found = new Kept(summon, augmented, built, builder.statsOf(built), new LinkedHashMap<>());
			kept.put(identity, found);
		}
		Map<BonusSourceKey, AppliedBonus> wanted = new LinkedHashMap<>();
		groupBonuses.forEach(bonus -> wanted.put(bonus.source(), bonus));
		creature.bonuses().forEach(bonus -> wanted.put(bonus.source(), bonus));
		lay(found, wanted);
		return new BuiltCreature(found.character(), found.baseStats(), builder.statsOf(found.character()));
	}

	/**
	 * Lets go of a creature dismissed from the fight.
	 *
	 * @param creature its name
	 * @param number   its number
	 */
	public void forget(String creature, int number)
	{
		kept.remove(identityOf(creature, number));
	}

	/**
	 * Lets go of every creature, when the fight is cleared.
	 */
	public void forgetAll()
	{
		kept.clear();
	}

	private static void lay(Kept found, Map<BonusSourceKey, AppliedBonus> wanted)
	{
		for (AppliedBonus laid : List.copyOf(found.laid().values()))
		{
			if (!laid.equals(wanted.get(laid.source())))
			{
				TemporaryBonusSources.find(laid.source())
					.ifPresent(source -> TemporaryBonusApplier.remove(found.character(), source));
				found.laid().remove(laid.source());
			}
		}
		for (AppliedBonus bonus : wanted.values())
		{
			if (found.laid().containsKey(bonus.source()))
			{
				continue;
			}
			Optional<CDOMObject> source = TemporaryBonusSources.find(bonus.source());
			if (source.isPresent())
			{
				TemporaryBonusApplier.apply(found.character(), source.get(), bonus.chosenValue());
				found.laid().put(bonus.source(), bonus);
			}
		}
	}

	private static String identityOf(String creature, int number)
	{
		return creature.strip().toLowerCase(Locale.ROOT) + '#' + number;
	}

	/**
	 * A creature kept in memory: what it was built from, its numbers before any
	 * bonus, and the bonuses laid on it now.
	 */
	private record Kept(SummonTemplate summon, boolean augmented, PlayerCharacter character,
		CreatureStats baseStats, Map<BonusSourceKey, AppliedBonus> laid)
	{
	}
}
