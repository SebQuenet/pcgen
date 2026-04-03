package pcgen.mcp;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

import pcgen.mcp.resources.DataSetResources;
import pcgen.mcp.tools.AbilityTools;
import pcgen.mcp.tools.BiographyTools;
import pcgen.mcp.tools.CharacterBuildTools;
import pcgen.mcp.tools.CharacterLifecycleTools;
import pcgen.mcp.tools.ChoiceTools;
import pcgen.mcp.tools.DeityDomainTools;
import pcgen.mcp.tools.CustomEquipmentTools;
import pcgen.mcp.tools.EquipmentTools;
import pcgen.mcp.tools.ExportTools;
import pcgen.mcp.tools.SkillTools;
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
			// Source management
			.tools(
				SourceTools.listGameModes(session),
				SourceTools.listSources(session),
				SourceTools.loadSources(session),
				// Character lifecycle
				CharacterLifecycleTools.createCharacter(session),
				CharacterLifecycleTools.getCharacter(session),
				CharacterLifecycleTools.openCharacter(session),
				CharacterLifecycleTools.saveCharacter(session),
				CharacterLifecycleTools.closeCharacter(session),
				// Character build basics
				CharacterBuildTools.setName(session),
				CharacterBuildTools.setRace(session),
				CharacterBuildTools.addClassLevel(session),
				CharacterBuildTools.setAbilityScore(session),
				CharacterBuildTools.setAlignment(session),
				// Abilities (feats, traits, etc.)
				AbilityTools.listAbilityCategories(session),
				AbilityTools.listAbilities(session),
				AbilityTools.addAbility(session),
				AbilityTools.removeAbility(session),
				// Skills
				SkillTools.investSkillPoints(session),
				SkillTools.getSkillSummary(session),
				// Deity & Domains
				DeityDomainTools.setDeity(session),
				DeityDomainTools.addDomain(session),
				DeityDomainTools.removeDomain(session),
				// Equipment
				EquipmentTools.buyEquipment(session),
				EquipmentTools.sellEquipment(session),
				EquipmentTools.getInventory(session),
				EquipmentTools.setFunds(session),
				// Custom/Magic Equipment
				CustomEquipmentTools.listEquipmentModifiers(session),
				CustomEquipmentTools.customizeEquipment(session),
				// Biography & XP
				BiographyTools.setBiography(session),
				BiographyTools.getBiography(session),
				BiographyTools.setXP(session),
				// Export
				ExportTools.exportCharacter(session),
				// Chooser interaction
				ChoiceTools.getPendingChoices(session),
				ChoiceTools.resolveChoice(session)
			)
			.resources(DataSetResources.createResources(session))
			.build();
	}
}
