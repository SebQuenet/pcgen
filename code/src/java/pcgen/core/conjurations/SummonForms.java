/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package pcgen.core.conjurations;

import java.util.List;
import java.util.Optional;

import pcgen.system.LanguageBundle;

/** Rules for forms a summoned creature can take after it arrives. */
public final class SummonForms
{
	private static final String HOUND_RULE = "https://aonprd.com/MonsterDisplay.aspx?ItemName=Hound+Archon";
	private static final String LANTERN_RULE = "https://aonprd.com/MonsterDisplay.aspx?ItemName=Lantern+Archon";
	private static final String BRALANI_RULE = "https://aonprd.com/MonsterDisplay.aspx?ItemName=Bralani";

	public record Form(String key, String label, String description, String source)
	{
	}

	private SummonForms()
	{
	}

	public static List<Form> forSummon(SummonTemplate summon)
	{
		return switch (summon.sourceRaceKey())
		{
			case "Archon (Hound)" -> List.of(
				new Form("dog", label("dog_label"), label("dog_description"), HOUND_RULE),
				new Form("wolf", label("wolf_label"), label("wolf_description"), HOUND_RULE),
				new Form("dire-wolf", label("dire_wolf_label"), label("dire_wolf_description"), HOUND_RULE));
			case "Azata (Bralani)" -> List.of(new Form("wind", label("wind_label"),
				label("wind_description"), BRALANI_RULE));
			default -> List.of();
		};
	}

	/** Gestalt consumes nine lantern archons; it cannot be selected on one individual card. */
	public static Optional<Form> collectiveForm(SummonTemplate summon)
	{
		return "Archon (Lantern)".equals(summon.sourceRaceKey())
			? Optional.of(new Form("gestalt", label("gestalt_label"), label("gestalt_description"),
				LANTERN_RULE)) : Optional.empty();
	}

	private static String label(String suffix)
	{
		return LanguageBundle.getString("in_conjurations_form_" + suffix);
	}

	public static boolean supports(SummonTemplate summon, String form)
	{
		return form == null || form.isBlank() || forSummon(summon).stream().anyMatch(found -> found.key().equals(form));
	}

	/** Applies the hound archon's beast shape II changes to its computed PCGen statistics. */
	public static CreatureStats statsOf(SummonTemplate summon, String form, CreatureStats base)
	{
		if ("Azata (Bralani)".equals(summon.sourceRaceKey()))
		{
			String speed = "wind".equals(form) ? "fly 100 ft. (perfect)" : "40 ft.";
			List<CreatureAttack> attacks = "wind".equals(form) ? base.attacks().stream()
				.filter(attack -> attack.name().toLowerCase().contains("slam")).toList() : base.attacks();
			return new CreatureStats(base.hitDice(), speed, base.maximumHitPoints(), base.armorClass(),
				base.touchArmorClass(), base.flatFootedArmorClass(), attacks, base.combatManeuverBonus(),
				base.combatManeuverDefense(), base.reflex(), base.fortitude(), base.will(), base.initiative(),
				base.specialAbilities());
		}
		if (form == null || form.isBlank() || !"Archon (Hound)".equals(summon.sourceRaceKey()))
		{
			return base;
		}
		int strengthDelta;
		int dexterityDelta;
		int sizeDelta;
		int naturalArmor;
		String speed;
		String biteDice;
		switch (form)
		{
			case "dog" -> {
				strengthDelta = 0;
				dexterityDelta = 1;
				sizeDelta = 1;
				naturalArmor = 1;
				speed = "40 ft.";
				biteDice = "1d4";
			}
			case "wolf" -> {
				strengthDelta = 1;
				dexterityDelta = 0;
				sizeDelta = 0;
				naturalArmor = 2;
				speed = "50 ft.";
				biteDice = "1d6";
			}
			case "dire-wolf" -> {
				strengthDelta = 2;
				dexterityDelta = -1;
				sizeDelta = -1;
				naturalArmor = 4;
				speed = "50 ft.";
				biteDice = "1d8";
			}
			default -> {
				return base;
			}
		}
		int armorDelta = -9 + naturalArmor + dexterityDelta + sizeDelta;
		int maneuverSizeDelta = -sizeDelta;
		Optional<CreatureAttack> originalBite = base.attacks().stream()
			.filter(attack -> attack.name().toLowerCase().contains("bite")).findFirst();
		int biteBonus = originalBite.map(attack -> firstSigned(attack.toHit())).orElse(6);
		int originalDamage = originalBite.map(attack -> damageModifier(attack.damage())).orElse(3);
		int biteDamage = originalDamage + Math.floorDiv(3 * strengthDelta, 2);
		CreatureAttack bite = new CreatureAttack("Bite", signed(biteBonus + strengthDelta + sizeDelta),
			biteDice + signed(biteDamage));
		return new CreatureStats(base.hitDice(), speed, base.maximumHitPoints(), base.armorClass() + armorDelta,
			base.touchArmorClass() + dexterityDelta + sizeDelta,
			base.flatFootedArmorClass() - 9 + naturalArmor + sizeDelta, List.of(bite),
			signed(firstSigned(base.combatManeuverBonus()) + strengthDelta + maneuverSizeDelta),
			base.combatManeuverDefense() + strengthDelta + dexterityDelta + maneuverSizeDelta,
			base.reflex().isBlank() ? "" : signed(firstSigned(base.reflex()) + dexterityDelta),
			base.fortitude(), base.will(), base.initiative().isBlank() ? ""
				: signed(firstSigned(base.initiative()) + dexterityDelta), base.specialAbilities());
	}

	private static int firstSigned(String value)
	{
		java.util.regex.Matcher number = java.util.regex.Pattern.compile("[+-]?\\d+").matcher(value);
		return number.find() ? Integer.parseInt(number.group()) : 0;
	}

	private static int damageModifier(String value)
	{
		java.util.regex.Matcher modifier = java.util.regex.Pattern.compile("[+-]\\d+$").matcher(value.strip());
		return modifier.find() ? Integer.parseInt(modifier.group()) : 0;
	}

	private static String signed(int number)
	{
		return number < 0 ? Integer.toString(number) : "+" + number;
	}
}
