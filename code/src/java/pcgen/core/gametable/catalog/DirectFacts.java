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
package pcgen.core.gametable.catalog;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;

import pcgen.cdom.base.CDOMObject;
import pcgen.cdom.content.ChallengeRating;
import pcgen.cdom.content.LevelCommandFactory;
import pcgen.cdom.enumeration.FactKey;
import pcgen.cdom.enumeration.ListKey;
import pcgen.cdom.enumeration.ObjectKey;
import pcgen.cdom.enumeration.RaceType;
import pcgen.cdom.enumeration.StringKey;
import pcgen.core.Kit;
import pcgen.core.Race;
import pcgen.core.gametable.BestiaryPawn;
import pcgen.core.gametable.BestiaryTactics;
import pcgen.core.gametable.MonsterSource;

/**
 * The values of a bestiary entry read in the data, without building the creature.
 */
public final class DirectFacts
{
	/** The category of a monster race. */
	public static final String MONSTER = "Monster";
	/** The category of a character file or an NPC kit. */
	public static final String NPC = "NPC";
	/** The category of a race players choose. */
	public static final String PLAYER_RACE = "Player race";

	/** The fact marking a race players choose; the game mode defines it when its sources load. */
	private static final String IS_PC = "IsPC";

	private DirectFacts()
	{
	}

	/**
	 * @param race a race of the loaded sources.
	 * @return what its data say of it.
	 */
	public static Map<CatalogField, FieldValue> ofRace(Race race)
	{
		Map<CatalogField, FieldValue> facts = common(race.getLocalizedName(), sourceOf(race), "race",
			new MonsterSource.BestiaryRace(race.getKeyName()));
		facts.put(CatalogField.CATEGORY, new FieldValue.Text(isPlayerRace(race) ? PLAYER_RACE : MONSTER));
		RaceType type = race.get(ObjectKey.RACETYPE);
		if (type != null)
		{
			facts.put(CatalogField.TYPE, new FieldValue.Items(Set.of(type.toString())));
		}
		facts.put(CatalogField.SUBTYPES, new FieldValue.Items(race.getSafeListFor(ListKey.RACESUBTYPE).stream()
			.map(Object::toString).collect(Collectors.toSet())));
		ChallengeRating rating = race.get(ObjectKey.CHALLENGE_RATING);
		if (rating != null)
		{
			number(rating.getRating().toString()).ifPresent(
				value -> facts.put(CatalogField.CHALLENGE_RATING, new FieldValue.Number(value)));
		}
		LevelCommandFactory racialClass = race.get(ObjectKey.MONSTER_CLASS);
		if (racialClass != null)
		{
			number(racialClass.getLevelCount().toString()).ifPresent(
				value -> facts.put(CatalogField.HIT_DICE, new FieldValue.Number(value)));
		}
		facts.put(CatalogField.HAS_TACTICS, new FieldValue.Flag(BestiaryTactics.of(race.getKeyName()).isPresent()));
		return facts;
	}

	/**
	 * @param kit an NPC kit of the loaded sources.
	 * @return what its data say of it.
	 */
	public static Map<CatalogField, FieldValue> ofKit(Kit kit)
	{
		Map<CatalogField, FieldValue> facts = common(kit.getLocalizedName(), sourceOf(kit), "kit",
			new MonsterSource.NpcKit(kit.getKeyName()));
		facts.put(CatalogField.CATEGORY, new FieldValue.Text(NPC));
		return facts;
	}

	/**
	 * @param pcg a character file.
	 * @return what is known of it without reading it.
	 */
	public static Map<CatalogField, FieldValue> ofFile(Path pcg)
	{
		String fileName = pcg.getFileName().toString();
		String name = fileName.endsWith(".pcg") ? fileName.substring(0, fileName.length() - ".pcg".length()) : fileName;
		Path folder = pcg.toAbsolutePath().getParent();
		Map<CatalogField, FieldValue> facts = common(name, folder == null ? "" : folder.toString(), "file",
			new MonsterSource.CharacterFile(pcg.toAbsolutePath().toString()));
		facts.put(CatalogField.CATEGORY, new FieldValue.Text(NPC));
		return facts;
	}

	private static Map<CatalogField, FieldValue> common(String name, String sourceBook, String origin, MonsterSource source)
	{
		Map<CatalogField, FieldValue> facts = new EnumMap<>(CatalogField.class);
		facts.put(CatalogField.NAME, new FieldValue.Text(name));
		facts.put(CatalogField.SOURCE_BOOK, new FieldValue.Text(sourceBook));
		facts.put(CatalogField.ORIGIN, new FieldValue.Text(origin));
		facts.put(CatalogField.HAS_PAWN, new FieldValue.Flag(BestiaryPawn.of(source).isPresent()));
		return facts;
	}

	private static boolean isPlayerRace(Race race)
	{
		return FactKey.getAllConstants().stream().filter(key -> IS_PC.equals(key.toString())).findFirst()
			.map(race::getResolved).map(isPc -> Boolean.parseBoolean(isPc.toString())).orElse(false);
	}

	private static String sourceOf(CDOMObject object)
	{
		String source = object.get(StringKey.SOURCE_LONG);
		return source == null ? "" : source;
	}

	private static OptionalDouble number(String text)
	{
		return Criterion.Test.number(text);
	}
}
