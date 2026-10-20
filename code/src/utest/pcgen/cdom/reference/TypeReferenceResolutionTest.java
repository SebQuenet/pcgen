package pcgen.cdom.reference;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pcgen.cdom.base.Loadable;
import pcgen.cdom.enumeration.ListKey;
import pcgen.cdom.enumeration.Type;
import pcgen.core.Ability;
import pcgen.core.AbilityCategory;
import pcgen.core.Equipment;
import pcgen.rules.context.RuntimeReferenceContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TypeReferenceResolutionTest
{
	private RuntimeReferenceContext referenceContext;
	private ReferenceManufacturer<Ability> featManufacturer;

	@BeforeEach
	void createFeatManufacturer()
	{
		referenceContext = RuntimeReferenceContext.createRuntimeReferenceContext();
		featManufacturer = referenceContext.getManufacturerFac(new AbilityCategory("TestFeat"));
	}

	private void featOfTypes(String key, String... typeNames)
	{
		Ability feat = featManufacturer.constructObject(key);
		for (String typeName : typeNames)
		{
			feat.addToListFor(ListKey.TYPE, Type.getConstant(typeName));
		}
	}

	private static <T extends Loadable> List<String> resolvedKeys(CDOMGroupRef<T> reference)
	{
		return reference.getContainedObjects().stream().map(T::getKeyName).toList();
	}

	@Test
	void resolvesToTheObjectsHavingEveryTypeInKeyOrder()
	{
		featOfTypes("Shake It Off", "Combat", "Teamwork");
		featOfTypes("Power Attack", "Combat");
		featOfTypes("Coordinated Defense", "Teamwork", "Combat");
		featOfTypes("Iron Will", "General");
		CDOMGroupRef<Ability> combatTeamwork = featManufacturer.getTypeReference("combat", "TEAMWORK");

		featManufacturer.resolveReferences(null);

		assertEquals(List.of("Coordinated Defense", "Shake It Off"), resolvedKeys(combatTeamwork));
	}

	@Test
	void resolvesEquipmentWithItsOwnTypeMatching()
	{
		ReferenceManufacturer<Equipment> equipmentManufacturer = referenceContext.getManufacturer(Equipment.class);
		Equipment longsword = equipmentManufacturer.constructObject("Longsword");
		longsword.addToListFor(ListKey.TYPE, Type.getConstant("Weapon"));
		longsword.addToListFor(ListKey.TYPE, Type.getConstant("Melee"));
		equipmentManufacturer.constructObject("Rope").addToListFor(ListKey.TYPE, Type.getConstant("Goods"));
		CDOMGroupRef<Equipment> meleeWeapons = equipmentManufacturer.getTypeReference("Weapon", "Melee");

		equipmentManufacturer.resolveReferences(null);

		assertEquals(List.of("Longsword"), resolvedKeys(meleeWeapons));
	}
}
