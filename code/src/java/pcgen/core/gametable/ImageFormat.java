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

import java.util.Arrays;
import java.util.Optional;

/**
 * The image formats the game table keeps: those Java can show. A file's format is read in
 * its first bytes, never in its name.
 */
public enum ImageFormat
{
	PNG("image/png", "png", new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}),
	JPEG("image/jpeg", "jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
	GIF("image/gif", "gif", new byte[]{'G', 'I', 'F', '8'}),
	BMP("image/bmp", "bmp", new byte[]{'B', 'M'});

	private final String mediaType;
	private final String extension;
	private final byte[] signature;

	ImageFormat(String mediaType, String extension, byte[] signature)
	{
		this.mediaType = mediaType;
		this.extension = extension;
		this.signature = signature;
	}

	/**
	 * @param content a file's bytes.
	 * @return its format; empty when it is not an image the game table keeps.
	 */
	public static Optional<ImageFormat> detect(byte[] content)
	{
		return Arrays.stream(values()).filter(format -> content.length >= format.signature.length
			&& Arrays.equals(Arrays.copyOf(content, format.signature.length), format.signature)).findFirst();
	}

	public String mediaType()
	{
		return mediaType;
	}

	public String extension()
	{
		return extension;
	}
}
