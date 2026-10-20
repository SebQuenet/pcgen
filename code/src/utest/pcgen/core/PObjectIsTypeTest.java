package pcgen.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pcgen.cdom.enumeration.ListKey;
import pcgen.cdom.enumeration.Type;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PObjectIsTypeTest
{
	private Ability combatTeamworkFeat;

	@BeforeEach
	void createFeat()
	{
		combatTeamworkFeat = new Ability();
		combatTeamworkFeat.addToListFor(ListKey.TYPE, Type.getConstant("Combat"));
		combatTeamworkFeat.addToListFor(ListKey.TYPE, Type.getConstant("Teamwork"));
	}

	@Test
	void matchesWhenEveryDottedTypeIsPresentWhateverTheCase()
	{
		assertTrue(combatTeamworkFeat.isType("teamwork.COMBAT"));
	}

	@Test
	void doesNotMatchWhenOneDottedTypeIsMissing()
	{
		assertFalse(combatTeamworkFeat.isType("Combat.Magic"));
	}

	@Test
	void ignoresTheTypePrefixes()
	{
		assertTrue(combatTeamworkFeat.isType("TYPE=Combat") && combatTeamworkFeat.isType("TYPE.Teamwork")
			&& combatTeamworkFeat.isType("!Combat"));
	}

	@Test
	void neverMatchesAnEmptyType()
	{
		assertFalse(combatTeamworkFeat.isType(""));
	}
}
