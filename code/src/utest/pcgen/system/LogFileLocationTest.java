package pcgen.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class LogFileLocationTest
{
	@Test
	void writesTheLogInsideTheSettingsDirectory(@TempDir Path settingsDirectory)
	{
		String pattern = LogFileLocation.patternIn(settingsDirectory);

		assertEquals(settingsDirectory.resolve("pcgen.log").toString(), pattern);
	}

	@Test
	void createsASettingsDirectoryThatDoesNotExistYet(@TempDir Path parent)
	{
		Path settingsDirectory = parent.resolve("pcgen");

		LogFileLocation.patternIn(settingsDirectory);

		assertTrue(Files.isDirectory(settingsDirectory));
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void fallsBackToTheTemporaryDirectoryWhenTheSettingsDirectoryCannotBeCreated(@TempDir Path parent)
		throws IOException
	{
		Files.setPosixFilePermissions(parent, PosixFilePermissions.fromString("r-xr-xr-x"));

		String pattern = LogFileLocation.patternIn(parent.resolve("pcgen"));

		assertEquals("%t/pcgen.log", pattern);
	}
}
