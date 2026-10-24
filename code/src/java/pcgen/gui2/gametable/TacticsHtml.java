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
package pcgen.gui2.gametable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import pcgen.core.PlayerCharacter;
import pcgen.core.tactics.TacticalAttack;
import pcgen.core.tactics.TacticalBlock;
import pcgen.core.tactics.TacticalBuff;
import pcgen.core.tactics.TacticalCapabilityList;
import pcgen.core.tactics.TacticalCreature;
import pcgen.core.tactics.TacticalItem;
import pcgen.core.tactics.TacticalLiteral;
import pcgen.core.tactics.TacticalNote;
import pcgen.core.tactics.TacticalParseFailure;
import pcgen.core.tactics.TacticalParseSuccess;
import pcgen.core.tactics.TacticalPlanParser;
import pcgen.core.tactics.TacticalReference;
import pcgen.core.tactics.TacticalResource;
import pcgen.core.tactics.TacticalRow;
import pcgen.core.tactics.TacticalSection;
import pcgen.core.tactics.TacticalSpellList;
import pcgen.core.tactics.TacticalStep;
import pcgen.core.tactics.TacticalSubject;
import pcgen.core.tactics.TacticalTag;
import pcgen.io.tactics.ResolvedSpell;
import pcgen.io.tactics.ResolvedWeapon;
import pcgen.io.tactics.TacticalResolver;
import pcgen.system.LanguageBundle;

/**
 * Writes a monster's tactical sheet as simple HTML for the Swing pane of the fight, below its
 * stat block: each section under its title, each step as when, then what to do. The numbers a
 * {@code @weapon} or {@code @var} reference points at are read off the monster as built, its
 * templates included.
 */
final class TacticsHtml
{
	private TacticsHtml()
	{
	}

	/**
	 * @param monster a monster PCGen computed.
	 * @return its tactical sheet, empty when it has none.
	 */
	static String of(PlayerCharacter monster)
	{
		Optional<String> plan = monster.getDisplay().getTacticalPlan();
		if (plan.isEmpty())
		{
			return "";
		}
		StringBuilder html = new StringBuilder("<table width='100%' cellspacing='0' cellpadding='3'>");
		switch (TacticalPlanParser.parse(plan.get()))
		{
			case TacticalParseSuccess success -> {
				TacticalResolver resolver = new TacticalResolver(monster);
				for (TacticalSection section : success.sheet().sections())
				{
					SheetHtml.sectionTitle(html, section.title());
					section.blocks().forEach(block -> block(html, block, resolver));
				}
			}
			case TacticalParseFailure failure -> {
				SheetHtml.sectionTitle(html, LanguageBundle.getString("in_tactical_does_not_read"));
				failure.errors().forEach(error -> line(html, escape(
					LanguageBundle.getFormattedString("in_tactical_error", error.line(), error.message()))));
			}
		}
		return html.append("</table>").toString();
	}

	private static void block(StringBuilder html, TacticalBlock block, TacticalResolver resolver)
	{
		switch (block)
		{
			case TacticalStep step -> step(html, step);
			case TacticalNote note -> line(html, "<b>" + escape(note.title()) + "</b><br>"
				+ escape(note.prose()).replace("\n", "<br>"));
			case TacticalAttack attack -> attack(html, attack, resolver);
			case TacticalResource resource -> line(html, "<b>" + escape(resource.label()) + "</b> : "
				+ escape(valueOf(resource.maximum(), resolver)) + muted(resource.action()));
			case TacticalSpellList spells -> spells(html, spells, resolver);
			case TacticalCreature creature -> rows(html, creature.name() + " — " + creature.source() + ", "
				+ creature.duration(), creature.rows());
			case TacticalItem item -> rows(html, item.name(), item.rows());
			case TacticalCapabilityList list -> line(html, "<b>" + escape(list.title()) + "</b><br>" + String.join(
				"<br>", list.capabilities().stream().map(capability -> escape(capability.name()) + muted(
					String.join(" · ", List.of(capability.action(), capability.uses(), capability.effect()).stream()
						.filter(text -> !text.isBlank()).toList()))).toList()));
			case TacticalBuff buff -> line(html, "<b>" + escape(buff.label()) + "</b>" + muted(buff.duration())
				+ (buff.note().isBlank() ? "" : "<br>" + escape(buff.note())));
		}
	}

	private static void step(StringBuilder html, TacticalStep step)
	{
		StringBuilder text = new StringBuilder("<b>").append(escape(step.trigger())).append("</b><br>")
			.append(escape(step.actions()));
		if (!step.note().isBlank())
		{
			text.append("<br><i><span style='color:").append(SheetHtml.MUTED).append("'>").append(escape(step.note()))
				.append("</span></i>");
		}
		line(html, text.toString());
	}

	private static void attack(StringBuilder html, TacticalAttack attack, TacticalResolver resolver)
	{
		Optional<ResolvedWeapon> weapon = switch (attack.subject())
		{
			case TacticalReference reference -> resolver.weapon(reference.key());
			case TacticalLiteral ignored -> Optional.empty();
		};
		String name = weapon.map(ResolvedWeapon::name).orElse(attack.subject().display());
		String toHit = attack.toHit().orElseGet(() -> weapon.map(ResolvedWeapon::toHit).orElse(""));
		String damage = attack.damage().orElseGet(() -> weapon.map(ResolvedWeapon::damage).orElse(""));
		String critical = attack.critical().orElseGet(() -> weapon.map(ResolvedWeapon::critical).orElse(""));
		StringBuilder text = new StringBuilder(escape(name)).append(" <b>").append(escape(toHit)).append("</b> ")
			.append(escape(damage)).append(muted(critical));
		attack.variants().forEach(variant -> text.append("<br>— ").append(escape(variant.label())).append(" : ")
			.append(escape(variant.effect())));
		if (!attack.note().isBlank())
		{
			text.append("<br><i>").append(escape(attack.note())).append("</i>");
		}
		line(html, text.toString());
	}

	private static void spells(StringBuilder html, TacticalSpellList block, TacticalResolver resolver)
	{
		Map<String, List<String>> tagsByName = block.tags().stream().collect(Collectors.toMap(
			tag -> tag.spellName().toLowerCase(Locale.ROOT), TacticalTag::tags, (first, second) -> first));
		Map<Integer, List<ResolvedSpell>> byLevel = resolver.spells(block.source()).stream().collect(
			Collectors.groupingBy(ResolvedSpell::level, java.util.TreeMap::new, Collectors.toList()));
		Function<ResolvedSpell, String> describe = spell -> escape(spell.name())
			+ (spell.times() > 1 ? " ×" + spell.times() : "")
			+ muted(String.join(", ", tagsByName.getOrDefault(spell.name().toLowerCase(Locale.ROOT), List.of())));
		byLevel.forEach((level, spells) -> line(html, "<b>" + level + "</b> : "
			+ String.join(", ", spells.stream().map(describe).toList())));
	}

	private static void rows(StringBuilder html, String title, List<TacticalRow> rows)
	{
		line(html, "<b>" + escape(title) + "</b><br>" + String.join("<br>", rows.stream()
			.map(row -> escape(row.label()) + " : " + escape(row.content())).toList()));
	}

	private static String valueOf(TacticalSubject subject, TacticalResolver resolver)
	{
		return switch (subject)
		{
			case TacticalLiteral literal -> literal.text();
			case TacticalReference reference -> switch (reference.kind())
			{
				case VAR -> resolver.number(reference.key()).orElse(reference.display());
				case WEAPON -> resolver.weapon(reference.key()).map(ResolvedWeapon::name).orElse(reference.display());
				case TEMPBONUS -> reference.key();
			};
		};
	}

	private static String muted(String text)
	{
		return text.isBlank() ? "" : " <span style='color:" + SheetHtml.MUTED + "'>(" + escape(text) + ")</span>";
	}

	private static void line(StringBuilder html, String content)
	{
		html.append("<tr><td colspan='2'>").append(content).append("</td></tr>");
	}

	private static String escape(String text)
	{
		return EncountersPanel.escape(text);
	}
}
