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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.function.UnaryOperator;

/**
 * A fight the game master runs: who acts in which order, whose turn it is, and the
 * round. It never changes; each action gives the fight that follows.
 *
 * @param round      the round being played; zero until the fight starts
 * @param order      the combatants in acting order, those delaying included
 * @param current    the turn being played; empty before the fight starts and once the
 *                   last to act in a round has left
 * @param experience the experience points of the monsters that entered the fight
 */
public record Combat(int round, List<Combatant> order, Optional<Turn> current, long experience)
{
	public Combat
	{
		if (round < 0)
		{
			throw new IllegalArgumentException("A round is never negative: " + round);
		}
		order = List.copyOf(order);
		Objects.requireNonNull(current);
		Set<Integer> ids = new HashSet<>();
		for (Combatant combatant : order)
		{
			if (!ids.add(combatant.id()))
			{
				throw new IllegalArgumentException("Two combatants share the id " + combatant.id());
			}
		}
	}

	/**
	 * @return a fight with nobody in it.
	 */
	public static Combat empty()
	{
		return new Combat(0, List.of(), Optional.empty(), 0);
	}

	/**
	 * @return whether the first round has begun.
	 */
	public boolean isStarted()
	{
		return round > 0;
	}

	/**
	 * @param id a combatant's id.
	 * @return that combatant, or nothing when it is not in the fight.
	 */
	public Optional<Combatant> combatant(int id)
	{
		return order.stream().filter(combatant -> combatant.id() == id).findFirst();
	}

	/**
	 * @return an id no combatant of the fight holds.
	 */
	public int nextId()
	{
		return order.stream().mapToInt(Combatant::id).max().orElse(0) + 1;
	}

	/**
	 * @param added experience points of monsters entering the fight.
	 * @return this fight, worth that much more.
	 */
	public Combat withExperience(long added)
	{
		return new Combat(round, order, current, experience + added);
	}

	/**
	 * @param combatant a newcomer; it takes its place by initiative when it has one, last otherwise.
	 * @return this fight with the newcomer.
	 */
	public Combat with(Combatant combatant)
	{
		List<Combatant> combatants = new ArrayList<>(order);
		insertByInitiative(combatants, combatant);
		return new Combat(round, combatants, current, experience);
	}

	/**
	 * @param id    a combatant's id.
	 * @param score the initiative rolled or announced.
	 * @return this fight with that score given to the combatant and the rest of its group,
	 *         which move to their place, each followed by the creatures it conjured; the
	 *         others keep theirs.
	 */
	public Combat withInitiative(int id, int score)
	{
		Combatant target = require(id);
		List<Combatant> troop = order.stream().filter(combatant -> sameTroop(target, combatant))
			.map(combatant -> combatant.withInitiative(combatant.initiative().withScore(score))).toList();
		List<Combatant> combatants = new ArrayList<>(order);
		combatants.removeIf(combatant -> sameTroop(target, combatant) || conjuredByTroop(troop, combatant));
		for (Combatant member : troop)
		{
			insertByInitiative(combatants, member);
			int at = combatants.indexOf(member) + 1;
			for (Combatant conjured : conjuredBy(member))
			{
				combatants.add(at++, conjured.withInitiative(conjured.initiative().withScore(score)));
			}
		}
		return new Combat(round, combatants, current, experience);
	}

	private List<Combatant> conjuredBy(Combatant summoner)
	{
		return order.stream().filter(combatant -> combatant.summonerId().equals(OptionalInt.of(summoner.id())))
			.toList();
	}

	private static boolean conjuredByTroop(List<Combatant> troop, Combatant combatant)
	{
		return troop.stream().anyMatch(member -> combatant.summonerId().equals(OptionalInt.of(member.id())));
	}

	/**
	 * @param d20 rolls a twenty-sided die.
	 * @return this fight with an initiative rolled for each group of monsters still
	 *         without one: one roll per group, plus its modifier.
	 */
	public Combat rollingMissing(IntSupplier d20)
	{
		Combat rolled = this;
		for (Combatant combatant : order)
		{
			Combatant now = rolled.require(combatant.id());
			if (now.kind().isGameMasters() && now.initiative().score().isEmpty())
			{
				rolled = rolled.withInitiative(now.id(), d20.getAsInt() + now.initiative().modifier());
			}
		}
		return rolled;
	}

	/**
	 * @return the turns of a round in order: each combatant not delaying once, and a dual
	 *         initiative a second time twenty lower.
	 */
	public List<Turn> turns()
	{
		List<Turn> turns = new ArrayList<>();
		List<Integer> scores = new ArrayList<>();
		for (Combatant combatant : order)
		{
			if (!combatant.delaying())
			{
				turns.add(new Turn(combatant.id(), false));
				scores.add(combatant.initiative().score().orElse(Integer.MIN_VALUE));
			}
		}
		for (Combatant combatant : order)
		{
			OptionalInt score = combatant.initiative().score();
			if (!combatant.delaying() && combatant.initiative().dual() && score.isPresent())
			{
				int second = score.getAsInt() - Initiative.SECOND_TURN_DROP;
				int at = 0;
				while (at < scores.size() && scores.get(at) >= second)
				{
					at++;
				}
				turns.add(at, new Turn(combatant.id(), true));
				scores.add(at, second);
			}
		}
		return turns;
	}

	/**
	 * @return this fight at the first turn of round one, the spells of the first to act worn down.
	 */
	public Combat started()
	{
		// The first to act begins its turn too: a spell cast before the fight counts that turn
		return new Combat(1, order, turns().stream().findFirst(), experience).turnBegun(List.of()).combat();
	}

	/**
	 * @return the fight with the turn passed to the next combatant, whose spells wear down;
	 *         past the last, a new round begins and wears conditions and conjured creatures down.
	 */
	public CombatAdvance next()
	{
		if (!isStarted())
		{
			return new CombatAdvance(started(), List.of());
		}
		List<Turn> turns = turns();
		int index = current.map(turns::indexOf).orElse(-1);
		if (index >= 0 && index + 1 < turns.size())
		{
			return new Combat(round, order, Optional.of(turns.get(index + 1)), experience).turnBegun(List.of());
		}
		return newRound();
	}

	/** This fight once the turn passed on: a combatant beginning its first turn of the round wears its spells down. */
	private CombatAdvance turnBegun(List<Expiry> expiredBefore)
	{
		Optional<Turn> begun = current.filter(turn -> !turn.second());
		if (begun.isEmpty())
		{
			return new CombatAdvance(this, expiredBefore);
		}
		Combatant caster = require(begun.get().combatantId());
		List<Expiry> expired = new ArrayList<>(expiredBefore);
		List<CastSpell> lasting = new ArrayList<>();
		List<CastSpell> ended = new ArrayList<>();
		for (CastSpell spell : caster.spells())
		{
			spell.ticked().ifPresentOrElse(lasting::add, () -> ended.add(spell));
		}
		Combat worn = replaced(caster.withSpells(lasting));
		for (CastSpell spell : ended)
		{
			expired.add(new Expiry.SpellEnded(caster.name(), spell.name()));
			worn = worn.lifting(spell);
		}
		return new CombatAdvance(worn, expired);
	}

	private CombatAdvance newRound()
	{
		List<Expiry> expired = new ArrayList<>();
		List<Combatant> remaining = new ArrayList<>();
		for (Combatant combatant : order)
		{
			Combatant aged = combatant;
			if (combatant.roundsLeft().isPresent())
			{
				int left = combatant.roundsLeft().getAsInt() - 1;
				if (left < 1)
				{
					expired.add(new Expiry.CreatureLeft(combatant.name()));
					continue;
				}
				aged = aged.withRoundsLeft(OptionalInt.of(left));
			}
			List<Condition> lasting = new ArrayList<>();
			for (Condition condition : combatant.conditions())
			{
				Optional<Condition> ticked = condition.ticked();
				ticked.ifPresentOrElse(lasting::add,
					() -> expired.add(new Expiry.ConditionEnded(combatant.name(), condition.name())));
			}
			remaining.add(aged.withConditions(lasting));
		}
		Combat nextRound = new Combat(round + 1, remaining, Optional.empty(), experience);
		return nextRound.withCurrent(nextRound.turns().stream().findFirst()).turnBegun(expired);
	}

	public Combat damaged(int id, int amount)
	{
		return update(id, combatant -> combatant.health().map(health -> combatant.withHealth(health.damaged(amount)))
			.orElse(combatant));
	}

	public Combat healed(int id, int amount)
	{
		return update(id, combatant -> combatant.health().map(health -> combatant.withHealth(health.healed(amount)))
			.orElse(combatant));
	}

	public Combat withCondition(int id, Condition condition)
	{
		return update(id, combatant -> combatant.withCondition(condition));
	}

	public Combat withoutCondition(int id, String conditionName)
	{
		return update(id, combatant -> combatant.withoutCondition(conditionName));
	}

	/**
	 * @param ids       the combatants the condition goes on at once, such as a band of monsters.
	 * @param condition the condition; on a player's character, whose numbers PCGen does not hold, it is only a label.
	 * @return this fight with the condition laid.
	 */
	public Combat withConditionOn(Collection<Integer> ids, Condition condition)
	{
		Combat laid = this;
		for (int id : ids)
		{
			laid = laid.update(id, combatant -> combatant.withCondition(
				combatant.kind().isGameMasters() ? condition : condition.asLabel()));
		}
		return laid;
	}

	/**
	 * @param ids           combatants of the fight.
	 * @param conditionName the name of a condition to take off.
	 * @return this fight with the condition off each of them.
	 */
	public Combat withoutConditionOn(Collection<Integer> ids, String conditionName)
	{
		Combat lifted = this;
		for (int id : ids)
		{
			lifted = lifted.withoutCondition(id, conditionName);
		}
		return lifted;
	}

	public Combat withSpell(int id, CastSpell spell)
	{
		return update(id, combatant -> combatant.withSpell(spell));
	}

	/**
	 * @param casterId  the caster's id.
	 * @param effect    what the spell does, named after it, its rounds the spell's duration.
	 * @param targetIds the combatants it affects; none when it is only kept track of.
	 * @return this fight with the spell kept by its caster and its effect on its targets until it ends.
	 */
	public Combat cast(int casterId, Condition effect, List<Integer> targetIds)
	{
		CastSpell spell = new CastSpell(effect.name(), effect.roundsLeft(), targetIds);
		Combat recast = withoutSpell(casterId, spell.name());
		return recast.withSpell(casterId, spell).withConditionOn(targetIds, effect.lasting());
	}

	/**
	 * @param casterId  the caster's id.
	 * @param spellName the name of a spell it no longer keeps up.
	 * @return this fight without the spell, its effect lifted from its targets.
	 */
	public Combat withoutSpell(int casterId, String spellName)
	{
		Combatant caster = require(casterId);
		Optional<CastSpell> dropped = caster.spells().stream()
			.filter(spell -> spell.name().equalsIgnoreCase(spellName.strip())).findFirst();
		Combat without = replaced(caster.withoutSpell(spellName));
		return dropped.map(without::lifting).orElse(without);
	}

	/** Whether a spell of that name kept up by a combatant of this fight affects the target. */
	private boolean holdsEffect(String spellName, int targetId)
	{
		return order.stream().flatMap(combatant -> combatant.spells().stream())
			.anyMatch(spell -> spell.name().equalsIgnoreCase(spellName) && spell.targetIds().contains(targetId));
	}

	/** This fight with an ended spell's effect off its targets, unless another spell of that name still holds it there. */
	private Combat lifting(CastSpell ended)
	{
		Combat lifted = this;
		for (int targetId : ended.targetIds())
		{
			if (!holdsEffect(ended.name(), targetId) && combatant(targetId).isPresent())
			{
				lifted = lifted.withoutCondition(targetId, ended.name());
			}
		}
		return lifted;
	}

	/**
	 * @param changed a combatant of the fight, changed.
	 * @return this fight with it in its old place.
	 */
	public Combat replaced(Combatant changed)
	{
		return update(changed.id(), combatant -> changed);
	}

	/**
	 * @param id a combatant's id.
	 * @return this fight with the combatant stepped out of the order; its turn, if it was
	 *         acting, passes on.
	 */
	public Combat delaying(int id)
	{
		Combat stepped = update(id, combatant -> combatant.withDelaying(true));
		return stepped.withCurrent(currentOnceGone(id, stepped));
	}

	/**
	 * @param id a delaying combatant's id.
	 * @return this fight with the combatant acting now, just before the turn it
	 *         interrupts, with the initiative of whoever acted last.
	 */
	public Combat resuming(int id)
	{
		Combatant back = require(id);
		if (!back.delaying())
		{
			return this;
		}
		if (!isStarted())
		{
			return update(id, combatant -> combatant.withDelaying(false));
		}
		List<Turn> turns = turns();
		List<Combatant> combatants = new ArrayList<>(order);
		combatants.remove(back);
		int score;
		int at;
		if (current.isPresent())
		{
			int index = turns.indexOf(current.get());
			score = scoreOf((index > 0) ? turns.get(index - 1) : current.get());
			at = combatants.indexOf(require(current.get().combatantId()));
		}
		else
		{
			score = turns.isEmpty() ? 0 : scoreOf(turns.get(turns.size() - 1));
			at = combatants.size();
		}
		combatants.add(at, back.withDelaying(false).withInitiative(back.initiative().withScore(score)));
		return new Combat(round, combatants, Optional.of(new Turn(id, false)), experience);
	}

	/**
	 * @param id    a combatant's id.
	 * @param index its new place in the order.
	 * @return this fight with the combatant moved, taking the initiative of the one now
	 *         before it (after it, when first) so that later sorting keeps the place.
	 */
	public Combat moved(int id, int index)
	{
		Combatant moving = require(id);
		List<Combatant> combatants = new ArrayList<>(order);
		combatants.remove(moving);
		int at = Math.max(0, Math.min(index, combatants.size()));
		OptionalInt neighbour = OptionalInt.empty();
		if (at > 0)
		{
			neighbour = combatants.get(at - 1).initiative().score();
		}
		else if (!combatants.isEmpty())
		{
			neighbour = combatants.get(0).initiative().score();
		}
		Combatant placed = neighbour.isPresent()
			? moving.withInitiative(moving.initiative().withScore(neighbour.getAsInt())) : moving;
		combatants.add(at, placed);
		return new Combat(round, combatants, current, experience);
	}

	/**
	 * @param id a combatant's id.
	 * @return this fight without the combatant; its turn, if it was acting, passes on. The effects
	 *         of its spells stay on their targets as conditions lasting the rounds the spells had left.
	 */
	public Combat removed(int id)
	{
		Combat without = new Combat(round, order.stream().filter(combatant -> combatant.id() != id).toList(), current,
			experience);
		for (CastSpell spell : require(id).spells())
		{
			without = without.orphaning(spell);
		}
		return without.withCurrent(currentOnceGone(id, without));
	}

	/** This fight with the effect of a spell whose caster left counting down on its own, where no other spell holds it. */
	private Combat orphaning(CastSpell spell)
	{
		Combat orphaned = this;
		for (int targetId : spell.targetIds())
		{
			if (holdsEffect(spell.name(), targetId) || combatant(targetId).isEmpty())
			{
				continue;
			}
			orphaned = orphaned.update(targetId, target -> target.withConditions(target.conditions().stream()
				.map(condition -> condition.name().equalsIgnoreCase(spell.name())
					? new Condition(condition.name(), condition.bonus(), spell.roundsLeft(), condition.chosenValue())
					: condition)
				.toList()));
		}
		return orphaned;
	}

	/**
	 * @param summonerId the conjurer's id.
	 * @param creatures  the creatures conjured together, with ids free in this fight.
	 * @return this fight with the creatures acting right after their conjurer, on its initiative.
	 */
	public Combat summoned(int summonerId, List<Combatant> creatures)
	{
		Combatant summoner = require(summonerId);
		List<Combatant> combatants = new ArrayList<>(order);
		int at = combatants.indexOf(summoner) + 1;
		while (at < combatants.size() && combatants.get(at).summonerId().equals(OptionalInt.of(summonerId)))
		{
			at++;
		}
		String group = creatures.isEmpty() ? "" : "summon:" + creatures.get(0).id();
		for (Combatant creature : creatures)
		{
			combatants.add(at++, creature.withSummoner(summonerId, group, summoner.initiative()));
		}
		return new Combat(round, combatants, current, experience);
	}

	private Combat withCurrent(Optional<Turn> turn)
	{
		return new Combat(round, order, turn, experience);
	}

	private Combat update(int id, UnaryOperator<Combatant> change)
	{
		require(id);
		return new Combat(round, order.stream().map(combatant -> (combatant.id() == id) ? change.apply(combatant)
			: combatant).toList(), current, experience);
	}

	private Combatant require(int id)
	{
		return combatant(id).orElseThrow(() -> new IllegalArgumentException("No combatant " + id + " in the fight"));
	}

	/** The turn that follows the combatant's when it was acting, among the turns left in this round. */
	private Optional<Turn> currentOnceGone(int id, Combat after)
	{
		if (current.isEmpty() || current.get().combatantId() != id)
		{
			return current;
		}
		List<Turn> before = turns();
		List<Turn> left = after.turns();
		for (int index = before.indexOf(current.get()) + 1; index < before.size(); index++)
		{
			Turn candidate = before.get(index);
			if (candidate.combatantId() != id && left.contains(candidate))
			{
				return Optional.of(candidate);
			}
		}
		return Optional.empty();
	}

	private int scoreOf(Turn turn)
	{
		int score = require(turn.combatantId()).initiative().score().orElse(0);
		return turn.second() ? score - Initiative.SECOND_TURN_DROP : score;
	}

	private static boolean sameTroop(Combatant target, Combatant other)
	{
		return other.id() == target.id() || (!target.group().isEmpty() && target.group().equals(other.group()));
	}

	/** Places the combatant before the first one it outranks, after any it ties with. */
	private static void insertByInitiative(List<Combatant> combatants, Combatant combatant)
	{
		OptionalInt score = combatant.initiative().score();
		if (score.isEmpty())
		{
			combatants.add(combatant);
			return;
		}
		int at = 0;
		while (at < combatants.size() && !outranks(combatant, combatants.get(at)))
		{
			at++;
		}
		combatants.add(at, combatant);
	}

	private static boolean outranks(Combatant combatant, Combatant other)
	{
		OptionalInt otherScore = other.initiative().score();
		if (otherScore.isEmpty())
		{
			return true;
		}
		int score = combatant.initiative().score().getAsInt();
		if (score != otherScore.getAsInt())
		{
			return score > otherScore.getAsInt();
		}
		return combatant.initiative().modifier() > other.initiative().modifier();
	}
}
