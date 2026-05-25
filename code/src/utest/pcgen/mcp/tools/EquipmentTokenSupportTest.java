package pcgen.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EquipmentTokenSupportTest
{
	@Test
	void buildSpellsTokenProducesCanonicalLine()
	{
		String token = EquipmentTokenSupport.buildSpellsToken(
			"Sceptre de Timéon", "Holy Smite", "1", "SceptreCL");
		assertEquals(
			"SPELLS:Sceptre de Timéon|TIMES=1|CASTERLEVEL=SceptreCL|Holy Smite",
			token);
	}
}
