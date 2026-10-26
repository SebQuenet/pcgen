package pcgen.system;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class CharactersLocationTest
{
	private static final Path PROGRAM_DIRECTORY = Path.of("/opt/pcgen/lib/app");
	private static final Path DOCUMENTS_DIRECTORY = Path.of("/home/player/Documents");

	@Test
	void keepsTheCharactersNextToTheProgramWhenNotInstalled()
	{
		CharactersLocation location = new CharactersLocation(false, PROGRAM_DIRECTORY, DOCUMENTS_DIRECTORY);

		assertEquals(PROGRAM_DIRECTORY.resolve("characters").toString(), location.defaultDirectory());
	}

	@Test
	void putsTheCharactersOfAnInstalledProgramInTheDocuments()
	{
		CharactersLocation location = new CharactersLocation(true, PROGRAM_DIRECTORY, DOCUMENTS_DIRECTORY);

		assertEquals(DOCUMENTS_DIRECTORY.resolve("PCGen").resolve("characters").toString(),
			location.defaultDirectory());
	}

	@Test
	void movesASavedPathInsideTheInstallationToTheDocuments()
	{
		CharactersLocation location = new CharactersLocation(true, PROGRAM_DIRECTORY, DOCUMENTS_DIRECTORY);

		String savedPath = location.relocated(PROGRAM_DIRECTORY.resolve("characters").toString());

		assertEquals(location.defaultDirectory(), savedPath);
	}

	@Test
	void keepsAPathTheUserChose()
	{
		CharactersLocation location = new CharactersLocation(true, PROGRAM_DIRECTORY, DOCUMENTS_DIRECTORY);

		String savedPath = location.relocated("/home/player/campaigns");

		assertEquals("/home/player/campaigns", savedPath);
	}

	@Test
	void keepsTheSavedPathWhenNotInstalled()
	{
		CharactersLocation location = new CharactersLocation(false, PROGRAM_DIRECTORY, DOCUMENTS_DIRECTORY);
		String programCharacters = PROGRAM_DIRECTORY.resolve("characters").toString();

		String savedPath = location.relocated(programCharacters);

		assertEquals(programCharacters, savedPath);
	}
}
