package pcgen.mcp.resources;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;

import pcgen.facade.core.DataSetFacade;
import pcgen.mcp.McpSessionManager;
import pcgen.mcp.serialization.FacadeSerializer;

public final class DataSetResources
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private DataSetResources()
	{
	}

	public static List<SyncResourceSpecification> createResources(McpSessionManager session)
	{
		List<SyncResourceSpecification> resources = new ArrayList<>();

		resources.add(new SyncResourceSpecification(
			new Resource("pcgen://data/races", "Available Races",
				"List of all races in the currently loaded data set", "application/json", null),
			(exchange, request) -> {
				DataSetFacade dataSet = session.getCurrentDataSet();
				if (dataSet == null)
				{
					return textResult(request.uri(), "No sources loaded. Use load_sources tool first.");
				}
				var list = FacadeSerializer.serializeList(dataSet.getRaces(), FacadeSerializer::serializeRace);
				return jsonResult(request.uri(), list);
			}
		));

		resources.add(new SyncResourceSpecification(
			new Resource("pcgen://data/classes", "Available Classes",
				"List of all character classes in the currently loaded data set", "application/json", null),
			(exchange, request) -> {
				DataSetFacade dataSet = session.getCurrentDataSet();
				if (dataSet == null)
				{
					return textResult(request.uri(), "No sources loaded.");
				}
				var list = FacadeSerializer.serializeList(dataSet.getClasses(), FacadeSerializer::serializePCClass);
				return jsonResult(request.uri(), list);
			}
		));

		resources.add(new SyncResourceSpecification(
			new Resource("pcgen://data/stats", "Available Stats",
				"List of ability score stats (e.g., STR, DEX, CON)", "application/json", null),
			(exchange, request) -> {
				DataSetFacade dataSet = session.getCurrentDataSet();
				if (dataSet == null)
				{
					return textResult(request.uri(), "No sources loaded.");
				}
				var list = FacadeSerializer.serializeList(dataSet.getStats(), FacadeSerializer::serializeStat);
				return jsonResult(request.uri(), list);
			}
		));

		resources.add(new SyncResourceSpecification(
			new Resource("pcgen://data/alignments", "Available Alignments",
				"List of alignments (e.g., Lawful Good, Chaotic Evil)", "application/json", null),
			(exchange, request) -> {
				DataSetFacade dataSet = session.getCurrentDataSet();
				if (dataSet == null)
				{
					return textResult(request.uri(), "No sources loaded.");
				}
				var list = FacadeSerializer.serializeList(dataSet.getAlignments(), FacadeSerializer::serializeAlignment);
				return jsonResult(request.uri(), list);
			}
		));

		resources.add(new SyncResourceSpecification(
			new Resource("pcgen://data/skills", "Available Skills",
				"List of all skills in the currently loaded data set", "application/json", null),
			(exchange, request) -> {
				DataSetFacade dataSet = session.getCurrentDataSet();
				if (dataSet == null)
				{
					return textResult(request.uri(), "No sources loaded.");
				}
				var list = FacadeSerializer.serializeList(dataSet.getSkills(), FacadeSerializer::serializeSkill);
				return jsonResult(request.uri(), list);
			}
		));

		resources.add(new SyncResourceSpecification(
			new Resource("pcgen://data/deities", "Available Deities",
				"List of all deities in the currently loaded data set", "application/json", null),
			(exchange, request) -> {
				DataSetFacade dataSet = session.getCurrentDataSet();
				if (dataSet == null)
				{
					return textResult(request.uri(), "No sources loaded.");
				}
				var list = FacadeSerializer.serializeList(dataSet.getDeities(), FacadeSerializer::serializeDeity);
				return jsonResult(request.uri(), list);
			}
		));

		resources.add(new SyncResourceSpecification(
			new Resource("pcgen://data/equipment", "Available Equipment",
				"List of all equipment in the currently loaded data set", "application/json", null),
			(exchange, request) -> {
				DataSetFacade dataSet = session.getCurrentDataSet();
				if (dataSet == null)
				{
					return textResult(request.uri(), "No sources loaded.");
				}
				var list = FacadeSerializer.serializeList(dataSet.getEquipment(), FacadeSerializer::serializeEquipment);
				return jsonResult(request.uri(), list);
			}
		));

		resources.add(new SyncResourceSpecification(
			new Resource("pcgen://data/templates", "Available Templates",
				"List of all character templates in the currently loaded data set", "application/json", null),
			(exchange, request) -> {
				DataSetFacade dataSet = session.getCurrentDataSet();
				if (dataSet == null)
				{
					return textResult(request.uri(), "No sources loaded.");
				}
				var list = FacadeSerializer.serializeList(dataSet.getTemplates(), FacadeSerializer::serializeTemplate);
				return jsonResult(request.uri(), list);
			}
		));

		resources.add(new SyncResourceSpecification(
			new Resource("pcgen://data/kits", "Available Kits",
				"List of all character kits in the currently loaded data set", "application/json", null),
			(exchange, request) -> {
				DataSetFacade dataSet = session.getCurrentDataSet();
				if (dataSet == null)
				{
					return textResult(request.uri(), "No sources loaded.");
				}
				var list = FacadeSerializer.serializeList(dataSet.getKits(), FacadeSerializer::serializeKit);
				return jsonResult(request.uri(), list);
			}
		));

		return resources;
	}

	private static ReadResourceResult jsonResult(String uri, Object data)
	{
		try
		{
			String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(data);
			return new ReadResourceResult(List.of(new TextResourceContents(uri, "application/json", json)));
		}
		catch (JsonProcessingException e)
		{
			return textResult(uri, "JSON serialization error: " + e.getMessage());
		}
	}

	private static ReadResourceResult textResult(String uri, String text)
	{
		return new ReadResourceResult(List.of(new TextResourceContents(uri, "text/plain", text)));
	}
}
