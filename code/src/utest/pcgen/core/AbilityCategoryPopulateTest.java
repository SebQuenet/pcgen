package pcgen.core;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pcgen.cdom.enumeration.ListKey;
import pcgen.cdom.enumeration.Type;
import pcgen.cdom.reference.CDOMDirectSingleRef;
import pcgen.cdom.reference.ReferenceManufacturer;
import pcgen.rules.context.RuntimeReferenceContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbilityCategoryPopulateTest
{
	private RuntimeReferenceContext referenceContext;
	private AbilityCategory featCategory;
	private ReferenceManufacturer<Ability> featManufacturer;

	@BeforeEach
	void createFeatCategory()
	{
		referenceContext = RuntimeReferenceContext.createRuntimeReferenceContext();
		featCategory = new AbilityCategory("TestFeat");
		featManufacturer = referenceContext.getManufacturerFac(featCategory);
	}

	private AbilityCategory childCategoryOfTypes(String key, String... typeNames)
	{
		AbilityCategory childCategory = new AbilityCategory(key);
		childCategory.setAbilityCategory(CDOMDirectSingleRef.getRef(featCategory));
		for (String typeName : typeNames)
		{
			childCategory.addAbilityType(Type.getConstant(typeName));
		}
		return childCategory;
	}

	private Ability featOfTypes(String key, String... typeNames)
	{
		Ability feat = featManufacturer.constructObject(key);
		for (String typeName : typeNames)
		{
			feat.addToListFor(ListKey.TYPE, Type.getConstant(typeName));
		}
		return feat;
	}

	private List<String> populatedKeys(AbilityCategory childCategory)
	{
		ReferenceManufacturer<Ability> childManufacturer = referenceContext.getManufacturerFac(childCategory);
		childCategory.populate(featManufacturer, childManufacturer, null);
		return childManufacturer.getAllObjects().stream().map(Ability::getKeyName).sorted().toList();
	}

	@Test
	void takesEveryParentAbilityOfAnyOfItsTypes()
	{
		featOfTypes("Power Attack", "Combat");
		featOfTypes("Iron Will", "General");
		featOfTypes("Spell Focus", "Magic", "General");
		featOfTypes("Run", "Movement");

		List<String> populated = populatedKeys(childCategoryOfTypes("Combat Or Magic", "Combat", "Magic"));

		assertEquals(List.of("Power Attack", "Spell Focus"), populated);
	}

	@Test
	void seesAbilitiesAddedToTheParentAfterAnotherCategoryWasPopulated()
	{
		featOfTypes("Power Attack", "Combat");
		populatedKeys(childCategoryOfTypes("Combat Feats", "Combat"));
		featOfTypes("Cleave", "Combat");

		List<String> populated = populatedKeys(childCategoryOfTypes("Fighter Feats", "Combat"));

		assertEquals(List.of("Cleave", "Power Attack"), populated);
	}
}
