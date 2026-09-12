package pcgen.mcp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class McpConfigInstallerTest
{
	@Test
	void offersTheServerToBothClientsWhenClaudeDesktopHasAKnownLocation()
	{
		Path configFile = Path.of("/home/joe/.config/Claude/claude_desktop_config.json");

		List<McpClientConfigurator> clients = McpConfigInstaller.clientsFor(Optional.of(configFile));

		assertEquals(
			List.of("Claude Desktop (" + configFile + ")", "Claude Code CLI"),
			clients.stream().map(McpClientConfigurator::description).toList());
	}

	@Test
	void fallsBackToClaudeCodeAloneWhereClaudeDesktopHasNoKnownLocation()
	{
		List<McpClientConfigurator> clients = McpConfigInstaller.clientsFor(Optional.empty());

		assertEquals(List.of("Claude Code CLI"),
			clients.stream().map(McpClientConfigurator::description).toList());
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void reportsOneLinePerClientItOffersTheServerTo(@TempDir Path clientDirectory) throws IOException
	{
		Path configFile = clientDirectory.resolve("claude_desktop_config.json");
		Path acceptingCli = writeAcceptingCli(clientDirectory);
		List<McpClientConfigurator> clients =
			List.of(new ClaudeDesktopConfigFile(configFile), new ClaudeCodeClient(acceptingCli.toString()));

		List<String> lines = McpConfigInstaller.installFor(Path.of("/opt/pcgen/bin/pcgen-mcp"), clients);

		assertEquals(List.of(
			"Claude Desktop (" + configFile + "): server 'pcgen' registered.",
			"Claude Code CLI: server 'pcgen' registered."), lines);
	}

	private static Path writeAcceptingCli(Path directory) throws IOException
	{
		Path script = directory.resolve("claude");
		Files.writeString(script, "#!/bin/sh\nexit 0\n");
		Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwxr-xr-x"));
		return script;
	}
}
