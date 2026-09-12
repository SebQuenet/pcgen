package pcgen.mcp.config;

/**
 * Turns an install outcome into the line the user reads in their terminal.
 */
public final class ConfigInstallReport
{
	private ConfigInstallReport()
	{
	}

	public static String describe(String client, ConfigInstallOutcome outcome)
	{
		return client + ": " + switch (outcome)
		{
			case ConfigInstallOutcome.Installed installed -> installed.backupFile()
				.map(backup -> "server 'pcgen' registered, previous configuration saved as " + backup + ".")
				.orElse("server 'pcgen' registered.");
			case ConfigInstallOutcome.ClientNotFound ignored -> "not installed on this machine, skipped.";
			case ConfigInstallOutcome.ConfigUnreadable unreadable ->
				"could not be read, left untouched (" + unreadable.reason() + ").";
			case ConfigInstallOutcome.InstallFailed failed ->
				"registration failed (" + failed.reason() + ").";
		};
	}
}
