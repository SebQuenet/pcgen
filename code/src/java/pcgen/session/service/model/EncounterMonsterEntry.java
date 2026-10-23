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
package pcgen.session.service.model;

/**
 * Monsters of a prepared encounter.
 *
 * @param name            the name of one
 * @param count           how many enter
 * @param recipe          how each is built
 * @param experience      what they are worth together, 0 when they cannot be built
 * @param challengeRating the challenge rating of one, null when it cannot be built
 */
public record EncounterMonsterEntry(String name, int count, RecipeEntry recipe, long experience, String challengeRating)
{
}
