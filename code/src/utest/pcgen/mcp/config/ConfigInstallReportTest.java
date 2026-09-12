package pcgen.mcp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class ConfigInstallReportTest
{
	private static final String CLIENT = "Claude Desktop (/home/joe/.config/Claude/claude_desktop_config.json)";

	@Test
	void confirmsARegistrationThatNeededNoBackup()
	{
		String line = ConfigInstallReport.describe(CLIENT, new ConfigInstallOutcome.Installed(Optional.empty()));

		assertEquals(CLIENT + ": server 'pcgen' registered.", line);
	}

	@Test
	void namesTheBackupItLeftBehind()
	{
		ConfigInstallOutcome outcome =
			new ConfigInstallOutcome.Installed(Optional.of(Path.of("/home/joe/config.json.bak")));

		String line = ConfigInstallReport.describe(CLIENT, outcome);

		assertEquals(CLIENT + ": server 'pcgen' registered, previous configuration saved as "
			+ "/home/joe/config.json.bak.", line);
	}

	@Test
	void saysWhichClientWasSkipped()
	{
		String line = ConfigInstallReport.describe(CLIENT, new ConfigInstallOutcome.ClientNotFound());

		assertEquals(CLIENT + ": not installed on this machine, skipped.", line);
	}

	@Test
	void warnsThatAnUnreadableConfigurationWasLeftAlone()
	{
		String line = ConfigInstallReport.describe(CLIENT, new ConfigInstallOutcome.ConfigUnreadable("bad token"));

		assertEquals(CLIENT + ": could not be read, left untouched (bad token).", line);
	}

	@Test
	void reportsWhyTheRegistrationFailed()
	{
		String line = ConfigInstallReport.describe(CLIENT, new ConfigInstallOutcome.InstallFailed("permission denied"));

		assertEquals(CLIENT + ": registration failed (permission denied).", line);
	}
}
