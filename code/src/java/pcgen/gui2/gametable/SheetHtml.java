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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import pcgen.core.conjurations.CreatureAttack;
import pcgen.core.conjurations.CreatureStats;
import pcgen.core.conjurations.SpecialAbilities;
import pcgen.core.gametable.CastSpell;
import pcgen.core.gametable.Combat;
import pcgen.core.gametable.Combatant;
import pcgen.core.gametable.Condition;
import pcgen.core.gametable.FeintDefense;
import pcgen.core.gametable.Health;
import pcgen.core.gametable.MonsterSheet;
import pcgen.core.gametable.PlayerGlance;
import pcgen.system.LanguageBundle;
import pcgen.util.FirstResultSearch;

/**
 * Writes the sheet the game master reads beside the fight, as simple HTML for a Swing
 * text pane: the numbers asked for every round in large type, then the stat block by
 * section, one ability to a line.
 */
final class SheetHtml
{
	/** Where a link to take a condition off points; the condition's name follows. */
	static final String REMOVE_LINK = "remove:";

	/** Where a link to take a condition off the whole group points; the condition's name follows. */
	static final String REMOVE_FROM_GROUP_LINK = "removeFromGroup:";

	/** Where a link to drop a spell cast points; the spell's name follows. */
	static final String DROP_SPELL_LINK = "dropSpell:";

	private static final String BODY = "<html><body style='font-family:sans-serif;font-size:14pt;margin:6px'>";
	private static final String END = "</body></html>";
	private static final String MUTED = "#6b5a48";
	private static final String FIGURE_BACKGROUND = "#f1ebe1";
	private static final String SECTION_BACKGROUND = "#7a2b1d";

	private SheetHtml()
	{
	}

	/**
	 * @param sheet a monster's sheet, before it enters an encounter.
	 * @return its stat block, without any fight state.
	 */
	static String preview(MonsterSheet sheet)
	{
		StringBuilder html = new StringBuilder(BODY);
		html.append("<p align='right' style='color:").append(MUTED).append("'>")
			.append(LanguageBundle.getString("in_gametable_challengeRating")).append(" <b>")
			.append(escape(sheet.challengeRating())).append("</b></p>");
		figures(html, String.valueOf(sheet.stats().maximumHitPoints()), sheet);
		statBlock(html, sheet);
		return html.append(END).toString();
	}

	/**
	 * @param combatant a monster in the fight.
	 * @param sheet     what PCGen computed of it, when it could be built.
	 * @param combat    the fight it is in, which names its group and the targets of its spells.
	 * @return its sheet, with its hit points left, its conditions and the spells it cast.
	 */
	static String monster(Combatant combatant, Optional<MonsterSheet> sheet, Combat combat)
	{
		StringBuilder html = head(combatant, sheet.map(MonsterSheet::challengeRating));
		Health health = combatant.health().orElseThrow();
		String hitPoints = health.remaining() + " / " + health.maximum();
		sheet.ifPresentOrElse(found -> {
			figures(html, hitPoints, found);
			conditions(html, combatant, combat);
			statBlock(html, found);
		}, () -> {
			html.append("<p style='font-size:20pt'><b>").append(LanguageBundle.getString("in_gametable_hitPoints"))
				.append(" ").append(hitPoints).append("</b></p>");
			html.append("<p><i>").append(LanguageBundle.getString("in_gametable_notBuilt")).append("</i></p>");
			conditions(html, combatant, combat);
		});
		return html.append(END).toString();
	}

	/**
	 * @param combatant a player's character or a creature it conjured.
	 * @param glance    the few numbers read from its file, when known.
	 * @param combat    the fight it is in, which names its group and the targets of its spells.
	 * @return its sheet: those numbers, its conditions and the spells it cast.
	 */
	static String player(Combatant combatant, Optional<PlayerGlance> glance, Combat combat)
	{
		StringBuilder html = head(combatant, Optional.empty());
		glance.ifPresent(found -> {
			html.append("<table width='100%' cellspacing='4' cellpadding='4'><tr>");
			figure(html, "in_gametable_perception", found.perception(), "");
			figure(html, "in_gametable_senseMotive", found.senseMotive(), "");
			html.append("</tr></table>");
			saves(html, found.fortitude(), found.reflex(), found.will());
		});
		combatant.roundsLeft().ifPresent(rounds -> html.append("<p><b>")
			.append(LanguageBundle.getString("in_gametable_roundsLeft")).append("</b> ").append(rounds).append("</p>"));
		conditions(html, combatant, combat);
		return html.append(END).toString();
	}

	private static StringBuilder head(Combatant combatant, Optional<String> challengeRating)
	{
		StringBuilder html = new StringBuilder(BODY);
		html.append("<table width='100%' cellspacing='0' cellpadding='0'><tr><td style='font-size:18pt'><b>")
			.append(escape(combatant.name())).append("</b></td>");
		challengeRating.ifPresent(rating -> html.append("<td align='right' valign='bottom' style='color:").append(MUTED)
			.append("'>").append(LanguageBundle.getString("in_gametable_challengeRating")).append(" <b>")
			.append(escape(rating)).append("</b></td>"));
		html.append("</tr></table>");
		combatant.recipe().ifPresent(recipe -> {
			String touches = EncountersPanel.touchesOf(recipe);
			html.append("<div style='font-size:11pt;color:").append(MUTED).append("'><i>")
				.append(escape(RetouchDialog.sourceText(recipe.source())))
				.append(touches.isEmpty() ? "" : " — " + escape(touches)).append("</i></div>");
		});
		return html;
	}

	/** The numbers asked for every round, in large type. */
	private static void figures(StringBuilder html, String hitPoints, MonsterSheet sheet)
	{
		CreatureStats stats = sheet.stats();
		html.append("<table width='100%' cellspacing='4' cellpadding='4'><tr>");
		figure(html, "in_gametable_hitPoints", hitPoints,
			LanguageBundle.getFormattedString("in_gametable_hitDice", stats.hitDice()));
		figure(html, "in_gametable_armorClass", String.valueOf(stats.armorClass()),
			LanguageBundle.getFormattedString("in_gametable_armorDetail", stats.touchArmorClass(),
				stats.flatFootedArmorClass()));
		switch (sheet.feint())
		{
			case FeintDefense.Difficulty difficulty -> figure(html, "in_gametable_feint",
				LanguageBundle.getFormattedString("in_gametable_feintDifficulty", difficulty.difficulty()),
				hindranceOf(difficulty));
			case FeintDefense.Impossible impossible -> figure(html, "in_gametable_feint", "—",
				LanguageBundle.getString("in_gametable_feintImpossible"));
		}
		figure(html, "in_gametable_initiativeColumn", stats.initiative(), "");
		html.append("</tr></table>");
	}

	private static String hindranceOf(FeintDefense.Difficulty difficulty)
	{
		return switch (difficulty.hindrance())
		{
			case NONE -> "";
			case NONHUMANOID -> LanguageBundle.getFormattedString("in_gametable_feintNonhumanoid", difficulty.base(),
				difficulty.hindrance().penalty());
			case ANIMAL_MIND -> LanguageBundle.getFormattedString("in_gametable_feintAnimalMind", difficulty.base(),
				difficulty.hindrance().penalty());
		};
	}

	private static void figure(StringBuilder html, String labelKey, String number, String detail)
	{
		html.append("<td align='center' valign='top' bgcolor='").append(FIGURE_BACKGROUND).append("'>")
			.append("<div style='font-size:11pt;color:").append(MUTED).append("'>")
			.append(LanguageBundle.getString(labelKey).toUpperCase()).append("</div>")
			.append("<div style='font-size:22pt'><b>").append(escape(number)).append("</b></div>");
		if (!detail.isEmpty())
		{
			html.append("<div style='font-size:11pt;color:").append(MUTED).append("'>").append(escape(detail))
				.append("</div>");
		}
		html.append("</td>");
	}

	private static void statBlock(StringBuilder html, MonsterSheet sheet)
	{
		CreatureStats stats = sheet.stats();
		SpecialAbilities abilities = sheet.abilities();
		html.append("<table width='100%' cellspacing='0' cellpadding='3'>");
		section(html, "in_gametable_sectionDefense");
		html.append("</table>");
		saves(html, stats.fortitude(), stats.reflex(), stats.will());
		html.append("<table width='100%' cellspacing='0' cellpadding='3'>");
		row(html, "in_gametable_combatManeuverDefense", String.valueOf(stats.combatManeuverDefense()));
		listRow(html, "in_gametable_specialDefenses", abilities.defenses());
		section(html, "in_gametable_sectionOffense");
		row(html, "in_gametable_speed", stats.speed());
		row(html, "in_gametable_combatManeuverBonus", stats.combatManeuverBonus());
		html.append("</table>");
		attacks(html, stats.attacks());
		html.append("<table width='100%' cellspacing='0' cellpadding='3'>");
		listRow(html, "in_gametable_specialAttacks", abilities.attacks());
		if (!abilities.qualities().isEmpty())
		{
			section(html, "in_gametable_specialQualities");
			for (String quality : abilities.qualities())
			{
				html.append("<tr><td colspan='2'>").append(escape(quality)).append("</td></tr>");
			}
		}
		if (!sheet.feats().isEmpty())
		{
			section(html, "in_gametable_feats");
			html.append("<tr><td colspan='2'>").append(String.join(", ", sheet.feats().stream()
				.map(SheetHtml::escape).toList())).append("</td></tr>");
		}
		html.append("</table>");
		references(html, sheet.references());
	}

	/** The three saves side by side, each under its name. */
	private static void saves(StringBuilder html, String fortitude, String reflex, String will)
	{
		html.append("<table width='100%' cellspacing='4' cellpadding='2'><tr>");
		for (String key : List.of("in_gametable_fortitude", "in_gametable_reflex", "in_gametable_will"))
		{
			html.append("<td align='center' width='33%' style='font-size:11pt;color:").append(MUTED).append("'><b>")
				.append(LanguageBundle.getString(key)).append("</b></td>");
		}
		html.append("</tr><tr>");
		for (String save : List.of(fortitude, reflex, will))
		{
			html.append("<td align='center' bgcolor='").append(FIGURE_BACKGROUND).append("' style='font-size:18pt'><b>")
				.append(escape(save)).append("</b></td>");
		}
		html.append("</tr></table>");
	}

	private static void references(StringBuilder html, List<FirstResultSearch.Link> links)
	{
		if (links.isEmpty())
		{
			return;
		}
		html.append("<p style='font-size:12pt'>");
		html.append(String.join(" &nbsp;·&nbsp; ", links.stream().map(link -> "<a href='"
			+ link.address().replace("&", "&amp;").replace("'", "&#39;") + "'>" + escape(link.label()) + "</a>")
			.toList()));
		html.append("</p>");
	}

	private static void attacks(StringBuilder html, List<CreatureAttack> attacks)
	{
		if (attacks.isEmpty())
		{
			return;
		}
		// Sized to its content: across the whole pane, a bonus sits too far from its attack to be read
		html.append("<table cellspacing='0' cellpadding='4'><tr>");
		for (String key : List.of("in_gametable_attackName", "in_gametable_attackBonus", "in_gametable_attackDamage"))
		{
			html.append("<td style='font-size:11pt;color:").append(MUTED).append("'><b>")
				.append(LanguageBundle.getString(key)).append("</b></td>");
		}
		html.append("</tr>");
		for (int index = 0; index < attacks.size(); index++)
		{
			CreatureAttack attack = attacks.get(index);
			html.append(index % 2 == 0 ? "<tr bgcolor='" + FIGURE_BACKGROUND + "'>" : "<tr>")
				.append("<td>").append(escape(attack.name())).append("</td><td><b>").append(escape(attack.toHit()))
				.append("</b></td><td>").append(escape(attack.damage())).append("</td></tr>");
		}
		html.append("</table>");
	}

	private static void section(StringBuilder html, String titleKey)
	{
		html.append("<tr><td colspan='2' bgcolor='").append(SECTION_BACKGROUND)
			.append("' style='color:#ffffff;font-size:12pt'><b>")
			.append(LanguageBundle.getString(titleKey).toUpperCase()).append("</b></td></tr>");
	}

	private static void row(StringBuilder html, String labelKey, String value)
	{
		if (value.isBlank())
		{
			return;
		}
		html.append("<tr><td valign='top' width='35%' style='color:").append(MUTED).append("'><b>")
			.append(LanguageBundle.getString(labelKey)).append("</b></td><td>").append(escape(value))
			.append("</td></tr>");
	}

	private static void listRow(StringBuilder html, String labelKey, List<String> values)
	{
		if (values.isEmpty())
		{
			return;
		}
		html.append("<tr><td valign='top' width='35%' style='color:").append(MUTED).append("'><b>")
			.append(LanguageBundle.getString(labelKey)).append("</b></td><td>");
		html.append(String.join("<br>", values.stream().map(SheetHtml::escape).toList()));
		html.append("</td></tr>");
	}

	private static void conditions(StringBuilder html, Combatant combatant, Combat combat)
	{
		List<Ongoing> conditions = new ArrayList<>();
		for (Condition condition : combatant.conditions())
		{
			List<Link> links = new ArrayList<>();
			links.add(new Link(LanguageBundle.getString("in_gametable_takeOff"), REMOVE_LINK, condition.name()));
			if (groupBearing(combatant, combat, condition.name()).size() > 1)
			{
				links.add(new Link(LanguageBundle.getString("in_gametable_takeOffGroup"), REMOVE_FROM_GROUP_LINK,
					condition.name()));
			}
			conditions.add(new Ongoing(condition.name(), condition.roundsLeft(), "", links));
		}
		ongoing(html, "in_gametable_conditions", conditions);
		ongoing(html, "in_gametable_spells", combatant.spells().stream().map(spell -> new Ongoing(spell.name(),
			spell.roundsLeft(), targetsOf(spell, combat),
			List.of(new Link(LanguageBundle.getString("in_gametable_takeOff"), DROP_SPELL_LINK, spell.name()))))
			.toList());
	}

	/**
	 * @return the combatants of its group, itself included, that bear the condition; only itself when
	 *         it has no group.
	 */
	static List<Integer> groupBearing(Combatant combatant, Combat combat, String conditionName)
	{
		if (combatant.group().isBlank())
		{
			return List.of(combatant.id());
		}
		return combat.order().stream().filter(other -> other.group().equals(combatant.group()))
			.filter(other -> other.conditions().stream().anyMatch(found -> found.name().equalsIgnoreCase(conditionName)))
			.map(Combatant::id).toList();
	}

	private static String targetsOf(CastSpell spell, Combat combat)
	{
		List<String> names = spell.targetIds().stream().map(combat::combatant).flatMap(Optional::stream)
			.map(Combatant::name).toList();
		return names.isEmpty() ? "" : LanguageBundle.getFormattedString("in_gametable_spellTargets",
			String.join(", ", names));
	}

	/** A link taking a condition or spell off: what the game master reads, where it points, what it names. */
	private record Link(String label, String prefix, String name)
	{
	}

	/** A condition or a spell as its section lists it: its name, its rounds left, what it bears on, its links. */
	private record Ongoing(String name, OptionalInt roundsLeft, String detail, List<Link> links)
	{
	}

	private static void ongoing(StringBuilder html, String titleKey, List<Ongoing> entries)
	{
		if (entries.isEmpty())
		{
			return;
		}
		html.append("<table width='100%' cellspacing='0' cellpadding='3'>");
		section(html, titleKey);
		for (Ongoing entry : entries)
		{
			html.append("<tr><td><b>").append(escape(entry.name())).append("</b>");
			entry.roundsLeft().ifPresent(rounds -> html.append(" (").append(rounds).append(")"));
			if (!entry.detail().isEmpty())
			{
				html.append("<br><span style='color:").append(MUTED).append("'>").append(escape(entry.detail()))
					.append("</span>");
			}
			html.append("</td><td align='right' valign='top'>");
			html.append(String.join(" · ", entry.links().stream().map(link -> "<a href='" + link.prefix()
				+ URLEncoder.encode(link.name(), StandardCharsets.UTF_8) + "'>" + link.label() + "</a>").toList()));
			html.append("</td></tr>");
		}
		html.append("</table>");
	}

	private static String escape(String text)
	{
		return EncountersPanel.escape(text);
	}
}
