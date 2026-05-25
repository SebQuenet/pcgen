package pcgen.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pcgen.core.Equipment;
import pcgen.rules.context.ConsolidatedListCommitStrategy;
import pcgen.rules.context.LoadContext;
import pcgen.rules.context.RuntimeLoadContext;
import pcgen.rules.context.RuntimeReferenceContext;
import plugin.bonustokens.Var;
import plugin.lsttokens.BonusLst;
import plugin.lsttokens.DefineLst;
import plugin.lsttokens.testsupport.TokenRegistration;

class EquipmentTokenSupportTest
{
	private LoadContext context;

	@BeforeEach
	void setUp()
	{
		TokenRegistration.clearTokens();
		TokenRegistration.register(new DefineLst());
		TokenRegistration.register(new BonusLst());
		TokenRegistration.register(Var.class);
		context = new RuntimeLoadContext(
			RuntimeReferenceContext.createRuntimeReferenceContext(),
			new ConsolidatedListCommitStrategy());
	}

	@Test
	void buildSpellsTokenProducesCanonicalLine()
	{
		String token = EquipmentTokenSupport.buildSpellsToken(
			"Sceptre de Timéon", "Holy Smite", "1", "SceptreCL");
		assertEquals(
			"SPELLS:Sceptre de Timéon|TIMES=1|CASTERLEVEL=SceptreCL|Holy Smite",
			token);
	}

	@Test
	void applyTokensAppliesValidTokensWithNoFailures()
	{
		Equipment equip = new Equipment();
		equip.setName("Test Item");
		List<String> failures = EquipmentTokenSupport.applyTokens(
			context, equip, List.of("DEFINE:SceptreCL|10", "BONUS:VAR|SceptreCL|5"));
		assertTrue(failures.isEmpty(), () -> "unexpected failures: " + failures);
	}

	@Test
	void applyTokensReportsNonTokenString()
	{
		Equipment equip = new Equipment();
		equip.setName("Test Item");
		List<String> failures = EquipmentTokenSupport.applyTokens(
			context, equip, List.of("GARBAGE_NO_COLON"));
		assertEquals(1, failures.size());
		assertTrue(failures.get(0).contains("GARBAGE_NO_COLON"));
	}

	@Test
	void applyTokensReportsMalformedTokenValue()
	{
		Equipment equip = new Equipment();
		equip.setName("Test Item");
		List<String> failures = EquipmentTokenSupport.applyTokens(
			context, equip, List.of("DEFINE:OnlyAName"));
		assertFalse(failures.isEmpty());
	}

	@Test
	void applyTokensReturnsEmptyForNull()
	{
		Equipment equip = new Equipment();
		equip.setName("Test Item");
		assertTrue(EquipmentTokenSupport.applyTokens(context, equip, null).isEmpty());
	}
}
