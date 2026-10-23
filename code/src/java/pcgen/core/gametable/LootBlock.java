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

/**
 * The loot of one finished fight.
 *
 * @param id              its number in the scenario.
 * @param title           the encounters fought.
 * @param challengeRating the challenge rating of the whole fight.
 * @param experience      what the fight's monsters were worth together.
 * @param lines           what the monsters carried, then what was drawn.
 * @param draws           what was drawn for, to draw it again.
 */
public record LootBlock(int id, String title, String challengeRating, long experience, List<LootLine> lines,
	List<LootDraw> draws)
{
	public LootBlock
	{
		Objects.requireNonNull(title);
		Objects.requireNonNull(challengeRating);
		lines = List.copyOf(lines);
		draws = List.copyOf(draws);
	}

	public long selectedValue()
	{
		return lines.stream().filter(LootLine::selected).mapToLong(LootLine::value).sum();
	}

	public long selectedResale()
	{
		return lines.stream().filter(LootLine::selected).mapToLong(LootLine::resale).sum();
	}

	/**
	 * @return the treasure value of the fight's challenge rating, as a reminder.
	 */
	public long budget()
	{
		return TreasureBudget.forChallengeRating(challengeRating);
	}

	/**
	 * @param players the characters sharing the loot.
	 * @return each one's share of the selected resale; nothing when no one shares it.
	 */
	public long sharePerPlayer(int players)
	{
		return players < 1 ? 0 : selectedResale() / players;
	}

	/**
	 * @param players the characters sharing the experience.
	 * @return each one's share of the fight's experience.
	 */
	public long experiencePerPlayer(int players)
	{
		return Experience.share(experience, players);
	}

	/**
	 * @param line     the line's index.
	 * @param selected whether it is kept.
	 * @return the block with that line kept or left out.
	 */
	public LootBlock selecting(int line, boolean selected)
	{
		if (line < 0 || line >= lines.size())
		{
			throw new IllegalArgumentException("The loot of " + title + " has no line " + line);
		}
		List<LootLine> changed = new ArrayList<>(lines);
		changed.set(line, lines.get(line).selecting(selected));
		return new LootBlock(id, title, challengeRating, experience, changed, draws);
	}

	/**
	 * @param drawn lines drawn again.
	 * @return the block keeping the carried lines as they are, the new drawn lines in place of the old.
	 */
	public LootBlock withDrawnLines(List<LootLine> drawn)
	{
		List<LootLine> changed = new ArrayList<>(lines.stream().filter(found -> found.drawnAs().isEmpty()).toList());
		changed.addAll(drawn);
		return new LootBlock(id, title, challengeRating, experience, changed, draws);
	}
}
