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
import java.util.List;
import java.util.Optional;

import pcgen.core.conjurations.CreatureAttack;
import pcgen.core.conjurations.CreatureStats;
import pcgen.core.gametable.Combatant;
import pcgen.core.gametable.Condition;
import pcgen.core.gametable.Health;
import pcgen.core.gametable.PlayerGlance;
import pcgen.system.LanguageBundle;

/**
 * Writes the sheet the game master reads beside the fight, as simple HTML for a Swing
 * text pane.
 */
final class SheetHtml
{
	/** Where a link to take a condition off points; the condition's name follows. */
	static final String REMOVE_LINK = "remove:";

	private SheetHtml()
	{
	}

	/**
	 * @param stats           a monster's numbers.
	 * @param challengeRating its challenge rating, when known.
	 * @return its stat block, without any fight state.
	 */
	static String preview(CreatureStats stats, Optional<String> challengeRating)
	{
		StringBuilder html = new StringBuilder("<html><body style='font-family:sans-serif;font-size:11pt'>");
		challengeRating.ifPresent(rating -> line(html, "in_gametable_challengeRating", rating));
		line(html, "in_gametable_hitPoints", LanguageBundle.getFormattedString("in_gametable_hitPointsAndDice", stats.maximumHitPoints(), stats.hitDice()));
		monsterNumbers(html, stats);
		return html.append("</body></html>").toString();
	}

	/**
	 * @param combatant a monster in the fight.
	 * @param stats     its numbers, when it could be built.
	 * @return its sheet, with its hit points left and its conditions.
	 */
	static String monster(Combatant combatant, Optional<CreatureStats> stats)
	{
		StringBuilder html = head(combatant);
		Health health = combatant.health().orElseThrow();
		html.append("<p style='font-size:14pt'><b>").append(LanguageBundle.getString("in_gametable_hitPoints"))
			.append(" ").append(health.remaining()).append(" / ").append(health.maximum()).append("</b></p>");
		stats.ifPresentOrElse(found -> monsterNumbers(html, found),
			() -> html.append("<p><i>").append(LanguageBundle.getString("in_gametable_notBuilt")).append("</i></p>"));
		conditions(html, combatant.conditions());
		return html.append("</body></html>").toString();
	}

	/**
	 * @param combatant a player's character or a creature it conjured.
	 * @param glance    the few numbers read from its file, when known.
	 * @return its sheet: those numbers and its conditions.
	 */
	static String player(Combatant combatant, Optional<PlayerGlance> glance)
	{
		StringBuilder html = head(combatant);
		glance.ifPresent(found -> {
			line(html, "in_gametable_perception", found.perception());
			line(html, "in_gametable_senseMotive", found.senseMotive());
			html.append("<p>").append(LanguageBundle.getFormattedString("in_gametable_saves", found.fortitude(),
				found.reflex(), found.will())).append("</p>");
		});
		combatant.roundsLeft().ifPresent(rounds -> line(html, "in_gametable_roundsLeft", String.valueOf(rounds)));
		conditions(html, combatant.conditions());
		return html.append("</body></html>").toString();
	}

	private static StringBuilder head(Combatant combatant)
	{
		StringBuilder html = new StringBuilder("<html><body style='font-family:sans-serif;font-size:11pt'>");
		html.append("<h2>").append(EncountersPanel.escape(combatant.name())).append("</h2>");
		if (combatant.recipe().isPresent())
		{
			String touches = EncountersPanel.touchesOf(combatant.recipe().get());
			html.append("<p><i>").append(EncountersPanel.escape(RetouchDialog.sourceText(
				combatant.recipe().get().source()))).append(touches.isEmpty() ? "" : " — " + EncountersPanel.escape(
					touches)).append("</i></p>");
		}
		return html;
	}

	private static void monsterNumbers(StringBuilder html, CreatureStats stats)
	{
		html.append("<p>").append(LanguageBundle.getFormattedString("in_gametable_armorClasses", stats.armorClass(),
			stats.touchArmorClass(), stats.flatFootedArmorClass())).append("</p>");
		html.append("<p>").append(LanguageBundle.getFormattedString("in_gametable_saves", stats.fortitude(),
			stats.reflex(), stats.will())).append("</p>");
		html.append("<p>").append(LanguageBundle.getFormattedString("in_gametable_maneuvers",
			stats.combatManeuverBonus(), stats.combatManeuverDefense(), EncountersPanel.escape(stats.speed())))
			.append("</p>");
		html.append("<p><b>").append(LanguageBundle.getString("in_gametable_attacks")).append("</b><br>");
		for (CreatureAttack attack : stats.attacks())
		{
			html.append(EncountersPanel.escape(attack.name())).append(" ").append(attack.toHit()).append(" (")
				.append(EncountersPanel.escape(attack.damage())).append(")<br>");
		}
		html.append("</p>");
		if (!stats.specialAbilities().isBlank())
		{
			html.append("<p><b>").append(LanguageBundle.getString("in_gametable_specialAbilities")).append("</b><br>")
				.append(EncountersPanel.escape(stats.specialAbilities())).append("</p>");
		}
	}

	private static void conditions(StringBuilder html, List<Condition> conditions)
	{
		if (conditions.isEmpty())
		{
			return;
		}
		html.append("<p><b>").append(LanguageBundle.getString("in_gametable_conditions")).append("</b><br>");
		for (Condition condition : conditions)
		{
			html.append(EncountersPanel.escape(condition.name()));
			condition.roundsLeft().ifPresent(rounds -> html.append(" (").append(rounds).append(")"));
			html.append(" <a href='").append(REMOVE_LINK)
				.append(URLEncoder.encode(condition.name(), StandardCharsets.UTF_8)).append("'>")
				.append(LanguageBundle.getString("in_gametable_takeOff")).append("</a><br>");
		}
		html.append("</p>");
	}

	private static void line(StringBuilder html, String key, String value)
	{
		html.append("<p><b>").append(LanguageBundle.getString(key)).append("</b> ")
			.append(EncountersPanel.escape(value)).append("</p>");
	}
}
