package pcgen.mcp.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import pcgen.core.Deity;
import pcgen.core.Domain;
import pcgen.core.QualifiedObject;
import pcgen.facade.core.CharacterFacade;
import pcgen.facade.core.DataSetFacade;
import pcgen.mcp.McpSessionManager;

public final class DeityDomainTools
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private DeityDomainTools()
	{
	}

	public static SyncToolSpecification setDeity(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("set_deity",
				"Set a character's deity",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"deity_key": { "type": "string", "description": "Deity key or name" }
						},
						"required": ["character_id", "deity_key"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					String deityKey = (String) args.get("deity_key");
					DataSetFacade dataSet = character.getDataSet();

					Deity found = null;
					for (Deity deity : dataSet.getDeities())
					{
						if (deity.getKeyName().equalsIgnoreCase(deityKey) || deity.getDisplayName().equalsIgnoreCase(deityKey))
						{
							found = deity;
							break;
						}
					}
					if (found == null)
					{
						return errorResult("Deity not found: " + deityKey);
					}
					character.setDeity(found);
					return toResult(Map.of("status", "ok", "deity", found.getDisplayName()));
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification addDomain(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("add_domain",
				"Add a domain to a character (must have deity set and domain selections remaining)",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"domain_key": { "type": "string", "description": "Domain key or name" }
						},
						"required": ["character_id", "domain_key"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					String domainKey = (String) args.get("domain_key");

					QualifiedObject<Domain> found = null;
					for (QualifiedObject<Domain> qd : character.getAvailableDomains())
					{
						Domain domain = qd.getRawObject();
						if (domain.getKeyName().equalsIgnoreCase(domainKey) || domain.getDisplayName().equalsIgnoreCase(domainKey))
						{
							found = qd;
							break;
						}
					}
					if (found == null)
					{
						return errorResult("Domain not found or not available: " + domainKey);
					}

					int remaining = character.getRemainingDomainSelectionsRef().get();
					if (remaining <= 0)
					{
						return errorResult("No domain selections remaining");
					}

					character.addDomain(found);
					return toResult(Map.of("status", "ok", "domain", found.getRawObject().getDisplayName(),
						"remainingSelections", character.getRemainingDomainSelectionsRef().get()));
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	public static SyncToolSpecification removeDomain(McpSessionManager session)
	{
		return new SyncToolSpecification(
			new Tool("remove_domain",
				"Remove a domain from a character",
				"""
					{
						"type": "object",
						"properties": {
							"character_id": { "type": "string", "description": "Character ID" },
							"domain_key": { "type": "string", "description": "Domain key or name" }
						},
						"required": ["character_id", "domain_key"]
					}
					"""),
			(exchange, args) -> {
				try
				{
					CharacterFacade character = session.getCharacter((String) args.get("character_id"));
					String domainKey = (String) args.get("domain_key");

					QualifiedObject<Domain> found = null;
					for (QualifiedObject<Domain> qd : character.getDomains())
					{
						Domain domain = qd.getRawObject();
						if (domain.getKeyName().equalsIgnoreCase(domainKey) || domain.getDisplayName().equalsIgnoreCase(domainKey))
						{
							found = qd;
							break;
						}
					}
					if (found == null)
					{
						return errorResult("Character does not have domain: " + domainKey);
					}

					character.removeDomain(found);
					return toResult(Map.of("status", "ok", "removed", found.getRawObject().getDisplayName()));
				}
				catch (Exception e)
				{
					return errorResult(e.getMessage());
				}
			}
		);
	}

	private static CallToolResult toResult(Object data)
	{
		try
		{
			String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(data);
			return new CallToolResult(List.of(new TextContent(json)), false);
		}
		catch (JsonProcessingException e)
		{
			return new CallToolResult(List.of(new TextContent(data.toString())), false);
		}
	}

	private static CallToolResult errorResult(String message)
	{
		return new CallToolResult(List.of(new TextContent(message)), true);
	}
}
