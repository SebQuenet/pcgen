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
package pcgen.core.gametable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import pcgen.core.PCClass;
import pcgen.core.PObject;
import pcgen.core.PlayerCharacter;
import pcgen.core.Globals;
import pcgen.core.Skill;
import pcgen.core.analysis.SkillModifier;
import pcgen.core.analysis.SkillRankControl;
import pcgen.core.character.CharacterSpell;
import pcgen.core.character.SpellInfo;

/**
 * The skills a monster trained and the spells it can cast, as a stat block lists them.
 */
public final class MonsterRepertoire
{
	private MonsterRepertoire()
	{
	}

	/**
	 * A skill and the total of its check.
	 *
	 * @param name  the skill's name.
	 * @param total what the monster adds to a d20 for it.
	 */
	public record SkillTotal(String name, int total)
	{
	}

	/** The spells of one caster: a class, which prepares or knows them by level, or the race, which has them innately. */
	public sealed interface CasterSpells
	{
		/** @return the class or race that casts them. */
		String caster();

		/**
		 * @param caster the race.
		 * @param spells its spell-like abilities.
		 */
		record Innate(String caster, List<InnateSpell> spells) implements CasterSpells
		{
			public Innate
			{
				spells = List.copyOf(spells);
			}
		}

		/**
		 * @param caster the class.
		 * @param spells its spells prepared or, for a class that prepares none, known.
		 */
		record Levelled(String caster, List<LevelledSpell> spells) implements CasterSpells
		{
			public Levelled
			{
				spells = List.copyOf(spells);
			}
		}
	}

	/**
	 * @param name     the spell's name.
	 * @param times    how often it can be cast, {@link SpellInfo#TIMES_AT_WILL} when at will.
	 * @param timeUnit the period those times run over, such as Day; Constant for an effect always on.
	 */
	public record InnateSpell(String name, int times, String timeUnit)
	{
	}

	/**
	 * @param name  the spell's name.
	 * @param level its level for the class.
	 * @param times how many times it is prepared, or 1 when known.
	 */
	public record LevelledSpell(String name, int level, int times)
	{
	}

	/**
	 * @param monster a monster PCGen computed.
	 * @return the skills it has ranks in, the best check first.
	 */
	public static List<SkillTotal> skillsOf(PlayerCharacter monster)
	{
		return monster.getSkillSet().stream().filter(skill -> SkillRankControl.getTotalRank(monster, skill) > 0)
			.map(skill -> new SkillTotal(skill.getLocalizedName(), totalOf(monster, skill)))
			.sorted(Comparator.comparingInt(SkillTotal::total).reversed().thenComparing(SkillTotal::name)).toList();
	}

	private static int totalOf(PlayerCharacter monster, Skill skill)
	{
		return SkillRankControl.getTotalRank(monster, skill).intValue() + SkillModifier.modifier(skill, monster);
	}

	/**
	 * @param monster a monster PCGen computed.
	 * @return the spells of each of its casters that has any.
	 */
	public static List<CasterSpells> spellsOf(PlayerCharacter monster)
	{
		List<CasterSpells> casters = new ArrayList<>();
		for (PObject caster : monster.getSpellClassList())
		{
			List<SpellInfo> castable = castable(monster, caster);
			if (castable.isEmpty())
			{
				continue;
			}
			if (caster instanceof PCClass)
			{
				casters.add(new CasterSpells.Levelled(caster.getDisplayName(), castable.stream()
					.map(info -> new LevelledSpell(nameOf(info), info.getActualLevel(), Math.max(1, info.getTimes())))
					.sorted(Comparator.comparingInt(LevelledSpell::level).thenComparing(spell -> spell.name().toLowerCase(Locale.ROOT)))
					.toList()));
			}
			else
			{
				casters.add(new CasterSpells.Innate(caster.getDisplayName(), castable.stream()
					.map(info -> new InnateSpell(nameOf(info), info.getTimes(), info.getTimeUnit()))
					.sorted(Comparator.comparing(spell -> spell.name().toLowerCase(Locale.ROOT))).toList()));
			}
		}
		return List.copyOf(casters);
	}

	/** The spells the caster has ready: those prepared, or those known when it prepares none. */
	private static List<SpellInfo> castable(PlayerCharacter monster, PObject caster)
	{
		String knownBook = Globals.getDefaultSpellBook();
		List<SpellInfo> known = new ArrayList<>();
		List<SpellInfo> ready = new ArrayList<>();
		for (CharacterSpell spell : monster.getCharacterSpells(caster))
		{
			for (SpellInfo info : spell.getInfoList())
			{
				(knownBook.equals(info.getBook()) ? known : ready).add(info);
			}
		}
		return ready.isEmpty() ? known : ready;
	}

	private static String nameOf(SpellInfo info)
	{
		return info.getOwner().getSpell().getDisplayName();
	}
}
