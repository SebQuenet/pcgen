package plugin.jepcommands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import pcgen.AbstractCharacterTestCase;
import pcgen.core.PlayerCharacter;

import org.junit.jupiter.api.Test;

/**
 * {@code MasterVarCommandTest} tests the functioning of the jep mastervar plugin
 */
class MasterVarCommandTest extends AbstractCharacterTestCase
{
	@Test
	void aCharacterWithNoMasterReadsZeroFromItsMaster()
	{
		PlayerCharacter character = getCharacter();
		setPCStat(character, dex, 14);

		Float damageBonus = character.getVariableValue("max(mastervar(\"STR\"),DEX)+3", "");

		assertEquals(5.0f, damageBonus, 0.1f);
	}
}
