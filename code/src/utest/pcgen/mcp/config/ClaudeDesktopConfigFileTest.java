package pcgen.mcp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class ClaudeDesktopConfigFileTest
{
	private static final Path EXECUTABLE = Path.of("/opt/pcgen/bin/pcgen-mcp");

	@Test
	void writesServerEntryWhenConfigFileDoesNotExistYet(@TempDir Path clientDirectory) throws IOException
	{
		Path configFile = clientDirectory.resolve("claude_desktop_config.json");

		new ClaudeDesktopConfigFile(configFile).install(EXECUTABLE);

		assertEquals(EXECUTABLE.toString(), readCommandOf(configFile, "pcgen"));
	}

	@Test
	void keepsServersAlreadyConfiguredByTheUser(@TempDir Path clientDirectory) throws IOException
	{
		Path configFile = clientDirectory.resolve("claude_desktop_config.json");
		Files.writeString(configFile, """
			{"mcpServers": {"filesystem": {"command": "/usr/bin/mcp-filesystem"}}}""");

		new ClaudeDesktopConfigFile(configFile).install(EXECUTABLE);

		assertEquals("/usr/bin/mcp-filesystem", readCommandOf(configFile, "filesystem"));
	}

	@Test
	void replacesAStaleExecutablePathOfAPreviousInstallation(@TempDir Path clientDirectory) throws IOException
	{
		Path configFile = clientDirectory.resolve("claude_desktop_config.json");
		Files.writeString(configFile, """
			{"mcpServers": {"pcgen": {"command": "/old/location/pcgen-mcp"}}}""");

		new ClaudeDesktopConfigFile(configFile).install(EXECUTABLE);

		assertEquals(EXECUTABLE.toString(), readCommandOf(configFile, "pcgen"));
	}

	@Test
	void keepsACopyOfTheConfigFileItModifies(@TempDir Path clientDirectory) throws IOException
	{
		Path configFile = clientDirectory.resolve("claude_desktop_config.json");
		String contentWrittenByTheUser = """
			{"mcpServers": {"filesystem": {"command": "/usr/bin/mcp-filesystem"}}}""";
		Files.writeString(configFile, contentWrittenByTheUser);

		new ClaudeDesktopConfigFile(configFile).install(EXECUTABLE);

		assertEquals(contentWrittenByTheUser,
			Files.readString(clientDirectory.resolve("claude_desktop_config.json.bak")));
	}

	@Test
	void keepsNoCopyWhenThereWasNoConfigFileToPreserve(@TempDir Path clientDirectory) throws IOException
	{
		Path configFile = clientDirectory.resolve("claude_desktop_config.json");

		new ClaudeDesktopConfigFile(configFile).install(EXECUTABLE);

		assertFalse(Files.exists(clientDirectory.resolve("claude_desktop_config.json.bak")));
	}

	@Test
	void reportsTheClientAsMissingWhenItsDirectoryDoesNotExist(@TempDir Path configHome)
	{
		Path configFile = configHome.resolve("Claude").resolve("claude_desktop_config.json");

		ConfigInstallOutcome outcome = new ClaudeDesktopConfigFile(configFile).install(EXECUTABLE);

		assertEquals(new ConfigInstallOutcome.ClientNotFound(), outcome);
	}

	@Test
	void createsNoDirectoryForAClientThatIsNotInstalled(@TempDir Path configHome)
	{
		Path clientDirectory = configHome.resolve("Claude");

		new ClaudeDesktopConfigFile(clientDirectory.resolve("claude_desktop_config.json")).install(EXECUTABLE);

		assertFalse(Files.exists(clientDirectory));
	}

	@Test
	void leavesAnUnreadableConfigFileUntouched(@TempDir Path clientDirectory) throws IOException
	{
		Path configFile = clientDirectory.resolve("claude_desktop_config.json");
		Files.writeString(configFile, "{ this is not JSON");

		new ClaudeDesktopConfigFile(configFile).install(EXECUTABLE);

		assertEquals("{ this is not JSON", Files.readString(configFile));
	}

	@Test
	void reportsAnUnreadableConfigFileRatherThanOverwritingIt(@TempDir Path clientDirectory) throws IOException
	{
		Path configFile = clientDirectory.resolve("claude_desktop_config.json");
		Files.writeString(configFile, "{ this is not JSON");

		ConfigInstallOutcome outcome = new ClaudeDesktopConfigFile(configFile).install(EXECUTABLE);

		assertInstanceOf(ConfigInstallOutcome.ConfigUnreadable.class, outcome);
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void reportsAConfigFileItCannotWriteTo(@TempDir Path clientDirectory) throws IOException
	{
		Path configFile = clientDirectory.resolve("claude_desktop_config.json");
		Files.writeString(configFile, "{}");
		Files.setPosixFilePermissions(configFile, PosixFilePermissions.fromString("r--r--r--"));

		ConfigInstallOutcome outcome = new ClaudeDesktopConfigFile(configFile).install(EXECUTABLE);

		assertInstanceOf(ConfigInstallOutcome.InstallFailed.class, outcome);
	}

	private static String readCommandOf(Path configFile, String serverName) throws IOException
	{
		JsonNode root = new ObjectMapper().readTree(Files.readString(configFile));
		return root.path("mcpServers").path(serverName).path("command").asText();
	}
}
