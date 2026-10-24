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

import java.util.Objects;

/**
 * An image on its way to Tabletop Simulator.
 *
 * @param fileName the name it is filed under in the cloud, with its extension.
 * @param format   its format.
 * @param bytes    its content.
 * @param piece    what it becomes when placed on the table.
 */
public record TabletopUpload(String fileName, ImageFormat format, byte[] bytes, TabletopPiece piece)
{
	public TabletopUpload
	{
		Objects.requireNonNull(fileName);
		Objects.requireNonNull(format);
		Objects.requireNonNull(bytes);
		Objects.requireNonNull(piece);
	}
}
