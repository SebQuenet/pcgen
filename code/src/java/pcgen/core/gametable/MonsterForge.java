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

import java.io.File;
import java.util.Collection;
import java.util.List;

import pcgen.cdom.base.Constants;
import pcgen.cdom.content.LevelCommandFactory;
import pcgen.cdom.enumeration.ObjectKey;
import pcgen.core.Campaign;
import pcgen.core.Globals;
import pcgen.core.PCTemplate;
import pcgen.core.PlayerCharacter;
import pcgen.core.Race;
import pcgen.core.SettingsHandler;
import pcgen.core.conjurations.SummonTemplate;
import pcgen.core.conjurations.SummonedCreatureBuilder;
import pcgen.facade.core.UIDelegate;
import pcgen.io.PCGIOHandler;
import pcgen.rules.context.AbstractReferenceContext;
import pcgen.system.LanguageBundle;
import pcgen.util.chooser.ChooserFactory;

/**
 * Builds a monster from its recipe: from a bestiary race as a conjured creature is
 * built, or from a saved character, then with the game master's touches. The source
 * file is read, never written.
 */
public final class MonsterForge
{
	private static final Object FORGE_LOCK = new Object();

	private final Collection<Campaign> campaigns;
	private final UIDelegate choices;
	private final SummonedCreatureBuilder builder;

	/**
	 * @param campaigns the loaded sources the monsters are built on.
	 * @param choices   what answers the questions the data asks while a monster is built.
	 */
	public MonsterForge(Collection<Campaign> campaigns, UIDelegate choices)
	{
		this.campaigns = List.copyOf(campaigns);
		this.choices = choices;
		this.builder = new SummonedCreatureBuilder(campaigns, choices);
	}

	/**
	 * @return the builder this forge builds bestiary monsters with, which also reads their numbers.
	 */
	public SummonedCreatureBuilder builder()
	{
		return builder;
	}

	/**
	 * @param recipe how to build the monster.
	 * @return the monster, or what the loaded sources or the disk lack to build it.
	 */
	public ForgedMonster forge(MonsterRecipe recipe)
	{
		AbstractReferenceContext data = Globals.getContext().getReferenceContext();
		for (String template : recipe.addedTemplates())
		{
			if (data.silentlyGetConstructedCDOMObject(PCTemplate.class, template) == null)
			{
				return new ForgedMonster.Missing(LanguageBundle.getFormattedString("in_gametable_missingTemplate", template));
			}
		}
		synchronized (FORGE_LOCK)
		{
			ForgedMonster started = switch (recipe.source())
			{
				case MonsterSource.BestiaryRace race -> fromRace(data, race.raceKey(), recipe.addedTemplates());
				case MonsterSource.CharacterFile file -> fromFile(data, file.path(), recipe.addedTemplates());
			};
			if (started instanceof ForgedMonster.Forged forged)
			{
				return touched(forged.character(), data, recipe);
			}
			return started;
		}
	}

	private ForgedMonster fromRace(AbstractReferenceContext data, String raceKey, List<String> templates)
	{
		if (data.silentlyGetConstructedCDOMObject(Race.class, raceKey) == null)
		{
			return new ForgedMonster.Missing(LanguageBundle.getFormattedString("in_gametable_missingRace", raceKey));
		}
		return new ForgedMonster.Forged(builder.build(new SummonTemplate(raceKey, templates, 0), false));
	}

	private ForgedMonster fromFile(AbstractReferenceContext data, String path, List<String> templates)
	{
		File file = new File(path);
		if (!file.isFile())
		{
			return new ForgedMonster.Missing(LanguageBundle.getFormattedString("in_gametable_missingFile", path));
		}
		UIDelegate previousChoices = ChooserFactory.getDelegate();
		ChooserFactory.setDelegate(choices);
		try
		{
			PlayerCharacter monster = new PlayerCharacter(campaigns);
			Globals.getPCList().remove(monster);
			PCGIOHandler reader = new PCGIOHandler();
			reader.read(monster, file.getAbsolutePath());
			if (!reader.getErrors().isEmpty())
			{
				return new ForgedMonster.Missing(LanguageBundle.getFormattedString("in_gametable_unreadableFile", path,
					String.join("; ", reader.getErrors())));
			}
			Globals.getPCList().remove(monster);
			for (String template : templates)
			{
				monster.addTemplate(data.silentlyGetConstructedCDOMObject(PCTemplate.class, template));
			}
			return new ForgedMonster.Forged(monster);
		}
		finally
		{
			ChooserFactory.setDelegate(previousChoices);
		}
	}

	private ForgedMonster touched(PlayerCharacter monster, AbstractReferenceContext data, MonsterRecipe recipe)
	{
		for (String template : recipe.removedTemplates())
		{
			PCTemplate removed = data.silentlyGetConstructedCDOMObject(PCTemplate.class, template);
			if (removed != null && monster.getDisplay().hasTemplate(removed))
			{
				monster.removeTemplate(removed);
			}
		}
		if (recipe.addedHitDice() == 0)
		{
			return new ForgedMonster.Forged(monster);
		}
		LevelCommandFactory racialClass = monster.getRace().get(ObjectKey.MONSTER_CLASS);
		if (racialClass == null)
		{
			return new ForgedMonster.Missing(
				LanguageBundle.getFormattedString("in_gametable_noRacialHitDice", monster.getRace().getDisplayName()));
		}
		UIDelegate previousChoices = ChooserFactory.getDelegate();
		int previousHitPointRule = SettingsHandler.getHPRollMethod();
		ChooserFactory.setDelegate(choices);
		// Monsters take the average of their hit dice, as the Bestiary prints them
		SettingsHandler.setHPRollMethod(Constants.HP_AVERAGE);
		try
		{
			monster.incrementClassLevel(recipe.addedHitDice(), racialClass.getPCClass(), true);
			return new ForgedMonster.Forged(monster);
		}
		finally
		{
			SettingsHandler.setHPRollMethod(previousHitPointRule);
			ChooserFactory.setDelegate(previousChoices);
		}
	}
}
