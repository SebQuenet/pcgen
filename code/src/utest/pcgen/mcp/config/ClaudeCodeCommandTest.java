package pcgen.mcp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class ClaudeCodeCommandTest
{
	@Test
	void registersTheServerForEveryProjectOfTheUser()
	{
		Path serverExecutable = Path.of("/opt/pcgen/bin/pcgen-mcp");

		List<String> command = ClaudeCodeCommand.addServer("claude", serverExecutable);

		assertEquals(
			List.of("claude", "mcp", "add", "--scope", "user", "pcgen", "--", serverExecutable.toString()),
			command);
	}
}
