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
package pcgen.io.tactics;

/**
 * What the character's own data says about one of its weapons.
 *
 * @param name     the weapon as the character names it.
 * @param toHit    its attack bonus, as the export layer computes it.
 * @param damage   its damage.
 * @param critical its critical range and multiplier.
 */
public record ResolvedWeapon(String name, String toHit, String damage, String critical)
{
}
