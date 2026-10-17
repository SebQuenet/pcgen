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
package pcgen.persistence.translation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import pcgen.cdom.base.CDOMObject;
import pcgen.core.Campaign;
import pcgen.core.Globals;
import pcgen.facade.core.SourceSelectionFacade;
import pcgen.session.HeadlessUIDelegate;
import pcgen.session.PcgenSession;
import pcgen.system.CharacterManager;
import pcgen.system.FacadeFactory;
import pcgen.util.TestHelper;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A translated name is display only. Every loaded object is given a made up name, and a real
 * character saved afterwards must hold the same lines as before, in whatever order: a
 * difference is a place where a shown name is used as a key.
 */
class PseudoTranslationTest
{
	private static final String TEST_CONFIG_FILE = "config.ini.junit-translation";
	private static final String CHARACTERS_FOLDER = "code/testsuite/PCGfiles";
	private static final String SAVE_DATE_LINE = "# Character Save Date";
	private static final Pattern OUTPUT_ORDER = Pattern.compile("\\|OUTPUTORDER:-?\\d+");

	@TempDir
	static Path workFolder;

	@BeforeAll
	static void loadGameModes()
	{
		TestHelper.loadGameModes(TEST_CONFIG_FILE);
		FacadeFactory.initialize();
	}

	@ParameterizedTest
	@ValueSource(strings = {"pf_Rogue", "pf_Cleric", "pf_Paladin", "pf_goldielocks"})
	void aTranslatedNameDoesNotChangeASavedCharacter(String character) throws IOException
	{
		Path original = Path.of(CHARACTERS_FOLDER, character + ".pcg");
		PcgenSession session = new PcgenSession();
		SourceSelectionFacade sources =
				CharacterManager.getRequiredSourcesForCharacter(original.toFile(), new HeadlessUIDelegate());
		session.loadSources(sources.getGameMode().get().getName(),
			StreamSupport.stream(sources.getCampaigns().spliterator(), false).map(Campaign::getKeyName).toList());
		String english = savedAgain(session, original, character + "-en.pcg");

		pseudoTranslateEveryObject();
		String pseudoTranslated = savedAgain(session, original, character + "-xx.pcg");

		assertEquals(english, pseudoTranslated);
	}

	private static void pseudoTranslateEveryObject()
	{
		for (Object loaded : Globals.getContext().getReferenceContext().getAllConstructedObjects())
		{
			if (loaded instanceof CDOMObject object)
			{
				object.setLocalizedName("⟦" + object.getDisplayName() + "⟧");
			}
		}
	}

	private static String savedAgain(PcgenSession session, Path original, String copyName) throws IOException
	{
		Path copy = workFolder.resolve(copyName);
		Files.copy(original, copy, StandardCopyOption.REPLACE_EXISTING);
		String characterId = session.openCharacter(new File(copy.toString())).getCharacterId();
		session.saveCharacter(characterId);
		session.closeCharacter(characterId);
		// Skills are listed in the order of their shown names, which a translation changes
		return Files.readAllLines(copy, StandardCharsets.UTF_8).stream()
			.filter(line -> !line.startsWith(SAVE_DATE_LINE))
			.map(line -> OUTPUT_ORDER.matcher(line).replaceAll(""))
			.sorted()
			.collect(Collectors.joining("\n"));
	}
}
