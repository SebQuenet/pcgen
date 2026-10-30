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
 * How a monster of the game table is built.
 *
 * @param race             the key of its race in the loaded sources, null when built otherwise
 * @param characterFile    the character file it is read from, null when built otherwise
 * @param kit              the key of the NPC kit it is built from, null when built otherwise
 * @param addedTemplates   the keys of the templates laid on it
 * @param removedTemplates the keys of the templates taken off it
 * @param addedHitDice     the hit dice added to its race's
 */
public record RecipeEntry(String race, String characterFile, String kit, List<String> addedTemplates, List<String> removedTemplates,
	int addedHitDice)
{
}
