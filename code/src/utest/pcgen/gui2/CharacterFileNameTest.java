package pcgen.gui2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CharacterFileNameTest
{
	@Test
	void namesACharacterAfterItsLevel()
	{
		assertEquals("Shalendra-11.pcg", CharacterFileName.suggest("Shalendra", 11, 0));
	}

	@Test
	void addsTheMythicTierOnTwoDigits()
	{
		assertEquals("Drakès-13-04.pcg", CharacterFileName.suggest("Drakès", 13, 4));
	}

	@Test
	void writesALowLevelOnTwoDigits()
	{
		assertEquals("Leina-05.pcg", CharacterFileName.suggest("Leina", 5, 0));
	}
}
