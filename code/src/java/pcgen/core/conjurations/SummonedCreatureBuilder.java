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
import java.util.Collection;
import java.util.List;

import pcgen.cdom.base.Constants;
import pcgen.core.AbilityCategory;
import pcgen.core.Campaign;
import pcgen.core.Globals;
import pcgen.core.Kit;
import pcgen.core.PCTemplate;
import pcgen.core.PlayerCharacter;
import pcgen.core.Race;
import pcgen.core.SettingsHandler;
import pcgen.core.character.EquipSet;
import pcgen.facade.core.UIDelegate;
import pcgen.io.ExportHandler;
import pcgen.rules.context.AbstractReferenceContext;
import pcgen.system.LanguageBundle;
import pcgen.util.chooser.ChooserFactory;

/**
 * Builds a conjured creature as a character of its own, so that PCGen computes
 * its numbers: its race, the Bestiary's default kit for that race, its
 * templates, then Augment Summoning when its conjurer has the feat. The
 * creature is never saved nor listed among the open characters.
 */
public final class SummonedCreatureBuilder
{
	/** The template PCGen's data lays on a creature its conjurer augments. */
	public static final String AUGMENT_SUMMONING = "Augment Summoning";

	private static final String DEFAULT_KIT_SUFFIX = " Default";
	private static final String DEFAULT_SET_NAME = "Default";
	private static final String WEAPON = "WEAPON.MERGENONE.";
	private static final Object BUILD_LOCK = new Object();
	private static final int MOST_ATTACKS = 20;
	private static final int FORTITUDE = 0;
	private static final int REFLEX = 1;
	private static final int WILL = 2;

	private final Collection<Campaign> campaigns;
	private final UIDelegate choices;

	/**
	 * @param campaigns the loaded sources the creatures are built on.
	 * @param choices   what answers the questions the data asks while a creature
	 *                  is built, in place of a dialog.
	 */
	public SummonedCreatureBuilder(Collection<Campaign> campaigns, UIDelegate choices)
	{
		this.campaigns = List.copyOf(campaigns);
		this.choices = choices;
	}

	/**
	 * @param conjurer the character conjuring the creatures.
	 * @return whether its creatures arrive augmented, which they do when it has
	 *         the feat Augment Summoning.
	 */
	public static boolean augments(PlayerCharacter conjurer)
	{
		return conjurer.hasAbilityKeyed(AbilityCategory.FEAT, AUGMENT_SUMMONING);
	}

	/**
	 * @param summon    the race and templates of the creature. They must be in the
	 *                  loaded sources: see {@link CatalogResolution}.
	 * @param augmented whether its conjurer augments it.
	 * @return the creature, with no temporary bonus yet.
	 */
	public PlayerCharacter build(SummonTemplate summon, boolean augmented)
	{
		// Building sets global chooser and hit point rules. Only one summon may
		// change those settings at a time, including when catalog work runs off the UI thread.
		synchronized (BUILD_LOCK)
		{
			return buildLocked(summon, augmented);
		}
	}

	private PlayerCharacter buildLocked(SummonTemplate summon, boolean augmented)
	{
		AbstractReferenceContext data = Globals.getContext().getReferenceContext();
		UIDelegate previousChoices = ChooserFactory.getDelegate();
		int previousHitPointRule = SettingsHandler.getHPRollMethod();
		ChooserFactory.setDelegate(choices);
		// The Bestiary gives a creature the average of its hit dice, never a roll
		SettingsHandler.setHPRollMethod(Constants.HP_AVERAGE);
		try
		{
			PlayerCharacter creature = new PlayerCharacter(campaigns);
			Globals.getPCList().remove(creature);
			creature.beginBatchMode();
			// A kit's gear needs a set to be equipped in, or the creature goes unarmed
			creature.addEquipSet(new EquipSet(EquipSet.DEFAULT_SET_PATH, DEFAULT_SET_NAME));
			creature.setCalcEquipSetId(EquipSet.DEFAULT_SET_PATH);
			creature.setRace(data.silentlyGetConstructedCDOMObject(Race.class, summon.sourceRaceKey()));
			String kit = summon.metadata().getOrDefault("kit", summon.sourceRaceKey() + DEFAULT_KIT_SUFFIX);
			Kit.applyKit(data.silentlyGetConstructedCDOMObject(Kit.class, kit),
				creature);
			List<String> templates = new ArrayList<>(summon.templateKeys());
			if (augmented && !"true".equals(summon.metadata().get("suppressAugment"))
				&& !templates.contains(AUGMENT_SUMMONING))
			{
				templates.add(AUGMENT_SUMMONING);
			}
			for (String template : templates)
			{
				creature.addTemplate(data.silentlyGetConstructedCDOMObject(PCTemplate.class, template));
			}
			creature.endBatchMode();
			return creature;
		}
		finally
		{
			SettingsHandler.setHPRollMethod(previousHitPointRule);
			ChooserFactory.setDelegate(previousChoices);
		}
	}

	/**
	 * @param creature a creature this builder built.
	 * @return its numbers, read as PCGen's output sheets read them.
	 */
	public CreatureStats statsOf(PlayerCharacter creature)
	{
		List<CreatureAttack> attacks = attacksOf(creature);
		int hitDice = creature.getDisplay().totalHitDice() + creature.getDisplay().totalNonMonsterLevels();
		return new CreatureStats(hitDice, token(creature, "MOVEMENT"), number(creature, "HP"), number(creature, "AC.Total"),
			number(creature, "AC.Touch"), number(creature, "AC.Flatfooted"), attacks,
			token(creature, "VAR.CMB.INTVAL.SIGN"), number(creature, "VAR.CMD.INTVAL"), save(creature, REFLEX),
			save(creature, FORTITUDE), save(creature, WILL), token(creature, "INITIATIVEMOD"),
			specialAbilitiesOf(creature).joined());
	}

	/**
	 * @param creature a creature this builder built.
	 * @return the special attacks and qualities PCGen lists for it, then its damage reduction and spell resistance.
	 */
	public static SpecialAbilities specialAbilitiesOf(PlayerCharacter creature)
	{
		List<String> defenses = new ArrayList<>();
		String damageReduction = token(creature, "DR");
		if (!damageReduction.isBlank())
		{
			defenses.add(LanguageBundle.getString("in_conjurations_damage_reduction") + " " + damageReduction);
		}
		String spellResistance = token(creature, "SR");
		if (!spellResistance.isBlank() && !"0".equals(spellResistance))
		{
			defenses.add(LanguageBundle.getString("in_conjurations_spell_resistance") + " " + spellResistance);
		}
		return new SpecialAbilities(specialAbilitiesTyped(creature, "SpecialAttack"),
			specialAbilitiesTyped(creature, "SpecialQuality"), defenses);
	}

	private static List<String> specialAbilitiesTyped(PlayerCharacter creature, String type)
	{
		return SpecialAbilities.split(token(creature, "ABILITYALLLIST.Special Ability.VISIBLE.TYPE=" + type));
	}

	/** Applies summoning-list effects that are not part of the creature's race or template. */
	public CreatureStats statsOf(PlayerCharacter creature, SummonTemplate summon)
	{
		CreatureStats stats = statsOf(creature);
		int bonus = Integer.parseInt(summon.metadata().getOrDefault("willBonus", "0"));
		if (bonus == 0)
		{
			return stats;
		}
		String will = stats.will();
		try
		{
			will = String.format("%+d", Integer.parseInt(will.strip()) + bonus);
		}
		catch (NumberFormatException ignored)
		{
			return stats;
		}
		return new CreatureStats(stats.hitDice(), stats.speed(), stats.maximumHitPoints(), stats.armorClass(),
			stats.touchArmorClass(), stats.flatFootedArmorClass(), stats.attacks(), stats.combatManeuverBonus(),
			stats.combatManeuverDefense(), stats.reflex(), stats.fortitude(), will, stats.initiative(),
			stats.specialAbilities());
	}

	private static List<CreatureAttack> attacksOf(PlayerCharacter creature)
	{
		List<CreatureAttack> attacks = new ArrayList<>();
		for (int index = 0; index < MOST_ATTACKS; index++)
		{
			String name = token(creature, WEAPON + index + ".NAME");
			if (name.isBlank())
			{
				break;
			}
			attacks.add(new CreatureAttack(name, token(creature, WEAPON + index + ".TOTALHIT"),
				token(creature, WEAPON + index + ".DAMAGE")));
		}
		return attacks;
	}

	private static String save(PlayerCharacter creature, int check)
	{
		return token(creature, "CHECK." + check + ".TOTAL");
	}

	private static String token(PlayerCharacter creature, String name)
	{
		String value = ExportHandler.getTokenString(creature, name);
		return (value == null) ? "" : value.strip();
	}

	private static int number(PlayerCharacter creature, String name)
	{
		try
		{
			return Integer.parseInt(token(creature, name).replace("+", ""));
		}
		catch (NumberFormatException notANumber)
		{
			return 0;
		}
	}
}
