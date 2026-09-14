/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package pcgen.io.tactics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import pcgen.core.Globals;
import pcgen.core.PCCheck;
import pcgen.core.PlayerCharacter;
import pcgen.core.tactics.DeltaTarget;
import pcgen.core.tactics.SpellSource;
import pcgen.core.tactics.TacticalAttack;
import pcgen.core.tactics.TacticalBuff;
import pcgen.core.tactics.TacticalCapability;
import pcgen.core.tactics.TacticalCapabilityList;
import pcgen.core.tactics.TacticalBlock;
import pcgen.core.tactics.TacticalCreature;
import pcgen.core.tactics.TacticalDelta;
import pcgen.core.tactics.TacticalItem;
import pcgen.core.tactics.TacticalLiteral;
import pcgen.core.tactics.TacticalNote;
import pcgen.core.tactics.TacticalParseError;
import pcgen.core.tactics.TacticalParseFailure;
import pcgen.core.tactics.TacticalParseSuccess;
import pcgen.core.tactics.TacticalPlanParser;
import pcgen.core.tactics.TacticalReference;
import pcgen.core.tactics.TacticalResource;
import pcgen.core.tactics.TacticalRow;
import pcgen.core.tactics.TacticalSection;
import pcgen.core.tactics.TacticalSessionState;
import pcgen.core.tactics.TacticalSpellList;
import pcgen.core.tactics.TacticalStep;
import pcgen.core.tactics.TacticalSubject;
import pcgen.core.tactics.TacticalTag;
import pcgen.core.tactics.TacticalVariant;
import pcgen.system.LanguageBundle;

/**
 * The tactical sheet as an output sheet template sees it: the plan read off the
 * character, every reference resolved, and what the session has used up.
 *
 * <p>
 * Nested maps and lists rather than the records themselves, because
 * FreeMarker's default wrapper reads bean properties and a record has none.
 *
 * <p>
 * The values are the text as it was written. Escaping belongs to the template,
 * which declares {@code output_format="HTML"} so that FreeMarker escapes every
 * interpolation and none can be forgotten.
 */
public final class TacticalOutputModel
{
	private static final String KIND = "kind";

	private TacticalOutputModel()
	{
	}

	/**
	 * Build the model for a character.
	 *
	 * @param character the character whose sheet is being rendered
	 * @return the data model, with {@code present} false when the character has
	 *         no tactical plan at all
	 */
	public static Map<String, Object> of(PlayerCharacter character)
	{
		Optional<String> plan = character.getDisplay().getTacticalPlan();
		if (plan.isEmpty())
		{
			return Map.of("present", Boolean.FALSE, "errors", List.of(), "sections", List.of(), "session",
				sessionOf(TacticalSessionState.untouched()), "labels", labels(), "saveTargets", saveTargets());
		}
		TacticalSessionState session = character.getDisplay().getTacticalSession();
		return switch (TacticalPlanParser.parse(plan.get()))
		{
			case TacticalParseSuccess success -> Map.of("present", Boolean.TRUE, "errors", List.of(), "sections",
				sectionsOf(success.sheet().sections(), new TacticalResolver(character), session), "session",
				sessionOf(session), "labels", labels(), "saveTargets", saveTargets());
			case TacticalParseFailure failure -> Map.of("present", Boolean.TRUE, "errors", errorsOf(failure),
				"sections", List.of(), "session", sessionOf(session), "labels", labels(), "saveTargets",
				saveTargets());
		};
	}

	/**
	 * The sheet's own wording, in the user's language. The plan's text is the
	 * writer's; these are the headings around it.
	 *
	 * @return the labels the template shows, keyed by what they name
	 */
	/**
	 * Which buff target each save in the vitals band answers to.
	 *
	 * <p>
	 * The band prints the saves through {@code CHECK.n.NAME}, whose text is
	 * translated, so a buff cannot find a save by what it reads. The checks come
	 * back here in the same order the band loops over them, named by their
	 * English key, which is what a delta target is named after.
	 *
	 * @return one target name per check, in check order, empty where a check
	 *         answers to no target
	 */
	private static List<String> saveTargets()
	{
		List<String> targets = new ArrayList<>();
		for (PCCheck check : Globals.getContext().getReferenceContext()
			.getSortkeySortedCDOMObjects(PCCheck.class))
		{
			String key = check.getKeyName().toUpperCase(Locale.ROOT);
			targets.add(java.util.Arrays.stream(DeltaTarget.values()).map(DeltaTarget::name)
				.filter(key::equals).findFirst().map(name -> name.toLowerCase(Locale.ROOT)).orElse(""));
		}
		return targets;
	}

	private static Map<String, Object> labels()
	{
		return Map.ofEntries(Map.entry("attack", LanguageBundle.getString("in_tactical_attack")),
			Map.entry("hit", LanguageBundle.getString("in_tactical_hit")),
			Map.entry("damage", LanguageBundle.getString("in_tactical_damage")),
			Map.entry("critical", LanguageBundle.getString("in_tactical_critical")),
			Map.entry("normalTarget", LanguageBundle.getString("in_tactical_normal_target")),
			Map.entry("hitPoints", LanguageBundle.getString("in_tactical_hit_points")),
			Map.entry("everyTag", LanguageBundle.getString("in_tactical_every_tag")),
			Map.entry("prepared", LanguageBundle.getString("in_tactical_prepared")),
			Map.entry("known", LanguageBundle.getString("in_tactical_known")),
			Map.entry("noSpells", LanguageBundle.getString("in_tactical_no_spells")),
			Map.entry("empty", LanguageBundle.getString("in_tactical_empty")),
			Map.entry("doesNotRead", LanguageBundle.getString("in_tactical_does_not_read")),
			Map.entry("noWeapon", LanguageBundle.getString("in_tactical_no_weapon")),
			Map.entry("noVariable", LanguageBundle.getString("in_tactical_no_variable")),
			Map.entry("unmatchedTags", LanguageBundle.getString("in_tactical_unmatched_tags")),
			Map.entry("throughPcgen", LanguageBundle.getString("in_tactical_through_pcgen")),
			Map.entry("noBonus", LanguageBundle.getString("in_tactical_no_bonus")));
	}

	private static List<Map<String, Object>> errorsOf(TacticalParseFailure failure)
	{
		List<Map<String, Object>> errors = new ArrayList<>();
		for (TacticalParseError error : failure.errors())
		{
			errors.add(Map.of("line", error.line(), "message", error.message(), "text",
				LanguageBundle.getFormattedString("in_tactical_error", error.line(), error.message())));
		}
		return errors;
	}

	private static Map<String, Object> sessionOf(TacticalSessionState session)
	{
		return Map.of("damage", session.damageTaken(), "spent", Map.copyOf(session.resourcesSpent()));
	}

	private static List<Map<String, Object>> sectionsOf(List<TacticalSection> sections, TacticalResolver resolver,
		TacticalSessionState session)
	{
		List<Map<String, Object>> described = new ArrayList<>();
		for (TacticalSection section : sections)
		{
			List<Map<String, Object>> blocks = new ArrayList<>();
			for (TacticalBlock block : section.blocks())
			{
				blocks.add(blockOf(block, resolver, session));
			}
			described.add(Map.of("title", section.title(), "blocks", blocks));
		}
		return described;
	}

	private static Map<String, Object> blockOf(TacticalBlock block, TacticalResolver resolver,
		TacticalSessionState session)
	{
		return switch (block)
		{
			case TacticalStep step -> Map.of(KIND, "step", "trigger", step.trigger(), "actions", step.actions(),
				"note", step.note());
			case TacticalNote note -> Map.of(KIND, "note", "title", note.title(), "prose",
				List.of(note.prose().split("\n", -1)));
			case TacticalResource resource -> resourceOf(resource, resolver, session);
			case TacticalAttack attack -> attackOf(attack, resolver);
			case TacticalCreature creature -> creatureOf(creature);
			case TacticalSpellList spells -> spellsOf(spells, resolver);
			case TacticalCapabilityList capabilities -> capabilitiesOf(capabilities);
			case TacticalItem item -> itemOf(item);
			case TacticalBuff buff -> buffOf(buff, resolver, session);
		};
	}

	private static Map<String, Object> resourceOf(TacticalResource resource, TacticalResolver resolver,
		TacticalSessionState session)
	{
		Optional<String> maximum = valueOf(resource.maximum(), resolver);
		Map<String, Object> described = new LinkedHashMap<>();
		described.put(KIND, "resource");
		described.put("label", resource.label());
		described.put("action", resource.action());
		described.put("spent", session.spent(resource.label()));
		described.put("maximum", maximum.orElse(""));
		described.put("pips", pipsOf(maximum));
		described.put("unresolved", maximum.isEmpty());
		described.put("missing", maximum.isEmpty() ? resource.maximum().display() : "");
		return described;
	}

	/**
	 * @param maximum the resolved maximum, if there is one
	 * @return how many pips to draw, zero when the maximum is not a whole
	 *         number the sheet can count out
	 */
	private static int pipsOf(Optional<String> maximum)
	{
		try
		{
			return maximum.map(Integer::parseInt).filter(count -> count > 0).orElse(0);
		}
		catch (NumberFormatException notCountable)
		{
			return 0;
		}
	}

	private static Optional<String> valueOf(TacticalSubject subject, TacticalResolver resolver)
	{
		return switch (subject)
		{
			case TacticalLiteral literal -> Optional.of(literal.text());
			case TacticalReference reference -> switch (reference.kind())
			{
				case VAR -> resolver.number(reference.key());
				case WEAPON -> resolver.weapon(reference.key()).map(ResolvedWeapon::name);
				case TEMPBONUS -> Optional.of(reference.key());
			};
		};
	}

	private static Map<String, Object> attackOf(TacticalAttack attack, TacticalResolver resolver)
	{
		Optional<ResolvedWeapon> resolved = switch (attack.subject())
		{
			case TacticalReference reference -> resolver.weapon(reference.key());
			case TacticalLiteral ignored -> Optional.empty();
		};
		boolean unresolved = attack.subject() instanceof TacticalReference && resolved.isEmpty();

		List<Map<String, Object>> variants = new ArrayList<>();
		for (TacticalVariant variant : attack.variants())
		{
			variants.add(Map.of("label", variant.label(), "effect", variant.effect()));
		}

		Map<String, Object> described = new LinkedHashMap<>();
		described.put(KIND, "attack");
		described.put("name", resolved.map(ResolvedWeapon::name).orElse(attack.subject().display()));
		described.put("toHit", attack.toHit().orElseGet(() -> resolved.map(ResolvedWeapon::toHit).orElse("")));
		described.put("damage", attack.damage().orElseGet(() -> resolved.map(ResolvedWeapon::damage).orElse("")));
		described.put("critical",
			attack.critical().orElseGet(() -> resolved.map(ResolvedWeapon::critical).orElse("")));
		described.put("variants", variants);
		described.put("note", attack.note());
		described.put("unresolved", unresolved);
		described.put("missing", unresolved ? attack.subject().display() : "");
		return described;
	}

	private static Map<String, Object> capabilitiesOf(TacticalCapabilityList list)
	{
		List<Map<String, Object>> capabilities = new ArrayList<>();
		for (TacticalCapability capability : list.capabilities())
		{
			capabilities.add(Map.of("name", capability.name(), "tags", capability.tags(), "action",
				capability.action(), "uses", capability.uses(), "effect", capability.effect()));
		}
		return Map.of(KIND, "capabilities", "title", list.title(), "tags", list.tags(), "capabilities",
			capabilities);
	}

	private static Map<String, Object> itemOf(TacticalItem item)
	{
		List<Map<String, Object>> rows = new ArrayList<>();
		for (TacticalRow row : item.rows())
		{
			rows.add(Map.of("label", row.label(), "content", row.content()));
		}
		return Map.of(KIND, "item", "name", item.name(), "rows", rows);
	}

	/**
	 * A buff carries what the page needs to apply it: its deltas, keyed by what
	 * they change, and whether PCGen can be asked to recompute instead.
	 */
	private static Map<String, Object> buffOf(TacticalBuff buff, TacticalResolver resolver,
		TacticalSessionState session)
	{
		List<Map<String, Object>> deltas = new ArrayList<>();
		for (TacticalDelta delta : buff.gives())
		{
			deltas.add(Map.of("target", delta.target().name().toLowerCase(Locale.ROOT), "amount", delta.amount(),
				"signed", delta.signed()));
		}
		boolean throughPcgen = buff.applies().isPresent();
		String bonus = buff.applies().map(TacticalReference::key).orElse("");
		return Map.ofEntries(Map.entry(KIND, "buff"), Map.entry("label", buff.label()),
			Map.entry("duration", buff.duration()), Map.entry("deltas", deltas),
			Map.entry("throughPcgen", throughPcgen), Map.entry("bonus", bonus),
			Map.entry("known", !throughPcgen || resolver.hasTemporaryBonus(bonus)),
			Map.entry("active", session.isBuffActive(buff.label())), Map.entry("note", buff.note()));
	}

	private static Map<String, Object> creatureOf(TacticalCreature creature)
	{
		List<Map<String, Object>> rows = new ArrayList<>();
		for (TacticalRow row : creature.rows())
		{
			rows.add(Map.of("label", row.label(), "content", row.content()));
		}
		return Map.of(KIND, "creature", "name", creature.name(), "source", creature.source(), "duration",
			creature.duration(), "rows", rows);
	}

	private static Map<String, Object> spellsOf(TacticalSpellList block, TacticalResolver resolver)
	{
		Map<String, List<String>> tagsByName = new LinkedHashMap<>();
		for (TacticalTag tag : block.tags())
		{
			tagsByName.put(tag.spellName().toLowerCase(Locale.ROOT), tag.tags());
		}

		List<Map<String, Object>> spells = new ArrayList<>();
		java.util.Set<String> matched = new java.util.LinkedHashSet<>();
		for (ResolvedSpell spell : resolver.spells(block.source()))
		{
			String key = spell.name().toLowerCase(Locale.ROOT);
			List<String> tags = tagsByName.getOrDefault(key, List.of());
			if (!tags.isEmpty())
			{
				matched.add(key);
			}
			spells.add(Map.of("name", spell.name(), "level", spell.level(), "times", spell.times(), "tags", tags));
		}

		List<String> declared = block.tags().stream().flatMap(tag -> tag.tags().stream()).distinct()
			.sorted(Comparator.comparing(tag -> tag.toLowerCase(Locale.ROOT))).toList();
		List<String> unmatched = block.tags().stream().map(TacticalTag::spellName)
			.filter(name -> !matched.contains(name.toLowerCase(Locale.ROOT))).toList();

		return Map.of(KIND, "spells", "source",
			(block.source() == SpellSource.PREPARED) ? "prepared" : "known", "spells", spells, "tags", declared,
			"unmatched", unmatched);
	}
}
