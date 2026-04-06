package pcgen.mcp.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import pcgen.facade.core.CharacterFacade;
import pcgen.io.ExportHandler;
import pcgen.io.ExportUtilities;
import pcgen.system.BatchExporter;
import pcgen.system.ConfigurationSettings;
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

	public static SyncToolSpecification getCharacterSheet(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("get_character_sheet",
				"Export a character sheet as inline content in markdown, toon, json, or xml format. Returns the rendered content directly.",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"format": { "type": "string", "description": "Output format: markdown (default), toon, json, xml", "default": "markdown" }
						},
						"required": ["character_id"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					String format = args.containsKey("format") ? (String) args.get("format") : "markdown";

					File templateFile = resolveBuiltInTemplate(format);
					if (templateFile == null || !templateFile.exists())
					{
						return errorResult("Template not found for format: " + format);
					}

					ExportHandler handler = ExportHandler.createExportHandler(templateFile);
					StringWriter sw = new StringWriter();
					BufferedWriter bw = new BufferedWriter(sw);
					character.export(handler, bw);
					bw.flush();

					return new CallToolResult(sw.toString(), false);
				}
				catch (Exception e)
				{
					return errorResult("Export error: " + e.getMessage());
				}
			}
		);
	}

	private static File resolveBuiltInTemplate(String format)
	{
		String templateName = switch (format.toLowerCase())
		{
			case "markdown", "md" -> "csheet_llm.md.ftl";
			case "toon" -> "csheet_llm.toon.ftl";
			case "json" -> "csheet_llm.json.ftl";
			case "xml" -> "base.xml.ftl";
			default -> format;
		};

		String outputSheetsDir = ConfigurationSettings.getOutputSheetsDir();

		// Try game-mode-specific path first
		File template = new File(outputSheetsDir + "/d20/fantasy/htmlxml/" + templateName);
		if (template.exists())
		{
			return template;
		}

		// Fall back to base outputsheets directory
		template = new File(outputSheetsDir + "/" + templateName);
		if (template.exists())
		{
			return template;
		}

		// Try as absolute path
		template = new File(templateName);
		return template;
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
