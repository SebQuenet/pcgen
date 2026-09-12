package pcgen.mcp.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The configuration file through which Claude Desktop learns about MCP servers.
 */
public class ClaudeDesktopConfigFile implements McpClientConfigurator
{
	private static final String SERVERS_FIELD = "mcpServers";
	private static final String SERVER_NAME = "pcgen";
	private static final String COMMAND_FIELD = "command";
	private static final String BACKUP_SUFFIX = ".bak";

	private final ObjectMapper mapper = new ObjectMapper();
	private final Path configFile;

	public ClaudeDesktopConfigFile(Path configFile)
	{
		this.configFile = configFile;
	}

	@Override
	public String description()
	{
		return "Claude Desktop (" + configFile + ")";
	}

	@Override
	public ConfigInstallOutcome install(Path executable)
	{
		Path clientDirectory = configFile.getParent();
		if ((clientDirectory == null) || !Files.isDirectory(clientDirectory))
		{
			return new ConfigInstallOutcome.ClientNotFound();
		}

		ObjectNode root;
		try
		{
			root = readExistingConfig();
		}
		catch (IOException e)
		{
			return new ConfigInstallOutcome.ConfigUnreadable(e.getMessage());
		}
		root.withObjectProperty(SERVERS_FIELD).putObject(SERVER_NAME).put(COMMAND_FIELD, executable.toString());

		try
		{
			Optional<Path> backupFile = backupExistingConfig();
			Files.writeString(configFile, root.toPrettyString());
			return new ConfigInstallOutcome.Installed(backupFile);
		}
		catch (IOException e)
		{
			return new ConfigInstallOutcome.InstallFailed(e.getMessage());
		}
	}

	private Optional<Path> backupExistingConfig() throws IOException
	{
		if (!Files.exists(configFile))
		{
			return Optional.empty();
		}
		Path backupFile = configFile.resolveSibling(configFile.getFileName() + BACKUP_SUFFIX);
		Files.copy(configFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
		return Optional.of(backupFile);
	}

	private ObjectNode readExistingConfig() throws IOException
	{
		if (!Files.exists(configFile))
		{
			return mapper.createObjectNode();
		}
		return mapper.readValue(Files.readString(configFile), ObjectNode.class);
	}
}
