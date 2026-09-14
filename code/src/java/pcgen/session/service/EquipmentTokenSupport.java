package pcgen.session.service;

import java.util.ArrayList;
import java.util.List;

import pcgen.core.Equipment;
import pcgen.core.spell.Spell;
import pcgen.rules.context.LoadContext;

public final class EquipmentTokenSupport
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
	public static List<String> applyTokens(LoadContext context, Equipment equip, List<String> tokens)
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
	/** A spell-like ability an item is to grant, as a caller describes it. */
	public record SpellAbility(String spell, String timesPerDay, String casterLevel)
	{
	}

	public static List<String> applySpellAbilities(LoadContext context, Equipment equip, String itemId,
		List<SpellAbility> abilities)
	{
		List<String> failures = new ArrayList<>();
		if (abilities == null || abilities.isEmpty())
		{
			return failures;
		}
		List<String> spellTokens = new ArrayList<>();
		for (SpellAbility ability : abilities)
		{
			if (ability.spell() == null || ability.timesPerDay() == null || ability.casterLevel() == null)
			{
				failures.add(ability + " (spell, times_per_day and caster_level are required)");
				continue;
			}
			Spell found = context.getReferenceContext()
				.silentlyGetConstructedCDOMObject(Spell.class, ability.spell());
			if (found == null)
			{
				failures.add(ability.spell() + " (spell not found in loaded data)");
				continue;
			}
			spellTokens.add(buildSpellsToken(itemId, found.getKeyName(),
				ability.timesPerDay(), ability.casterLevel()));
		}
		failures.addAll(applyTokens(context, equip, spellTokens));
		return failures;
	}
}
