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
 * The Tabletop Simulator object an image becomes once placed on the table.
 */
public enum TabletopPiece
{
	/** A flat tile, for maps and illustrations. */
	TILE("Custom_Tile"),
	/** A standing cut-out, for the pawn of a creature. */
	FIGURINE("Figurine_Custom");

	private final String objectName;

	TabletopPiece(String objectName)
	{
		this.objectName = objectName;
	}

	/**
	 * @return the object's name in Tabletop Simulator, such as Custom_Tile.
	 */
	public String objectName()
	{
		return objectName;
	}
}
