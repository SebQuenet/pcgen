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
package pcgen.core.tactics;

/**
 * One element of a tactical section.
 *
 * <p>
 * A section holds a heterogeneous list of these: a step of the rotation, an
 * attack with its variants, a tickable resource, a summoned creature's stat
 * block, a tagged spell repertoire, or a prose note. The interface is sealed so
 * that a renderer must handle every kind, and so that no block can exist in a
 * half filled state.
 */
public sealed interface TacticalBlock
		permits TacticalStep, TacticalAttack, TacticalResource, TacticalCreature, TacticalSpellList, TacticalNote
{
}
