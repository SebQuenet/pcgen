package pcgen.rules.context;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pcgen.cdom.base.AssociatedPrereqObject;
import pcgen.cdom.base.CDOMReference;
import pcgen.cdom.enumeration.AssociationKey;
import pcgen.cdom.list.ClassSpellList;
import pcgen.cdom.reference.CDOMDirectSingleRef;
import pcgen.core.spell.Spell;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsolidatedListCommitStrategyTest
{
	private ConsolidatedListCommitStrategy strategy;
	private CDOMReference<ClassSpellList> wizardList;

	private static Spell spell(String key)
	{
		Spell spell = new Spell();
		spell.setName(key);
		return spell;
	}

	private List<String> tokensListing(Spell allowed)
	{
		Collection<AssociatedPrereqObject> associations = strategy.getAssociations(wizardList, allowed);
		if (associations == null)
		{
			return List.of();
		}
		return associations.stream()
			.map(association -> association.getAssociation(AssociationKey.TOKEN)).sorted().toList();
	}

	@BeforeEach
	void createStrategy()
	{
		strategy = new ConsolidatedListCommitStrategy();
		ClassSpellList wizardSpells = new ClassSpellList();
		wizardSpells.setName("Wizard");
		wizardList = CDOMDirectSingleRef.getRef(wizardSpells);
	}

	@Test
	void clearsOnlyTheAssociationsOfTheOwnerAndToken()
	{
		Spell fireball = spell("Fireball");
		Spell lightning = spell("Lightning Bolt");
		strategy.addToMasterList("CLASSES", fireball, wizardList, fireball);
		strategy.addToMasterList("DOMAINS", fireball, wizardList, fireball);
		strategy.addToMasterList("CLASSES", lightning, wizardList, lightning);

		strategy.clearAllMasterLists("CLASSES", fireball);

		assertEquals(List.of(List.of("DOMAINS"), List.of("CLASSES")),
			List.of(tokensListing(fireball), tokensListing(lightning)));
	}

	@Test
	void clearsTheAssociationsOfAnEqualOwner()
	{
		Spell fireball = spell("Fireball");
		strategy.addToMasterList("CLASSES", fireball, wizardList, fireball);

		strategy.clearAllMasterLists("CLASSES", spell("FIREBALL"));

		assertEquals(List.of(), tokensListing(fireball));
	}
}
