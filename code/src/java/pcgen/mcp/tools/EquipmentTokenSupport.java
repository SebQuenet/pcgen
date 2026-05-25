package pcgen.mcp.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pcgen.core.Equipment;
import pcgen.core.spell.Spell;
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
		// Do not call resolveReferences here: at runtime the global LoadContext is already
		// resolved, so getCDOMReference (e.g. for a SPELLS: spell) hands back an already-resolved
		// direct reference. Re-resolving an already-resolved context throws
		// "Cannot resolve a Single Reference twice".
		context.commit();
		return failures;
	}

	/**
	 * Validate each spell exists in the loaded data, build a SPELLS: line per ability, and apply
	 * them. Each ability map must contain string entries "spell", "times_per_day" and
	 * "caster_level". Returns failure messages (empty == all granted).
	 */
	static List<String> applySpellAbilities(LoadContext context, Equipment equip, String itemId,
		List<Map<String, Object>> abilities)
	{
		List<String> failures = new ArrayList<>();
		if (abilities == null || abilities.isEmpty())
		{
			return failures;
		}
		List<String> spellTokens = new ArrayList<>();
		for (Map<String, Object> ability : abilities)
		{
			Object spell = ability.get("spell");
			Object times = ability.get("times_per_day");
			Object casterLevel = ability.get("caster_level");
			if (spell == null || times == null || casterLevel == null)
			{
				failures.add(ability + " (spell, times_per_day and caster_level are required)");
				continue;
			}
			Spell found = context.getReferenceContext()
				.silentlyGetConstructedCDOMObject(Spell.class, spell.toString());
			if (found == null)
			{
				failures.add(spell + " (spell not found in loaded data)");
				continue;
			}
			spellTokens.add(buildSpellsToken(itemId, found.getKeyName(),
				times.toString(), casterLevel.toString()));
		}
		failures.addAll(applyTokens(context, equip, spellTokens));
		return failures;
	}
}
