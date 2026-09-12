package pcgen.mcp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class ClaudeCodeClientTest
{
	private static final Path SERVER_EXECUTABLE = Path.of("/opt/pcgen/bin/pcgen-mcp");

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void reportsTheServerAsRegisteredWhenTheCliAcceptsIt()
	{
		ConfigInstallOutcome outcome = new ClaudeCodeClient("/bin/true").install(SERVER_EXECUTABLE);

		assertEquals(new ConfigInstallOutcome.Installed(Optional.empty()), outcome);
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void reportsAFailureWhenTheCliRejectsTheServer()
	{
		ConfigInstallOutcome outcome = new ClaudeCodeClient("/bin/false").install(SERVER_EXECUTABLE);

		assertInstanceOf(ConfigInstallOutcome.InstallFailed.class, outcome);
	}

	@Test
	void reportsTheClientAsMissingWhenTheCliIsNotInstalled()
	{
		ConfigInstallOutcome outcome = new ClaudeCodeClient("/no/such/claude").install(SERVER_EXECUTABLE);

		assertEquals(new ConfigInstallOutcome.ClientNotFound(), outcome);
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void repeatsWhatTheCliComplainedAbout(@TempDir Path directory) throws IOException
	{
		Path failingCli = writeScript(directory, "echo 'unknown scope' >&2\nexit 1\n");

		ConfigInstallOutcome outcome = new ClaudeCodeClient(failingCli.toString()).install(SERVER_EXECUTABLE);

		assertTrue(failureReasonOf(outcome).contains("unknown scope"), failureReasonOf(outcome));
	}

	private static String failureReasonOf(ConfigInstallOutcome outcome)
	{
		return assertInstanceOf(ConfigInstallOutcome.InstallFailed.class, outcome).reason();
	}

	private static Path writeScript(Path directory, String body) throws IOException
	{
		Path script = directory.resolve("claude");
		Files.writeString(script, "#!/bin/sh\n" + body);
		Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwxr-xr-x"));
		return script;
	}
}
