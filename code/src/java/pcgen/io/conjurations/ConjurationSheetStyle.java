/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package pcgen.io.conjurations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * How the conjuration table looks: a dark header, one tinted block per level
 * opened by a darker border, variants in italic on a paler fill, numbers
 * centered, the RPGBOT rating in its color, the first columns frozen, and a
 * landscape page fitted to its width.
 */
final class ConjurationSheetStyle
{
	private static final double HEADER_HEIGHT = 44;
	private static final int FROZEN_COLUMNS = 4;
	private static final List<Integer> WIDTHS = List.of(8, 18, 34, 8, 16, 22, 7, 17, 34, 22, 48, 7, 7, 9, 9, 9, 9, 40);
	private static final Set<String> CENTERED = Set.of("level", "maximum_count", "ranking", "hit_points",
		"armor_class", "combat_maneuver_bonus", "combat_maneuver_defense", "reflex", "fortitude", "will",
		"initiative");
	private static final Set<String> BOLD = Set.of("level", "maximum_count");
	private static final XlsxStyle HEADER = new XlsxStyle(true, false, "FFFFFFFF", "FF2F3E4E", true, 0, false);
	/** Base and variant fills of the odd levels (I, III...) and of the even ones. */
	private static final String ODD_LEVEL = "FFE8EEF5";
	private static final String ODD_LEVEL_VARIANT = "FFF3F6FA";
	private static final String EVEN_LEVEL = "FFFFFFFF";
	private static final String EVEN_LEVEL_VARIANT = "FFF6F6F6";
	private static final Map<String, String> RATING_COLORS = Map.of("red", "FFA32626", "orange", "FF9C5200",
		"green", "FF187338", "blue", "FF1D5A85");

	private ConjurationSheetStyle()
	{
	}

	static XlsxWorkbook.Sheet sheetOf(String name, ConjurationSpreadsheet.Layout layout)
	{
		List<String> columns = ConjurationSpreadsheet.COLUMNS;
		List<XlsxWorkbook.Row> rows = new ArrayList<>();
		rows.add(new XlsxWorkbook.Row(layout.rows().getFirst().stream()
			.map(title -> new XlsxWorkbook.Cell(title, HEADER)).toList(), false, HEADER_HEIGHT));
		for (int index = 1; index < layout.rows().size(); index++)
		{
			int level = layout.levels().get(index);
			boolean variant = layout.folded().contains(index);
			boolean odd = level % 2 == 1;
			XlsxStyle line = XlsxStyle.BODY.withFill(odd ? (variant ? ODD_LEVEL_VARIANT : ODD_LEVEL)
				: (variant ? EVEN_LEVEL_VARIANT : EVEN_LEVEL));
			if (index == 1 || layout.levels().get(index - 1) != level)
			{
				line = line.asLevelStart();
			}
			List<Object> contents = layout.rows().get(index);
			List<XlsxWorkbook.Cell> cells = new ArrayList<>();
			for (int column = 0; column < contents.size(); column++)
			{
				cells.add(new XlsxWorkbook.Cell(contents.get(column),
					styleOf(line, columns.get(column), variant, layout.ratings().get(index))));
			}
			rows.add(new XlsxWorkbook.Row(cells, variant, 0));
		}
		return new XlsxWorkbook.Sheet(name, rows, WIDTHS, FROZEN_COLUMNS, true);
	}

	private static XlsxStyle styleOf(XlsxStyle line, String column, boolean variant, String rating)
	{
		XlsxStyle style = line;
		if (CENTERED.contains(column))
		{
			style = style.asCentered();
		}
		if (BOLD.contains(column))
		{
			style = style.asBold();
		}
		if ("creature".equals(column) && variant)
		{
			style = style.asSubordinate();
		}
		if ("ranking".equals(column) && RATING_COLORS.containsKey(rating))
		{
			style = style.asBold().withFontColor(RATING_COLORS.get(rating));
		}
		return style;
	}
}
