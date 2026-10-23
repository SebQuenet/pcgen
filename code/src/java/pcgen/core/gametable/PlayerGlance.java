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

/**
 * The few numbers of a player's character the game master reads during a fight. Its
 * armor class is left out: it changes too often in a fight to be of use.
 *
 * @param perception  its Perception bonus, signed
 * @param senseMotive its Sense Motive bonus, signed
 * @param fortitude   its Fortitude save
 * @param reflex      its Reflex save
 * @param will        its Will save
 */
public record PlayerGlance(String perception, String senseMotive, String fortitude, String reflex, String will)
{
}
