package pcgen.mcp.config;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Where this MCP server can be started from, as it must be written into a client's configuration.
 */
public final class ServerExecutable
{
	/** Set by the native launcher jpackage builds, to the absolute path of that launcher. */
	private static final String LAUNCHER_PATH_PROPERTY = "jpackage.app-path";

	private ServerExecutable()
	{
	}

	public static Optional<Path> forCurrentProcess()
	{
		return location(Optional.ofNullable(System.getProperty(LAUNCHER_PATH_PROPERTY)),
			ProcessHandle.current().info().command());
	}

	public static Optional<Path> location(Optional<String> launcherPath, Optional<String> processCommand)
	{
		return launcherPath.or(() -> processCommand).map(Path::of);
	}
}
