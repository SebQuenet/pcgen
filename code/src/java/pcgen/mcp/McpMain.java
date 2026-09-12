package pcgen.mcp;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import io.modelcontextprotocol.server.McpSyncServer;

import pcgen.system.ConfigurationSettings;
import pcgen.system.FacadeFactory;
import pcgen.system.Main;
import pcgen.system.PCGenTaskExecutor;
import pcgen.persistence.CampaignFileLoader;
import pcgen.persistence.GameModeFileLoader;
import pcgen.mcp.config.McpConfigInstaller;
import pcgen.mcp.config.ServerExecutable;
import pcgen.util.GracefulExit;
import pcgen.util.Logging;

/**
 * Entry point for the PCGen MCP server. Initializes PCGen in headless mode
 * and starts a JSON-RPC server on stdio.
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

		String settingsDir = parseSettingsDir(args);
		if (settingsDir != null)
		{
			ConfigurationSettings.setSystemProperty(ConfigurationSettings.SETTINGS_FILES_PATH, settingsDir);
		}
		else
		{
			ConfigurationSettings.setSystemProperty(
				ConfigurationSettings.SETTINGS_FILES_PATH,
				ConfigurationSettings.getDefaultSettingsFilesPath());
		}

		Main.loadProperties(false);

		Logging.log(Level.INFO, "Loading plugins and game data...");
		PCGenTaskExecutor executor = new PCGenTaskExecutor();
		executor.addPCGenTask(Main.createLoadPluginTask());
		executor.addPCGenTask(new GameModeFileLoader());
		executor.addPCGenTask(new CampaignFileLoader());
		executor.run();

		FacadeFactory.initialize();
		Logging.log(Level.INFO, "PCGen data loaded successfully.");

		McpSessionManager session = new McpSessionManager();
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

	private static String parseSettingsDir(String[] args)
	{
		for (int i = 0; i < args.length - 1; i++)
		{
			if ("-s".equals(args[i]) || "--settingsdir".equals(args[i]))
			{
				return args[i + 1];
			}
		}
		return null;
	}
}
