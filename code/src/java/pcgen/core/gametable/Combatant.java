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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * One entry of the fight. A monster carries the recipe it is rebuilt from and the hit
 * points the game master tracks; a player's character carries neither.
 *
 * @param id            unique within the fight
 * @param name          what the list shows, such as "Goblin 2". Never blank.
 * @param group         combatants sharing one initiative, such as the goblins of an
 *                      encounter; blank for one acting alone
 * @param initiative    its place in the order
 * @param health        its hit points, present exactly when it is a monster
 * @param conditions    its conditions, temporary bonuses and labels
 * @param recipe        how to rebuild it, present exactly when it is a monster
 * @param characterFile the player's saved character, read for a glance at a few numbers
 * @param summonerId    the conjurer's id, for a conjured creature
 * @param roundsLeft    rounds before a conjured creature leaves
 * @param delaying      whether it has stepped out of the order until it chooses to act
 */
public record Combatant(int id, String name, String group, Initiative initiative, Optional<Health> health,
	List<Condition> conditions, Optional<MonsterRecipe> recipe, Optional<String> characterFile, OptionalInt summonerId,
	OptionalInt roundsLeft, boolean delaying)
{
	public Combatant
	{
		if (Objects.requireNonNull(name).isBlank())
		{
			throw new IllegalArgumentException("A combatant has a name");
		}
		name = name.strip();
		group = Objects.requireNonNull(group).strip();
		Objects.requireNonNull(initiative);
		conditions = List.copyOf(conditions);
		Objects.requireNonNull(characterFile);
		Objects.requireNonNull(summonerId);
		Objects.requireNonNull(roundsLeft);
		if (health.isPresent() != recipe.isPresent())
		{
			throw new IllegalArgumentException("A monster, and only a monster, has hit points and a recipe: " + name);
		}
	}

	/**
	 * @param id            unique within the fight.
	 * @param name          the character's name.
	 * @param characterFile the player's saved character, if the game master has it.
	 * @return a player's character, waiting for the initiative its player announces.
	 */
	public static Combatant player(int id, String name, Optional<String> characterFile)
	{
		return new Combatant(id, name, "", new Initiative(OptionalInt.empty(), 0, false), Optional.empty(), List.of(),
			Optional.empty(), characterFile, OptionalInt.empty(), OptionalInt.empty(), false);
	}

	/**
	 * @param id                 unique within the fight.
	 * @param name               the name the list shows.
	 * @param group              combatants sharing its initiative.
	 * @param recipe             how to rebuild it.
	 * @param maximumHitPoints   its full hit points.
	 * @param initiativeModifier its initiative modifier.
	 * @param dual               whether it acts twice a round.
	 * @return an unhurt monster, its initiative not rolled yet.
	 */
	public static Combatant monster(int id, String name, String group, MonsterRecipe recipe, int maximumHitPoints,
		int initiativeModifier, boolean dual)
	{
		return new Combatant(id, name, group, new Initiative(OptionalInt.empty(), initiativeModifier, dual),
			Optional.of(new Health(maximumHitPoints, 0)), List.of(), Optional.of(recipe), Optional.empty(),
			OptionalInt.empty(), OptionalInt.empty(), false);
	}

	/**
	 * @return who it is to the game master.
	 */
	public CombatantKind kind()
	{
		if (recipe.isPresent())
		{
			return summonerId.isPresent() ? CombatantKind.MONSTER_SUMMON : CombatantKind.MONSTER;
		}
		return summonerId.isPresent() ? CombatantKind.PLAYER_SUMMON : CombatantKind.PLAYER;
	}

	public Combatant withInitiative(Initiative newInitiative)
	{
		return new Combatant(id, name, group, newInitiative, health, conditions, recipe, characterFile, summonerId,
			roundsLeft, delaying);
	}

	public Combatant withHealth(Health newHealth)
	{
		return new Combatant(id, name, group, initiative, Optional.of(newHealth), conditions, recipe, characterFile,
			summonerId, roundsLeft, delaying);
	}

	public Combatant withConditions(List<Condition> newConditions)
	{
		return new Combatant(id, name, group, initiative, health, newConditions, recipe, characterFile, summonerId,
			roundsLeft, delaying);
	}

	/**
	 * @param newRecipe the monster after a touch.
	 * @param maximum   its full hit points after the touch; the damage taken stays.
	 * @param modifier  its initiative modifier after the touch.
	 * @param dual      whether it acts twice a round after the touch.
	 * @return this monster, touched.
	 */
	public Combatant withRecipe(MonsterRecipe newRecipe, int maximum, int modifier, boolean dual)
	{
		Health touched = health.orElseThrow(() -> new IllegalStateException(name + " is not a monster")).withMaximum(
			maximum);
		return new Combatant(id, name, group, new Initiative(initiative.score(), modifier, dual), Optional.of(touched),
			conditions, Optional.of(newRecipe), characterFile, summonerId, roundsLeft, delaying);
	}

	public Combatant withRoundsLeft(OptionalInt newRoundsLeft)
	{
		return new Combatant(id, name, group, initiative, health, conditions, recipe, characterFile, summonerId,
			newRoundsLeft, delaying);
	}

	public Combatant withSummoner(int conjurerId, String newGroup, Initiative conjurerInitiative)
	{
		return new Combatant(id, name, newGroup,
			new Initiative(conjurerInitiative.score(), conjurerInitiative.modifier(), initiative.dual()), health,
			conditions, recipe, characterFile, OptionalInt.of(conjurerId), roundsLeft, delaying);
	}

	public Combatant withDelaying(boolean newDelaying)
	{
		return new Combatant(id, name, group, initiative, health, conditions, recipe, characterFile, summonerId,
			roundsLeft, newDelaying);
	}

	/**
	 * @param condition a condition to lay; one of the same name is replaced.
	 * @return this combatant under that condition.
	 */
	public Combatant withCondition(Condition condition)
	{
		List<Condition> laid = new ArrayList<>(conditions);
		laid.removeIf(existing -> existing.name().equalsIgnoreCase(condition.name()));
		laid.add(condition);
		return withConditions(laid);
	}

	/**
	 * @param conditionName the name of a condition to take off.
	 * @return this combatant without it.
	 */
	public Combatant withoutCondition(String conditionName)
	{
		return withConditions(
			conditions.stream().filter(existing -> !existing.name().equalsIgnoreCase(conditionName.strip())).toList());
	}
}
