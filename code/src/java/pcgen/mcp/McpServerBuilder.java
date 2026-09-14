package pcgen.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

import pcgen.mcp.resources.DataSetResources;
import pcgen.session.PcgenSession;
import pcgen.session.api.OperationRegistry;

/**
 * Dresses a session's operations as an MCP server speaking over stdio.
 */
public final class McpServerBuilder
{
	private McpServerBuilder()
	{
	}

	public static McpSyncServer build(PcgenSession session)
	{
		var transportProvider = new StdioServerTransportProvider(new ObjectMapper());
		OperationRegistry registry = OperationRegistry.forSession(session);

		return McpServer.sync(transportProvider)
			.serverInfo("pcgen", "1.0.0")
			.capabilities(McpSchema.ServerCapabilities.builder()
				.tools(true)
				.resources(false, false)
				.build())
			.tools(McpOperationAdapter.toolsFrom(registry))
			.resources(DataSetResources.createResources(session))
			.build();
	}
}
