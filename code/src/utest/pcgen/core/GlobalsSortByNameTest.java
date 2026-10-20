package pcgen.core;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import pcgen.cdom.base.CDOMObject;
import pcgen.cdom.enumeration.StringKey;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalsSortByNameTest
{
	private static Ability ability(String keyName, String displayName, String sortKey)
	{
		Ability ability = new Ability();
		ability.setName(displayName);
		ability.put(StringKey.KEY_NAME, keyName);
		if (sortKey != null)
		{
			ability.put(StringKey.SORT_KEY, sortKey);
		}
		return ability;
	}

	@Test
	void sortsInTheOrderOfTheNameComparator()
	{
		List<Ability> abilities = new ArrayList<>(List.of(
			ability("zeta", "Zeta", null),
			ability("eclair", "Éclair", null),
			ability("apple", "apple", null),
			ability("banana", "Banana", null),
			ability("sorted-last", "Aardvark", "zzz"),
			ability("twin-b", "Twin", null),
			ability("twin-a", "Twin", null),
			ability("same-sort-b", "Beta", "same"),
			ability("same-sort-a", "Alpha", "same"),
			ability("localized", "English", null)));
		abilities.getLast().setLocalizedName("Abricot");
		List<Ability> comparatorOrder = new ArrayList<>(abilities);
		comparatorOrder.sort(CDOMObject.P_OBJECT_NAME_COMP);

		Globals.sortPObjectListByName(abilities);

		assertEquals(comparatorOrder, abilities);
	}
}
