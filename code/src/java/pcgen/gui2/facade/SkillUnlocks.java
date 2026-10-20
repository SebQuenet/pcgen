/*
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
package pcgen.gui2.facade;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pcgen.core.Ability;
import pcgen.core.AbilityCategory;
import pcgen.core.Globals;
import pcgen.core.Skill;

/**
 * Finds the Pathfinder Unchained skill unlocks of a skill. The data keys each tier
 * "Skill unlock ~ &lt;base skill&gt; &lt;ranks&gt; Ranks".
 */
final class SkillUnlocks
{
	record Tier(int ranks, Ability ability)
	{
	}

	private static final String SPECIAL_ABILITY_CATEGORY = "Special Ability";
	private static final Pattern TIER_KEY = Pattern.compile("Skill unlock ~ (.+) (\\d+) Ranks", Pattern.CASE_INSENSITIVE);
	private static final String SUBTYPE_OPENING = " (";

	private SkillUnlocks()
	{
	}

	static List<Tier> of(Skill skill)
	{
		AbilityCategory specialAbilities = Globals.getContext().getReferenceContext()
			.silentlyGetConstructedCDOMObject(AbilityCategory.class, SPECIAL_ABILITY_CATEGORY);
		if (specialAbilities == null)
		{
			return List.of();
		}
		String baseSkill = baseSkillName(skill);
		List<Tier> tiers = new ArrayList<>();
		for (Ability ability : Globals.getContext().getReferenceContext().getManufacturerId(specialAbilities).getAllObjects())
		{
			Matcher tierKey = TIER_KEY.matcher(ability.getKeyName());
			if (tierKey.matches() && tierKey.group(1).equalsIgnoreCase(baseSkill))
			{
				tiers.add(new Tier(Integer.parseInt(tierKey.group(2)), ability));
			}
		}
		tiers.sort(Comparator.comparingInt(Tier::ranks));
		return tiers;
	}

	private static String baseSkillName(Skill skill)
	{
		String key = skill.getKeyName();
		int subtypeStart = key.indexOf(SUBTYPE_OPENING);
		return subtypeStart < 0 ? key : key.substring(0, subtypeStart);
	}
}
