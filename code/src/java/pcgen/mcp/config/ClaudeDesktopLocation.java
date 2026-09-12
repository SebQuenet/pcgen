package pcgen.mcp.config;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.UnaryOperator;

import org.apache.commons.lang3.SystemUtils;

/**
 * Where Claude Desktop keeps the configuration file listing its MCP servers.
 */
public final class ClaudeDesktopLocation
{
	private static final String CONFIG_FILE_NAME = "claude_desktop_config.json";
	private static final String APPLICATION_DIRECTORY = "Claude";
	private static final String ROAMING_PROFILE_VARIABLE = "APPDATA";
	private static final String CONFIG_HOME_VARIABLE = "XDG_CONFIG_HOME";
	private static final String DEFAULT_CONFIG_HOME = ".config";

	private ClaudeDesktopLocation()
	{
	}

	public static Optional<Path> forCurrentPlatform()
	{
		return configFile(SystemUtils.IS_OS_WINDOWS, System::getenv, Path.of(SystemUtils.USER_HOME));
	}

	public static Optional<Path> configFile(boolean onWindows, UnaryOperator<String> environment, Path userHome)
	{
		return applicationDataDirectory(onWindows, environment, userHome)
			.map(directory -> directory.resolve(APPLICATION_DIRECTORY).resolve(CONFIG_FILE_NAME));
	}

	private static Optional<Path> applicationDataDirectory(boolean onWindows, UnaryOperator<String> environment,
		Path userHome)
	{
		if (onWindows)
		{
			return Optional.ofNullable(environment.apply(ROAMING_PROFILE_VARIABLE)).map(Path::of);
		}
		return Optional.ofNullable(environment.apply(CONFIG_HOME_VARIABLE))
			.map(Path::of)
			.or(() -> Optional.of(userHome.resolve(DEFAULT_CONFIG_HOME)));
	}
}
