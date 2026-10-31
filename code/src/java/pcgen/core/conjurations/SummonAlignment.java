/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package pcgen.core.conjurations;

import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Alignments of summoned creatures on the law-chaos and good-evil axes, keyed
 * by the Pathfinder alignment abbreviations (LG, NG, ..., TN, ..., CE).
 */
final class SummonAlignment
{
	private static final int LAWFUL = 0;
	private static final int GOOD = 0;
	private static final int CHAOTIC = 2;
	private static final int EVIL = 2;

	private record Axes(int lawChaos, int goodEvil)
	{
	}

	private static final Map<String, Axes> AXES = Map.of(
		"LG", new Axes(0, 0), "NG", new Axes(1, 0), "CG", new Axes(2, 0),
		"LN", new Axes(0, 1), "TN", new Axes(1, 1), "CN", new Axes(2, 1),
		"LE", new Axes(0, 2), "NE", new Axes(1, 2), "CE", new Axes(2, 2));

	/** Each summoning template sets one axis and keeps the creature's other one. */
	private static final Map<String, UnaryOperator<Axes>> TEMPLATE_AXES = Map.of(
		"Celestial Creature", axes -> new Axes(axes.lawChaos(), GOOD),
		"Fiendish Creature", axes -> new Axes(axes.lawChaos(), EVIL),
		"Resolute Creature", axes -> new Axes(LAWFUL, axes.goodEvil()),
		"Entropic Creature", axes -> new Axes(CHAOTIC, axes.goodEvil()));

	private SummonAlignment()
	{
	}

	/** Whether the creature's alignment is the deity's or one step away from it on a single axis. */
	static boolean isWithinOneStep(String deityAlignment, String creatureAlignment)
	{
		Axes deity = AXES.get(deityAlignment);
		Axes creature = AXES.get(creatureAlignment);
		if (deity == null || creature == null)
		{
			return false;
		}
		return Math.abs(deity.lawChaos() - creature.lawChaos()) + Math.abs(deity.goodEvil() - creature.goodEvil()) <= 1;
	}

	/** The creature's alignment once the template is applied; unknown alignments and templates change nothing. */
	static String withTemplate(String creatureAlignment, String template)
	{
		Axes creature = AXES.get(creatureAlignment);
		UnaryOperator<Axes> change = TEMPLATE_AXES.get(template);
		if (creature == null || change == null)
		{
			return creatureAlignment;
		}
		Axes changed = change.apply(creature);
		return AXES.entrySet().stream().filter(entry -> entry.getValue().equals(changed)).map(Map.Entry::getKey)
			.findFirst().orElse(creatureAlignment);
	}
}
