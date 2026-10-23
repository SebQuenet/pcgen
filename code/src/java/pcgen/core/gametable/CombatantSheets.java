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

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import pcgen.cdom.base.CDOMObject;
import pcgen.cdom.content.CNAbility;
import pcgen.cdom.enumeration.ObjectKey;
import pcgen.core.Ability;
import pcgen.core.Equipment;
import pcgen.core.Globals;
import pcgen.core.PCStat;
import pcgen.core.PCTemplate;
import pcgen.core.PlayerCharacter;
import pcgen.core.Race;
import pcgen.core.bonus.TemporaryBonusApplier;
import pcgen.core.bonus.TemporaryBonusSources;
import pcgen.core.conjurations.AppliedBonus;
import pcgen.core.conjurations.BonusSourceKey;
import pcgen.core.conjurations.CreatureStats;
import pcgen.core.conjurations.SpecialAbilities;
import pcgen.core.conjurations.SummonedCreatureBuilder;
import pcgen.core.spell.Spell;
import pcgen.facade.core.BonusChoice;
import pcgen.io.ExportHandler;
import pcgen.util.FirstResultSearch;
import pcgen.util.enumeration.Visibility;

/**
 * Reads what the game table shows of a character PCGen computed, and lays the
 * conditions drawn from the data on a monster.
 */
public final class CombatantSheets
{
	/** The ability that gives a creature a second turn each round, twenty lower. */
	public static final String DUAL_INITIATIVE = "Mythic Simple Template ~ Dual Initiative";

	private static final String INTELLIGENCE = "INT";
	private static final String WISDOM = "WIS";
	private static final String HUMANOID = "Humanoid";
	private static final String SENSE_MOTIVE = "SKILL.Sense Motive";
	private static final String FEATS = "ABILITYALLLIST.FEAT.VISIBLE";

	private final SummonedCreatureBuilder reader;

	/**
	 * @param reader reads a creature's numbers as PCGen's output sheets read them.
	 */
	public CombatantSheets(SummonedCreatureBuilder reader)
	{
		this.reader = reader;
	}

	/**
	 * @param monster a monster PCGen computed.
	 * @return the numbers its sheet shows in a fight.
	 */
	public CreatureStats stats(PlayerCharacter monster)
	{
		return reader.statsOf(monster);
	}

	/**
	 * @param monster a monster PCGen computed.
	 * @return what its sheet shows in a fight.
	 */
	public MonsterSheet sheet(PlayerCharacter monster)
	{
		return new MonsterSheet(stats(monster), challengeRating(monster),
			SummonedCreatureBuilder.specialAbilitiesOf(monster), SpecialAbilities.split(token(monster, FEATS)),
			feint(monster), referencesOf(monster.getRace()));
	}

	private static List<FirstResultSearch.Link> referencesOf(Race race)
	{
		if (race == null || race.isUnselected())
		{
			return List.of();
		}
		return FirstResultSearch.creatureLinks(race.getDisplayName(), race.getLocalizedName());
	}

	/**
	 * @param monster a monster PCGen computed.
	 * @return what a Bluff check must reach to feint it.
	 */
	public FeintDefense feint(PlayerCharacter monster)
	{
		Optional<PCStat> intelligence = stat(monster, INTELLIGENCE);
		OptionalInt intelligenceScore = intelligence.filter(found -> !monster.isNonAbility(found))
			.map(found -> OptionalInt.of(monster.getTotalStatFor(found))).orElseGet(OptionalInt::empty);
		int wisdomModifier = stat(monster, WISDOM).map(monster::getStatModFor).orElse(0);
		OptionalInt trainedSenseMotive = number(token(monster, SENSE_MOTIVE + ".RANK")).orElse(0) > 0
			? number(token(monster, SENSE_MOTIVE + ".TOTAL")).map(OptionalInt::of).orElseGet(OptionalInt::empty)
			: OptionalInt.empty();
		return FeintDefense.of(monster.baseAttackBonus(), wisdomModifier, trainedSenseMotive, intelligenceScore,
			HUMANOID.equalsIgnoreCase(monster.getDisplay().getRaceType()));
	}

	private static Optional<PCStat> stat(PlayerCharacter character, String key)
	{
		return character.getStatSet().stream().filter(found -> key.equals(found.getKeyName())).findFirst();
	}

	private static Optional<Integer> number(String written)
	{
		try
		{
			return Optional.of((int) Double.parseDouble(written.replace("+", "")));
		}
		catch (NumberFormatException notANumber)
		{
			return Optional.empty();
		}
	}

	/**
	 * @param character a player's character.
	 * @return the few numbers the game master reads of it.
	 */
	public PlayerGlance glance(PlayerCharacter character)
	{
		return new PlayerGlance(token(character, "SKILL.Perception.TOTAL.SIGN"),
			token(character, SENSE_MOTIVE + ".TOTAL.SIGN"), token(character, "CHECK.0.TOTAL"),
			token(character, "CHECK.1.TOTAL"), token(character, "CHECK.2.TOTAL"));
	}

	/**
	 * @param monster a monster PCGen computed.
	 * @return its challenge rating as PCGen writes it, such as "1/3".
	 */
	public String challengeRating(PlayerCharacter monster)
	{
		return token(monster, "CR");
	}

	/**
	 * @param monster a monster PCGen computed.
	 * @return its initiative modifier.
	 */
	public int initiativeModifier(PlayerCharacter monster)
	{
		try
		{
			return Integer.parseInt(token(monster, "INITIATIVEMOD").replace("+", ""));
		}
		catch (NumberFormatException notANumber)
		{
			return 0;
		}
	}

	/**
	 * @param character a character PCGen computed.
	 * @return whether it acts twice a round.
	 */
	public boolean hasDualInitiative(PlayerCharacter character)
	{
		return character.getCNAbilities().stream().map(CNAbility::getAbilityKey).anyMatch(DUAL_INITIATIVE::equals);
	}

	/**
	 * @param monster   a monster PCGen computed.
	 * @param laid      the bonuses already laid on it.
	 * @param wanted    the conditions it should be under now.
	 * @return the bonuses laid once done: those of the wanted conditions the data holds.
	 */
	public Set<AppliedBonus> lay(PlayerCharacter monster, Set<AppliedBonus> laid, List<Condition> wanted)
	{
		Set<AppliedBonus> wantedBonuses = wanted.stream().map(Condition::applied).flatMap(Optional::stream)
			.collect(Collectors.toSet());
		Set<AppliedBonus> nowLaid = new HashSet<>(laid);
		for (AppliedBonus bonus : laid)
		{
			if (!wantedBonuses.contains(bonus))
			{
				TemporaryBonusSources.find(bonus.source())
					.ifPresent(source -> TemporaryBonusApplier.remove(monster, source));
				nowLaid.remove(bonus);
			}
		}
		for (AppliedBonus bonus : wantedBonuses)
		{
			if (!nowLaid.contains(bonus))
			{
				Optional<CDOMObject> source = TemporaryBonusSources.find(bonus.source());
				if (source.isPresent())
				{
					TemporaryBonusApplier.apply(monster, source.get(), bonus.chosenValue());
					nowLaid.add(bonus);
				}
			}
		}
		return nowLaid;
	}

	/**
	 * @param character a character, or nothing for a player's character the game master lacks.
	 * @return the conditions and bonuses the data holds, by name, for the condition picker:
	 *         what the character can lay on itself, then what anyone can lay on another.
	 */
	public static Map<String, BonusSourceKey> conditionSources(Optional<PlayerCharacter> character)
	{
		return keyableSources(character).stream().collect(Collectors.toMap(CDOMObject::getLocalizedName, TemporaryBonusSources::keyOf, (first, second) -> first,
				LinkedHashMap::new));
	}

	/**
	 * @param character the one character the condition goes on, or nothing for several or for a player's character.
	 * @param reader    a monster whose variables the value ranges read, when the condition goes on one.
	 * @return the conditions and bonuses the data holds, with the values a bonus asks for, as the conjurations offer.
	 */
	public static List<BonusChoice> conditionChoices(Optional<PlayerCharacter> character,
		Optional<PlayerCharacter> reader)
	{
		return keyableSources(character).stream().map(source -> new BonusChoice(TemporaryBonusSources.keyOf(source)
			.text(), source.getLocalizedName(), reader.flatMap(found -> TemporaryBonusApplier.choiceRange(found,
				source)))).toList();
	}

	private static List<CDOMObject> keyableSources(Optional<PlayerCharacter> character)
	{
		List<CDOMObject> sources = character.map(TemporaryBonusSources::availableTo)
			.orElseGet(TemporaryBonusSources::castableOnOthers);
		return sources.stream().filter(CombatantSheets::canBeKeyed)
			.sorted(Comparator.comparing(CDOMObject::getLocalizedName, String.CASE_INSENSITIVE_ORDER)).toList();
	}

	/** Skills also grant temporary bonuses, but a fight's condition comes from the kinds a bonus key names. */
	private static boolean canBeKeyed(CDOMObject source)
	{
		return source instanceof Ability || source instanceof Spell || source instanceof PCTemplate
			|| source instanceof Equipment;
	}

	/**
	 * @return the races of the loaded sources, by name.
	 */
	public static Map<String, String> races()
	{
		return byName(Globals.getContext().getReferenceContext().getConstructedCDOMObjects(Race.class).stream()
			.filter(race -> !race.isUnselected()).toList(), Race::getKeyName);
	}

	/**
	 * @return the templates of the loaded sources a game master can lay on a monster, by name.
	 */
	public static Map<String, String> templates()
	{
		return byName(Globals.getContext().getReferenceContext().getConstructedCDOMObjects(PCTemplate.class).stream()
			.filter(template -> template.getSafe(ObjectKey.VISIBILITY) != Visibility.HIDDEN).toList(),
			PCTemplate::getKeyName);
	}

	private static <T extends CDOMObject> Map<String, String> byName(List<T> objects, Function<T, String> key)
	{
		return objects.stream().sorted(Comparator.comparing(CDOMObject::getLocalizedName, String.CASE_INSENSITIVE_ORDER))
			.collect(Collectors.toMap(CDOMObject::getLocalizedName, key, (first, second) -> first,
				LinkedHashMap::new));
	}

	private static String token(PlayerCharacter character, String name)
	{
		String value = ExportHandler.getTokenString(character, name);
		return (value == null) ? "" : value.strip();
	}
}
