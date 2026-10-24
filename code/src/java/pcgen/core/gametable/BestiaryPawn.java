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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import pcgen.io.conjurations.PathfinderSummonPortraits;

/**
 * The pawn of a bestiary creature: a 400x600 JPEG on a white background, drawn by
 * {@code scripts/generate_bestiary_pawns.py} and named after the race key or, for a monster
 * built from a saved character, after the character's file.
 *
 * @param fileName the pawn's file name, such as giant_stone.jpg.
 * @param location where the pawn is read from; a page shows it from there.
 */
public record BestiaryPawn(String fileName, URL location)
{
	private static final String DIRECTORY = "/pcgen/bestiary/pawns/";
	private static final String CHARACTER_EXTENSION = ".pcg";

	public BestiaryPawn
	{
		Objects.requireNonNull(fileName);
		Objects.requireNonNull(location);
	}

	/**
	 * @param source what the monster is built from.
	 * @return its pawn; empty when none was drawn for it.
	 */
	public static Optional<BestiaryPawn> of(MonsterSource source)
	{
		String name = switch (source)
		{
			case MonsterSource.BestiaryRace race -> race.raceKey();
			case MonsterSource.CharacterFile file -> characterName(file.path());
		};
		String fileName = PathfinderSummonPortraits.fileNameOf(name);
		return Optional.ofNullable(BestiaryPawn.class.getResource(DIRECTORY + fileName))
			.map(location -> new BestiaryPawn(fileName, location));
	}

	private static String characterName(String path)
	{
		String fileName = Path.of(path).getFileName().toString();
		return fileName.endsWith(CHARACTER_EXTENSION)
			? fileName.substring(0, fileName.length() - CHARACTER_EXTENSION.length()) : fileName;
	}

	public byte[] bytes()
	{
		try (InputStream pawn = location.openStream())
		{
			return pawn.readAllBytes();
		}
		catch (IOException error)
		{
			throw new UncheckedIOException("Could not read the pawn " + fileName, error);
		}
	}

	/**
	 * @return the pawn, to stand on Tabletop Simulator's table as a figurine.
	 */
	public TabletopUpload forTabletop()
	{
		return new TabletopUpload(fileName, ImageFormat.JPEG, bytes(), TabletopPiece.FIGURINE);
	}
}
