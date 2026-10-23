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
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import pcgen.core.PlayerCharacter;

/**
 * The loot of a finished fight: for each of the game master's monsters still in it, the
 * conjured ones left out, what it carried, then what is drawn for it. A slain monster's
 * lines start in the loot; the others' are listed, left out.
 */
final class FightLoot
{
	private static final String TITLE_SEPARATOR = " + ";
	private static final String NO_RATING = "-";

	private final CreatureLoot creatureLoot;
	private final TreasureDraw treasureDraw;

	FightLoot(CreatureLoot creatureLoot, TreasureDraw treasureDraw)
	{
		this.creatureLoot = creatureLoot;
		this.treasureDraw = treasureDraw;
	}

	/**
	 * @param id              the number the block takes.
	 * @param combat          the fight ending.
	 * @param built           the monsters PCGen built, by combatant id.
	 * @param challengeRating reads a built monster's challenge rating.
	 * @param defaultTitle    the title of a fight no prepared encounter was launched into.
	 * @return the fight's loot; empty when no monster of the game master's took part.
	 */
	Optional<LootBlock> of(int id, Combat combat, Map<Integer, PlayerCharacter> built,
		Function<PlayerCharacter, String> challengeRating, String defaultTitle)
	{
		List<Combatant> looted = combat.order().stream()
			.filter(combatant -> combatant.kind() == CombatantKind.MONSTER && combatant.summonerId().isEmpty()).toList();
		if (looted.isEmpty())
		{
			return Optional.empty();
		}
		List<LootLine> carried = new ArrayList<>();
		List<LootLine> drawn = new ArrayList<>();
		List<LootDraw> draws = new ArrayList<>();
		for (Combatant monster : looted)
		{
			PlayerCharacter character = built.get(monster.id());
			if (character == null)
			{
				continue;
			}
			boolean slain = monster.health().map(health -> health.damage() >= health.maximum()).orElse(false);
			List<LootLine> its = creatureLoot.carried(character, monster.name(), slain);
			boolean fromCharacterFile = monster.recipe()
				.map(recipe -> recipe.source() instanceof MonsterSource.CharacterFile).orElse(false);
			LootDraw draw = creatureLoot.draw(character, monster.name(), challengeRating.apply(character), fromCharacterFile,
				its.stream().mapToLong(LootLine::value).sum());
			carried.addAll(its);
			draws.add(draw);
			drawn.addAll(treasureDraw.draw(draw, slain));
		}
		carried.addAll(drawn);
		String title = combat.encounters().isEmpty() ? defaultTitle : String.join(TITLE_SEPARATOR, combat.encounters());
		return Optional.of(new LootBlock(id, title, Experience.challengeRatingOf(combat.experience()).orElse(NO_RATING),
			combat.experience(), carried, draws));
	}

	/**
	 * @param block a fight's loot.
	 * @return the same loot with its drawn lines drawn again; each monster's new lines start in
	 *         the loot as its old drawn lines did.
	 */
	LootBlock rerolled(LootBlock block)
	{
		List<LootLine> drawn = new ArrayList<>();
		for (LootDraw draw : block.draws())
		{
			boolean selected = block.lines().stream()
				.filter(line -> line.drawnAs().isPresent() && line.carrier().equals(draw.carrier())).findFirst()
				.map(LootLine::selected).orElse(true);
			drawn.addAll(treasureDraw.draw(draw, selected));
		}
		return block.withDrawnLines(drawn);
	}
}
