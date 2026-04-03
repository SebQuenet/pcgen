package pcgen.mcp;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

import pcgen.mcp.resources.DataSetResources;
import pcgen.mcp.tools.CharacterBuildTools;
import pcgen.mcp.tools.CharacterLifecycleTools;
import pcgen.mcp.tools.SourceTools;

public final class McpServerBuilder
{
	private McpServerBuilder()
	{
	}

	public static McpSyncServer build(McpSessionManager session)
	{
		var transportProvider = new StdioServerTransportProvider(new ObjectMapper());

		return McpServer.sync(transportProvider)
			.serverInfo("pcgen", "1.0.0")
			.capabilities(McpSchema.ServerCapabilities.builder()
				.tools(true)
				.resources(false, false)
				.build())
			.tools(
				SourceTools.listGameModes(session),
				SourceTools.listSources(session),
				SourceTools.loadSources(session),
				CharacterLifecycleTools.createCharacter(session),
				CharacterLifecycleTools.getCharacter(session),
				CharacterLifecycleTools.openCharacter(session),
				CharacterLifecycleTools.saveCharacter(session),
				CharacterLifecycleTools.closeCharacter(session),
				CharacterBuildTools.setName(session),
				CharacterBuildTools.setRace(session),
				CharacterBuildTools.addClassLevel(session),
				CharacterBuildTools.setAbilityScore(session),
				CharacterBuildTools.setAlignment(session)
			)
			.resources(DataSetResources.createResources(session))
			.build();
	}
}
