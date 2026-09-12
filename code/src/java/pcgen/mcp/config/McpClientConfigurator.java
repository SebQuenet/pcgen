package pcgen.mcp.config;

import java.nio.file.Path;

/**
 * A client that can be told where to find the PCGen MCP server.
 */
public interface McpClientConfigurator
{
	/** How this client is named in the report the user reads. */
	String description();

	ConfigInstallOutcome install(Path serverExecutable);
}
