package pcgen.mcp.tools;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import pcgen.facade.core.CharacterFacade;
import pcgen.io.ExportUtilities;
import pcgen.system.BatchExporter;
import pcgen.mcp.McpSessionManager;

public final class ExportTools
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private ExportTools()
	{
	}

	public static SyncToolSpecification exportCharacter(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("export_character",
				"Export a character to PDF, HTML, or other formats using a template",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"template_path": { "type": "string", "description": "Path to the export template file (.xslt for PDF, .ftl for HTML/text)" },
							"output_path": { "type": "string", "description": "Path for the output file" }
						},
						"required": ["character_id", "template_path", "output_path"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					String templatePath = (String) args.get("template_path");
					String outputPath = (String) args.get("output_path");

					File templateFile = new File(templatePath);
					File outputFile = new File(outputPath);

					if (!templateFile.exists())
					{
						return errorResult("Template file not found: " + templatePath);
					}

					boolean success;
					if (ExportUtilities.isPdfTemplate(templateFile))
					{
						success = BatchExporter.exportCharacterToPDF(character, outputFile, templateFile);
					}
					else
					{
						success = BatchExporter.exportCharacterToNonPDF(character, outputFile, templateFile);
					}

					if (success)
					{
						return toResult(Map.of("status", "exported", "output", outputFile.getAbsolutePath()));
					}
					else
					{
						return errorResult("Export failed. Check template and output paths.");
					}
				}
				catch (Exception e)
				{
					return errorResult("Export error: " + e.getMessage());
				}
			}
		);
	}

	private static CallToolResult toResult(Object data)
	{
		try
		{
			String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(data);
			return new CallToolResult(json, false);
		}
		catch (JsonProcessingException e)
		{
			return new CallToolResult(data.toString(), false);
		}
	}

	private static CallToolResult errorResult(String message)
	{
		return new CallToolResult(message, true);
	}
}
