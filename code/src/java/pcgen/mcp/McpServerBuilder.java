package pcgen.mcp;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

import pcgen.mcp.resources.DataSetResources;
import pcgen.mcp.tools.BiographyTools;
import pcgen.mcp.tools.ChoiceTools;
import pcgen.mcp.tools.CustomEquipmentTools;
import pcgen.mcp.tools.DeityDomainTools;
import pcgen.mcp.tools.EquipmentSetTools;
import pcgen.mcp.tools.EquipmentTools;
import pcgen.mcp.tools.ExportTools;
import pcgen.mcp.tools.LanguageCompanionTools;
import pcgen.mcp.tools.SpellTools;
import pcgen.mcp.tools.TacticalSheetTools;
import pcgen.mcp.tools.TemplateTools;
import pcgen.mcp.tools.UtilityTools;
import pcgen.session.PcgenSession;
import pcgen.session.api.OperationRegistry;

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
			.tools(
				// Spells
				SpellTools.getAvailableSpells(session),
				SpellTools.getKnownSpells(session),
				SpellTools.addKnownSpell(session),
				SpellTools.removeKnownSpell(session),
				SpellTools.getPreparedSpells(session),
				SpellTools.addPreparedSpell(session),
				SpellTools.removePreparedSpell(session),
				SpellTools.getSpellbooks(session),
				SpellTools.addToSpellbook(session),
				SpellTools.batchAddPreparedSpells(session),
				// Deity & Domains
				DeityDomainTools.setDeity(session),
				DeityDomainTools.addDomain(session),
				DeityDomainTools.removeDomain(session),
				// Equipment - Purchase
				EquipmentTools.buyEquipment(session),
				EquipmentTools.sellEquipment(session),
				EquipmentTools.getInventory(session),
				EquipmentTools.setFunds(session),
				EquipmentTools.batchBuyEquipment(session),
				// Equipment - Custom/Magic
				CustomEquipmentTools.listEquipmentModifiers(session),
				CustomEquipmentTools.customizeEquipment(session),
				// Equipment - Wearing/Equipping
				EquipmentSetTools.listEquipmentSets(session),
				EquipmentSetTools.createEquipmentSet(session),
				EquipmentSetTools.getEquippedItems(session),
				EquipmentSetTools.equipItem(session),
				EquipmentSetTools.equipItems(session),
				EquipmentSetTools.unequipItem(session),
				// Templates & Temp Bonuses & Kits
				TemplateTools.addTemplate(session),
				TemplateTools.removeTemplate(session),
				TemplateTools.getTemplates(session),
				TemplateTools.addTempBonus(session),
				TemplateTools.removeTempBonus(session),
				TemplateTools.listTempBonuses(session),
				TemplateTools.addKit(session),
				// Languages & Companions
				LanguageCompanionTools.getLanguages(session),
				LanguageCompanionTools.addLanguage(session),
				LanguageCompanionTools.removeLanguage(session),
				LanguageCompanionTools.getCompanions(session),
				LanguageCompanionTools.addCompanion(session),
				LanguageCompanionTools.removeCompanion(session),
				// Biography & XP
				BiographyTools.setBiography(session),
				BiographyTools.getBiography(session),
				BiographyTools.setXP(session),
				// Tactical sheet
				TacticalSheetTools.setTacticalSheet(session),
				TacticalSheetTools.getTacticalSheet(session),
				TacticalSheetTools.clearTacticalSheet(session),
				TacticalSheetTools.listTacticalReferences(session),
				// Export
				ExportTools.exportCharacter(session),
				ExportTools.getCharacterSheet(session),
				// Utilities
				UtilityTools.getTodoList(session),
				UtilityTools.isQualifiedFor(session),
				UtilityTools.rollStats(session),
				UtilityTools.isDirty(session),
				UtilityTools.getCharacterDetails(session),
				// Chooser interaction
				ChoiceTools.getPendingChoices(session),
				ChoiceTools.resolveChoice(session)
			)
			.resources(DataSetResources.createResources(session))
			.build();
	}
}
