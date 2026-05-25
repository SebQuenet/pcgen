package pcgen.mcp.tools;

import java.util.ArrayList;
import java.util.List;

import pcgen.core.Equipment;
import pcgen.rules.context.LoadContext;

final class EquipmentTokenSupport
{
	private EquipmentTokenSupport()
	{
	}

	/**
	 * Build a SPELLS: token value granting one spell-like ability.
	 * Format per plugin/lsttokens/SpellsLst.java:60 —
	 * SPELLS:&lt;spellbook&gt;|TIMES=&lt;formula&gt;|CASTERLEVEL=&lt;formula&gt;|&lt;spell&gt;
	 *
	 * @param itemId      stable per-item label (the custom item name)
	 * @param spell       spell key
	 * @param timesPerDay integer or formula; -1 = at will
	 * @param casterLevel integer, variable, or formula
	 */
	static String buildSpellsToken(String itemId, String spell, String timesPerDay, String casterLevel)
	{
		return "SPELLS:" + itemId
			+ "|TIMES=" + timesPerDay
			+ "|CASTERLEVEL=" + casterLevel
			+ "|" + spell;
	}

	/**
	 * Apply a list of raw LST token strings ("NAME:value") to the equipment using the same
	 * parser + validation as data loading. Returns a list of human-readable failure messages;
	 * an empty list means every token applied. Tokens that parse are committed onto the item.
	 */
	static List<String> applyTokens(LoadContext context, Equipment equip, List<String> tokens)
	{
		List<String> failures = new ArrayList<>();
		if (tokens == null || tokens.isEmpty())
		{
			return failures;
		}
		for (String raw : tokens)
		{
			int colon = raw.indexOf(':');
			if (colon < 0)
			{
				failures.add(raw + " (not a NAME:value token)");
				continue;
			}
			String name = raw.substring(0, colon);
			String value = raw.substring(colon + 1);
			try
			{
				if (!context.processToken(equip, name, value))
				{
					failures.add(raw + " (rejected by token parser)");
				}
			}
			catch (Exception e)
			{
				String m = e.getMessage();
				failures.add(raw + " (" + (m != null ? m : e.getClass().getSimpleName()) + ")");
			}
		}
		context.commit();
		return failures;
	}
}
