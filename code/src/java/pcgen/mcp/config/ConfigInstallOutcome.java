package pcgen.mcp.config;

import java.nio.file.Path;
import java.util.Optional;

/**
 * What happened when the MCP server entry was written into a client's configuration.
 * Which client it was is known by the caller, not carried here.
 */
public sealed interface ConfigInstallOutcome
{
	/** The entry was written, over a copy of the previous configuration when there was one. */
	record Installed(Optional<Path> backupFile) implements ConfigInstallOutcome
	{
	}

	/** The client is not installed on this machine. */
	record ClientNotFound() implements ConfigInstallOutcome
	{
	}

	/** The configuration exists but could not be parsed, so it was left untouched. */
	record ConfigUnreadable(String reason) implements ConfigInstallOutcome
	{
	}

	/** The entry could not be written, most often because the configuration is not writable. */
	record InstallFailed(String reason) implements ConfigInstallOutcome
	{
	}
}
