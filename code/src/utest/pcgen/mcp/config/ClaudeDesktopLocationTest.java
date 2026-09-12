package pcgen.mcp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

class ClaudeDesktopLocationTest
{
	private static final Path USER_HOME = Path.of("/home/joe");
	private static final UnaryOperator<String> NO_VARIABLE_SET = name -> null;

	@Test
	void looksUnderTheRoamingProfileOnWindows()
	{
		UnaryOperator<String> environment = variablesOf(Map.of("APPDATA", "/users/joe/AppData/Roaming"));

		Optional<Path> configFile = ClaudeDesktopLocation.configFile(true, environment, USER_HOME);

		assertEquals(Optional.of(Path.of("/users/joe/AppData/Roaming/Claude/claude_desktop_config.json")), configFile);
	}

	@Test
	void findsNoLocationOnWindowsWithoutARoamingProfile()
	{
		Optional<Path> configFile = ClaudeDesktopLocation.configFile(true, NO_VARIABLE_SET, USER_HOME);

		assertEquals(Optional.empty(), configFile);
	}

	@Test
	void honoursTheFreedesktopConfigurationHomeElsewhere()
	{
		UnaryOperator<String> environment = variablesOf(Map.of("XDG_CONFIG_HOME", "/home/joe/elsewhere"));

		Optional<Path> configFile = ClaudeDesktopLocation.configFile(false, environment, USER_HOME);

		assertEquals(Optional.of(Path.of("/home/joe/elsewhere/Claude/claude_desktop_config.json")), configFile);
	}

	@Test
	void fallsBackToTheDefaultConfigurationHomeElsewhere()
	{
		Optional<Path> configFile = ClaudeDesktopLocation.configFile(false, NO_VARIABLE_SET, USER_HOME);

		assertEquals(Optional.of(Path.of("/home/joe/.config/Claude/claude_desktop_config.json")), configFile);
	}

	private static UnaryOperator<String> variablesOf(Map<String, String> variables)
	{
		return variables::get;
	}
}
