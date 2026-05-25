package pcgen.mcp.tools;

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
}
