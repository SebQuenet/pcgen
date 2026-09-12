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
		List<String> command = ClaudeCodeCommand.addServer("claude", Path.of("/opt/pcgen/bin/pcgen-mcp"));

		assertEquals(
			List.of("claude", "mcp", "add", "--scope", "user", "pcgen", "--", "/opt/pcgen/bin/pcgen-mcp"),
			command);
	}
}
