package pcgen.mcp.config;

import java.nio.file.Path;
import java.util.List;

/**
 * The command line through which the Claude Code CLI registers an MCP server.
 */
public final class ClaudeCodeCommand
{
	static final String EXECUTABLE_NAME = "claude";
	private static final String SERVER_NAME = "pcgen";

	private ClaudeCodeCommand()
	{
	}

	public static List<String> addServer(String cliExecutable, Path serverExecutable)
	{
		return List.of(cliExecutable, "mcp", "add", "--scope", "user", SERVER_NAME, "--", serverExecutable.toString());
	}
}
