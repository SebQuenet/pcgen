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
package pcgen.cdom.facet;

import pcgen.cdom.enumeration.CharID;
import pcgen.cdom.facet.base.AbstractItemFacet;

/**
 * TacticalPlanFacet tracks the source text of a Player Character's tactical
 * plan. A character either holds one plan or none at all.
 *
 * <p>
 * The text is what the character stores: the object model is read back out of
 * it wherever it is needed, so there is one representation rather than two to
 * keep in agreement.
 */
public class TacticalPlanFacet extends AbstractItemFacet<CharID, String>
{

}
