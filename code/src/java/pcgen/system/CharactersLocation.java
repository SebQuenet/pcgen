package pcgen.system;

import java.nio.file.Path;

import javax.swing.filechooser.FileSystemView;

import org.apache.commons.lang3.SystemUtils;

/**
 * Where characters are saved. An installed program must not keep them in its own
 * directory: the Windows installer deletes that directory when it upgrades or removes
 * PCGen, and on Linux the user cannot write there.
 */
public final class CharactersLocation
{
	/** Set by the native installers' launchers. */
	static final String IN_DOCUMENTS_PROPERTY = "pcgen.charactersInDocuments";

	private static final String CHARACTERS_DIRECTORY_NAME = "characters";
	private static final String APPLICATION_DIRECTORY_NAME = "PCGen";

	private final boolean isInstalled;
	private final Path programDirectory;
	private final Path documentsDirectory;

	CharactersLocation(boolean isInstalled, Path programDirectory, Path documentsDirectory)
	{
		this.isInstalled = isInstalled;
		this.programDirectory = programDirectory;
		this.documentsDirectory = documentsDirectory;
	}

	static CharactersLocation forCurrentProgram()
	{
		boolean isInstalled = Boolean.getBoolean(IN_DOCUMENTS_PROPERTY);
		// The shell's documents folder, which follows a redirection to OneDrive on Windows.
		Path documentsDirectory = isInstalled
			? FileSystemView.getFileSystemView().getDefaultDirectory().toPath()
			: Path.of(SystemUtils.USER_HOME);
		return new CharactersLocation(isInstalled, Path.of(ConfigurationSettings.getUserDir()), documentsDirectory);
	}

	String defaultDirectory()
	{
		if (isInstalled)
		{
			return documentsDirectory.resolve(APPLICATION_DIRECTORY_NAME).resolve(CHARACTERS_DIRECTORY_NAME).toString();
		}
		return programDirectory.resolve(CHARACTERS_DIRECTORY_NAME).toString();
	}

	/**
	 * @return the saved path, unless it is the installation's own characters directory
	 */
	String relocated(String savedPath)
	{
		if (isInstalled && programDirectory.resolve(CHARACTERS_DIRECTORY_NAME).toString().equals(savedPath))
		{
			return defaultDirectory();
		}
		return savedPath;
	}
}
