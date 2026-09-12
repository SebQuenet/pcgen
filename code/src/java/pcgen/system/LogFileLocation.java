package pcgen.system;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Where the log file goes. A native launcher can be started from a directory the user
 * cannot write to, so the log belongs with the user's settings rather than next to the
 * running program.
 */
public final class LogFileLocation
{
	private static final String LOG_FILE_NAME = "pcgen.log";
	/** java.util.logging expands %t to the system temporary directory. */
	private static final String TEMPORARY_DIRECTORY_PATTERN = "%t/" + LOG_FILE_NAME;

	private LogFileLocation()
	{
	}

	public static String forCurrentUser()
	{
		return patternIn(Path.of(ConfigurationSettings.getUserSettingsDirFromFilePath()));
	}

	public static String patternIn(Path settingsDirectory)
	{
		try
		{
			Files.createDirectories(settingsDirectory);
			return settingsDirectory.resolve(LOG_FILE_NAME).toString();
		}
		catch (IOException e)
		{
			return TEMPORARY_DIRECTORY_PATTERN;
		}
	}
}
