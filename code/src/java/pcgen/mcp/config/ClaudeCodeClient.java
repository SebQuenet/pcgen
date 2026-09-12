package pcgen.mcp.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

/**
 * The Claude Code command line tool, which registers MCP servers on its own.
 */
public class ClaudeCodeClient implements McpClientConfigurator
{
	private final String cliExecutable;

	public ClaudeCodeClient(String cliExecutable)
	{
		this.cliExecutable = cliExecutable;
	}

	@Override
	public String description()
	{
		return "Claude Code CLI";
	}

	@Override
	public ConfigInstallOutcome install(Path executable)
	{
		ProcessBuilder builder = new ProcessBuilder(ClaudeCodeCommand.addServer(cliExecutable, executable));
		builder.redirectErrorStream(true);
		try
		{
			Process process = builder.start();
			String output = readFully(process.getInputStream());
			if (process.waitFor() == 0)
			{
				return new ConfigInstallOutcome.Installed(Optional.empty());
			}
			return new ConfigInstallOutcome.InstallFailed(output.strip());
		}
		catch (IOException e)
		{
			return new ConfigInstallOutcome.ClientNotFound();
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			return new ConfigInstallOutcome.InstallFailed(e.getMessage());
		}
	}

	private static String readFully(InputStream stream) throws IOException
	{
		try (stream)
		{
			return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
