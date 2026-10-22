/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package pcgen.io.conjurations;

/**
 * How one cell of a workbook looks. Colors are ARGB hexadecimal, such as
 * {@code FF1D5A85}; an empty fill leaves the cell unfilled.
 *
 * @param bold       whether the text is bold
 * @param italic     whether the text is italic
 * @param fontColor  the text's color
 * @param fill       the background's color, or an empty string
 * @param centered   whether the content is centered rather than set to the left
 * @param indent     how many indentation steps the content is moved right
 * @param levelStart whether a thicker border above starts a new block of rows
 */
public record XlsxStyle(boolean bold, boolean italic, String fontColor, String fill, boolean centered, int indent,
	boolean levelStart)
{
	private static final String BLACK = "FF1A1F1B";

	/** Plain text, top-aligned and wrapped, on no fill. */
	public static final XlsxStyle BODY = new XlsxStyle(false, false, BLACK, "", false, 0, false);

	/** Bold text on a grey fill. */
	public static final XlsxStyle HEADER = new XlsxStyle(true, false, BLACK, "FFDDDDDD", false, 0, false);

	public XlsxStyle asBold()
	{
		return new XlsxStyle(true, italic, fontColor, fill, centered, indent, levelStart);
	}

	public XlsxStyle asCentered()
	{
		return new XlsxStyle(bold, italic, fontColor, fill, true, indent, levelStart);
	}

	public XlsxStyle withFill(String color)
	{
		return new XlsxStyle(bold, italic, fontColor, color, centered, indent, levelStart);
	}

	public XlsxStyle withFontColor(String color)
	{
		return new XlsxStyle(bold, italic, color, fill, centered, indent, levelStart);
	}

	public XlsxStyle asLevelStart()
	{
		return new XlsxStyle(bold, italic, fontColor, fill, centered, indent, true);
	}

	/** Italic and moved one step right, as a line that belongs to the one above it. */
	public XlsxStyle asSubordinate()
	{
		return new XlsxStyle(bold, true, fontColor, fill, centered, indent + 1, levelStart);
	}
}
