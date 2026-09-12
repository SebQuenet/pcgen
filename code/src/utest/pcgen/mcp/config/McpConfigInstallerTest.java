package pcgen.mcp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
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
		List<McpClientConfigurator> clients =
			McpConfigInstaller.clientsFor(Optional.of(Path.of("/home/joe/.config/Claude/claude_desktop_config.json")));

		assertEquals(
			List.of("Claude Desktop (/home/joe/.config/Claude/claude_desktop_config.json)", "Claude Code CLI"),
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
	void reportsOneLinePerClientItOffersTheServerTo(@TempDir Path clientDirectory)
	{
		Path configFile = clientDirectory.resolve("claude_desktop_config.json");
		List<McpClientConfigurator> clients =
			List.of(new ClaudeDesktopConfigFile(configFile), new ClaudeCodeClient("/bin/true"));

		List<String> lines = McpConfigInstaller.installFor(Path.of("/opt/pcgen/bin/pcgen-mcp"), clients);

		assertEquals(List.of(
			"Claude Desktop (" + configFile + "): server 'pcgen' registered.",
			"Claude Code CLI: server 'pcgen' registered."), lines);
	}
}
