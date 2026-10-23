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

import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * An image kept in the scenario file: an illustration, a map, a battlemap.
 *
 * @param id       its number, unique in the scenario.
 * @param name     what the game master calls it.
 * @param category what it shows.
 * @param format   its format.
 * @param content  its bytes, in base 64, so that the image keeps the value semantics of a record.
 */
public record ScenarioImage(int id, String name, ImageCategory category, ImageFormat format, String content)
{
	/** The largest image kept: 50 MB. */
	public static final int MAXIMUM_BYTES = 50 * 1024 * 1024;

	public ScenarioImage
	{
		if (Objects.requireNonNull(name).isBlank())
		{
			throw new IllegalArgumentException("An image has a name");
		}
		name = name.strip();
		Objects.requireNonNull(category);
		Objects.requireNonNull(format);
		Objects.requireNonNull(content);
	}

	/**
	 * @param bytes the file's content.
	 * @return the image; IllegalArgumentException when the bytes are no image the game table
	 *         keeps, or too many.
	 */
	public static ScenarioImage of(int id, String name, ImageCategory category, byte[] bytes)
	{
		if (bytes.length > MAXIMUM_BYTES)
		{
			throw new IllegalArgumentException(name + " is larger than " + MAXIMUM_BYTES + " bytes");
		}
		ImageFormat format = ImageFormat.detect(bytes)
			.orElseThrow(() -> new IllegalArgumentException(name + " is not a PNG, JPEG, GIF or BMP image"));
		return new ScenarioImage(id, name, category, format, Base64.getEncoder().encodeToString(bytes));
	}

	/**
	 * @return the image's size, read from its base 64 length and padding.
	 */
	public int sizeInBytes()
	{
		int padding = content.endsWith("==") ? 2 : (content.endsWith("=") ? 1 : 0);
		return content.length() / 4 * 3 - padding;
	}

	/**
	 * @return the image's name, safe as a file name, with its format's extension.
	 */
	public String fileName()
	{
		String safe = name.replaceAll("[\"\\\\/:*?<>|\\r\\n]", "_");
		String extension = "." + format.extension();
		return safe.toLowerCase(Locale.ROOT).endsWith(extension) ? safe : safe + extension;
	}

	public byte[] bytes()
	{
		return Base64.getDecoder().decode(content);
	}

	/**
	 * @param shelf  images in the order they are shown.
	 * @param added  an image joining them.
	 * @param isScenarioWide whether the shelf is the scenario's rather than an encounter's.
	 * @return the images with the new one after those of its category.
	 */
	static List<ScenarioImage> shelved(List<ScenarioImage> shelf, ScenarioImage added, boolean isScenarioWide)
	{
		if (added.category().isScenarioWide() != isScenarioWide)
		{
			throw new IllegalArgumentException(added.category() + " images do not belong "
				+ (isScenarioWide ? "to the scenario" : "to an encounter"));
		}
		List<ScenarioImage> images = new ArrayList<>(shelf);
		images.add(added);
		images.sort(Comparator.comparing(ScenarioImage::category));
		return List.copyOf(images);
	}
}
