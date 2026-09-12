package pcgen.mcp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class ServerExecutableTest
{
	@Test
	void prefersTheLauncherPathPublishedByJpackage()
	{
		Optional<Path> location = ServerExecutable.location(
			Optional.of("/opt/pcgen/bin/pcgen-mcp"), Optional.of("/usr/lib/jvm/java-25/bin/java"));

		assertEquals(Optional.of(Path.of("/opt/pcgen/bin/pcgen-mcp")), location);
	}

	@Test
	void fallsBackToTheCommandThatStartedThisProcess()
	{
		Optional<Path> location = ServerExecutable.location(Optional.empty(), Optional.of("/usr/bin/pcgen-mcp"));

		assertEquals(Optional.of(Path.of("/usr/bin/pcgen-mcp")), location);
	}

	@Test
	void findsNothingWhenNeitherIsKnown()
	{
		Optional<Path> location = ServerExecutable.location(Optional.empty(), Optional.empty());

		assertEquals(Optional.empty(), location);
	}
}
