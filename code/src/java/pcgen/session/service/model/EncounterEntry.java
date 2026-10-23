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

import java.util.List;

/**
 * A prepared encounter.
 *
 * @param name            its name, unique within the scenario
 * @param notes           the game master's notes
 * @param monsters        its monsters
 * @param experience      what its monsters are worth together
 * @param challengeRating the challenge rating that total amounts to, null for none
 * @param images          its location illustrations, battlemaps and other images, without their content
 */
public record EncounterEntry(String name, String notes, List<EncounterMonsterEntry> monsters, long experience,
	String challengeRating, List<ImageEntry> images)
{
}
