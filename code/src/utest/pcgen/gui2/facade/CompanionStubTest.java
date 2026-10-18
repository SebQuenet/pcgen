package pcgen.gui2.facade;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import pcgen.core.Race;
import pcgen.facade.util.DefaultListFacade;

import org.junit.jupiter.api.Test;

class CompanionStubTest
{
	@Test
	void refreshingUnchangedCompanionOptionsKeepsExistingRows()
	{
		Race race = new Race();
		CompanionStub original = new CompanionStub(race, "Eidolon");
		DefaultListFacade<CompanionStub> options = new DefaultListFacade<>(List.of(original));

		options.updateContents(List.of(new CompanionStub(race, "Eidolon")));

		assertSame(original, options.getElementAt(0));
	}
}
