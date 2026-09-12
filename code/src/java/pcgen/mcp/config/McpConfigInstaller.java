package pcgen.mcp.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Offers the PCGen MCP server to every client this machine is known to run.
 */
public final class McpConfigInstaller
{
	private McpConfigInstaller()
	{
	}

	public static List<McpClientConfigurator> clientsForCurrentPlatform()
	{
		return clientsFor(ClaudeDesktopLocation.forCurrentPlatform());
	}

	public static List<McpClientConfigurator> clientsFor(Optional<Path> claudeDesktopConfigFile)
	{
		List<McpClientConfigurator> clients = new ArrayList<>();
		claudeDesktopConfigFile.map(ClaudeDesktopConfigFile::new).ifPresent(clients::add);
		clients.add(new ClaudeCodeClient(ClaudeCodeCommand.EXECUTABLE_NAME));
		return List.copyOf(clients);
	}

	public static List<String> installFor(Path serverExecutable, List<McpClientConfigurator> clients)
	{
		return clients.stream()
			.map(client -> ConfigInstallReport.describe(client.description(), client.install(serverExecutable)))
			.toList();
	}
}
