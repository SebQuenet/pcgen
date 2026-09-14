package pcgen.mcp;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import io.modelcontextprotocol.server.McpSyncServer;

import pcgen.mcp.config.McpConfigInstaller;
import pcgen.mcp.config.ServerExecutable;
import pcgen.session.HeadlessBootstrap;
import pcgen.session.PcgenSession;
import pcgen.util.GracefulExit;
import pcgen.util.Logging;

/**
 * Entry point for the PCGen MCP server. Brings PCGen up headless and speaks
 * JSON-RPC over stdio.
 */
public class McpMain
{
	private static final String INSTALL_CONFIG_FLAG = "--install-config";

	private McpMain()
	{
	}

	public static void main(String... args)
	{
		if (List.of(args).contains(INSTALL_CONFIG_FLAG))
		{
			installIntoClients();
			return;
		}

		Logging.log(Level.INFO, "Starting PCGen MCP server...");

		HeadlessBootstrap.start(HeadlessBootstrap.settingsDirFrom(args));

		PcgenSession session = new PcgenSession();
		McpSyncServer server = McpServerBuilder.build(session);

		Logging.log(Level.INFO, "MCP server started. Listening on stdio.");
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			Logging.log(Level.INFO, "Shutting down MCP server...");
			server.close();
		}));
	}

	private static void installIntoClients()
	{
		Optional<Path> serverExecutable = ServerExecutable.forCurrentProcess();
		if (serverExecutable.isEmpty())
		{
			System.out.println("Cannot tell where this program is installed; add the server to your client by hand.");
			GracefulExit.exit(1);
			return;
		}
		McpConfigInstaller.installFor(serverExecutable.get(), McpConfigInstaller.clientsForCurrentPlatform())
			.forEach(System.out::println);
		System.out.println("Restart your client to pick up the change.");
	}
}
