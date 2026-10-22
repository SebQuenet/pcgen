/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package pcgen.io.conjurations;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A one-sheet Office Open XML workbook. Its first row is a header with a filter,
 * frozen and repeated on each printed page. A whole number is written as a
 * number, anything else as text.
 */
public final class XlsxWorkbook
{
	private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";
	private static final String MAIN_NAMESPACE = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
	private static final String RELATIONSHIP_NAMESPACE =
		"http://schemas.openxmlformats.org/officeDocument/2006/relationships";
	private static final String PACKAGE_RELATIONSHIPS = "http://schemas.openxmlformats.org/package/2006/relationships";
	private static final String BORDER_COLOR = "FFD0D7DE";
	private static final String LEVEL_BORDER_COLOR = "FF6B7785";
	private static final int NARROWEST_COLUMN = 6;
	private static final int WIDEST_COLUMN = 60;
	private static final int ALPHABET = 26;

	private XlsxWorkbook()
	{
	}

	/**
	 * One cell.
	 *
	 * @param content an {@link Integer}, written as a number, or anything else, written as text
	 * @param style   how it looks
	 */
	public record Cell(Object content, XlsxStyle style)
	{
	}

	/**
	 * One row.
	 *
	 * @param cells  its cells, from the first column
	 * @param folded whether it is folded under the nearest unfolded row above it
	 * @param height its height in points, or 0 to let the spreadsheet fit its content
	 */
	public record Row(List<Cell> cells, boolean folded, double height)
	{
	}

	/**
	 * The one sheet.
	 *
	 * @param name           its name
	 * @param rows           the header row, then the lines
	 * @param columnWidths   each column's width in characters, or empty to fit the content
	 * @param frozenColumns  how many columns on the left stay in view when scrolling right
	 * @param landscape      whether it prints in landscape, fitted to the page's width
	 */
	public record Sheet(String name, List<Row> rows, List<Integer> columnWidths, int frozenColumns, boolean landscape)
	{
	}

	/**
	 * @param sheetName the sheet's name
	 * @param rows      the header row, then one row per line; each cell an {@link Integer} or text
	 * @return the workbook's bytes, ready to be written to a {@code .xlsx} file.
	 */
	public static byte[] of(String sheetName, List<? extends List<?>> rows)
	{
		return of(sheetName, rows, Set.of());
	}

	/**
	 * A plain sheet: a bold header on a grey fill, wrapped text below it.
	 *
	 * @param sheetName  the sheet's name
	 * @param rows       the header row, then one row per line
	 * @param foldedRows the indexes in {@code rows} of the lines folded under the
	 *                   nearest unfolded line above them, hidden until it is unfolded
	 * @return the workbook's bytes.
	 */
	public static byte[] of(String sheetName, List<? extends List<?>> rows, Set<Integer> foldedRows)
	{
		List<Row> styled = IntStream.range(0, rows.size()).mapToObj(index -> new Row(rows.get(index).stream()
			.map(content -> new Cell(content, index == 0 ? XlsxStyle.HEADER : XlsxStyle.BODY)).toList(),
			foldedRows.contains(index), 0)).toList();
		return of(new Sheet(sheetName, styled, List.of(), 0, false));
	}

	/**
	 * @param sheet the sheet, its header first
	 * @return the workbook's bytes.
	 */
	public static byte[] of(Sheet sheet)
	{
		Map<XlsxStyle, Integer> styles = new LinkedHashMap<>();
		styles.put(XlsxStyle.BODY, 0);
		String worksheet = worksheet(sheet, styles);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8))
		{
			part(zip, "[Content_Types].xml", contentTypes());
			part(zip, "_rels/.rels", XML_HEADER + "<Relationships xmlns=\"" + PACKAGE_RELATIONSHIPS + "\">"
				+ "<Relationship Id=\"rId1\" Type=\"" + RELATIONSHIP_NAMESPACE + "/officeDocument\" Target=\"xl/workbook.xml\"/>"
				+ "</Relationships>");
			part(zip, "xl/workbook.xml", workbook(sheet.name()));
			part(zip, "xl/_rels/workbook.xml.rels", XML_HEADER + "<Relationships xmlns=\"" + PACKAGE_RELATIONSHIPS + "\">"
				+ "<Relationship Id=\"rId1\" Type=\"" + RELATIONSHIP_NAMESPACE + "/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
				+ "<Relationship Id=\"rId2\" Type=\"" + RELATIONSHIP_NAMESPACE + "/styles\" Target=\"styles.xml\"/>"
				+ "</Relationships>");
			part(zip, "xl/styles.xml", stylesheet(List.copyOf(styles.keySet())));
			part(zip, "xl/worksheets/sheet1.xml", worksheet);
		}
		catch (IOException error)
		{
			throw new UncheckedIOException("Could not write the workbook in memory", error);
		}
		return bytes.toByteArray();
	}

	private static void part(ZipOutputStream zip, String name, String content) throws IOException
	{
		zip.putNextEntry(new ZipEntry(name));
		zip.write(content.getBytes(StandardCharsets.UTF_8));
		zip.closeEntry();
	}

	private static String contentTypes()
	{
		return XML_HEADER + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
			+ "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
			+ "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
			+ "<Override PartName=\"/xl/workbook.xml\" "
			+ "ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
			+ "<Override PartName=\"/xl/worksheets/sheet1.xml\" "
			+ "ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
			+ "<Override PartName=\"/xl/styles.xml\" "
			+ "ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
			+ "</Types>";
	}

	/** The header row is repeated at the top of each printed page. */
	private static String workbook(String sheetName)
	{
		String quoted = "'" + escaped(sheetName).replace("'", "''") + "'";
		return XML_HEADER + "<workbook xmlns=\"" + MAIN_NAMESPACE + "\" xmlns:r=\"" + RELATIONSHIP_NAMESPACE
			+ "\"><sheets><sheet name=\"" + escaped(sheetName) + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
			+ "<definedNames><definedName name=\"_xlnm.Print_Titles\" localSheetId=\"0\">" + quoted
			+ "!$1:$1</definedName></definedNames></workbook>";
	}

	/** Every cell has a thin grey border; a row starting a new block has a darker one above. */
	private static String stylesheet(List<XlsxStyle> styles)
	{
		List<String> fonts = new ArrayList<>();
		List<String> fills = new ArrayList<>(List.of("<fill><patternFill patternType=\"none\"/></fill>",
			"<fill><patternFill patternType=\"gray125\"/></fill>"));
		StringBuilder formats = new StringBuilder();
		for (XlsxStyle style : styles)
		{
			String font = "<font>" + (style.bold() ? "<b/>" : "") + (style.italic() ? "<i/>" : "")
				+ "<sz val=\"10\"/><color rgb=\"" + style.fontColor() + "\"/><name val=\"Calibri\"/></font>";
			if (!fonts.contains(font))
			{
				fonts.add(font);
			}
			int fill = 0;
			if (!style.fill().isEmpty())
			{
				String solid = "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"" + style.fill()
					+ "\"/><bgColor indexed=\"64\"/></patternFill></fill>";
				if (!fills.contains(solid))
				{
					fills.add(solid);
				}
				fill = fills.indexOf(solid);
			}
			formats.append("<xf numFmtId=\"0\" fontId=\"").append(fonts.indexOf(font)).append("\" fillId=\"")
				.append(fill).append("\" borderId=\"").append(style.levelStart() ? 1 : 0)
				.append("\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\">")
				.append("<alignment horizontal=\"").append(style.centered() ? "center" : "left")
				.append("\" vertical=\"top\" wrapText=\"1\"").append(style.indent() > 0 ? " indent=\"" + style.indent() + "\"" : "")
				.append("/></xf>");
		}
		String thin = "<left style=\"thin\"><color rgb=\"" + BORDER_COLOR + "\"/></left><right style=\"thin\"><color rgb=\""
			+ BORDER_COLOR + "\"/></right>";
		String bottom = "<bottom style=\"thin\"><color rgb=\"" + BORDER_COLOR + "\"/></bottom><diagonal/>";
		return XML_HEADER + "<styleSheet xmlns=\"" + MAIN_NAMESPACE + "\">"
			+ "<fonts count=\"" + fonts.size() + "\">" + String.join("", fonts) + "</fonts>"
			+ "<fills count=\"" + fills.size() + "\">" + String.join("", fills) + "</fills>"
			+ "<borders count=\"2\"><border>" + thin + "<top style=\"thin\"><color rgb=\"" + BORDER_COLOR + "\"/></top>" + bottom
			+ "</border><border>" + thin + "<top style=\"medium\"><color rgb=\"" + LEVEL_BORDER_COLOR + "\"/></top>" + bottom
			+ "</border></borders>"
			+ "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
			+ "<cellXfs count=\"" + styles.size() + "\">" + formats + "</cellXfs>"
			+ "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
			+ "</styleSheet>";
	}

	private static String worksheet(Sheet sheet, Map<XlsxStyle, Integer> styles)
	{
		List<Row> rows = sheet.rows();
		int columns = rows.stream().mapToInt(row -> row.cells().size()).max().orElse(0);
		boolean folds = rows.stream().anyMatch(Row::folded);
		String topLeft = columnName(sheet.frozenColumns()) + "2";
		// The fold button sits on the line above the folded ones
		StringBuilder xml = new StringBuilder(XML_HEADER).append("<worksheet xmlns=\"").append(MAIN_NAMESPACE)
			.append("\"><sheetPr><outlinePr summaryBelow=\"0\"/>").append(sheet.landscape() ? "<pageSetUpPr fitToPage=\"1\"/>" : "")
			.append("</sheetPr><sheetViews><sheetView workbookViewId=\"0\"><pane ")
			.append(sheet.frozenColumns() > 0 ? "xSplit=\"" + sheet.frozenColumns() + "\" " : "")
			.append("ySplit=\"1\" topLeftCell=\"").append(topLeft).append("\" activePane=\"")
			.append(sheet.frozenColumns() > 0 ? "bottomRight" : "bottomLeft").append("\" state=\"frozen\"/></sheetView>")
			.append("</sheetViews><sheetFormatPr defaultRowHeight=\"15\" outlineLevelRow=\"").append(folds ? 1 : 0).append("\"/>");
		if (columns > 0)
		{
			xml.append("<cols>");
			for (int column = 0; column < columns; column++)
			{
				int width = column < sheet.columnWidths().size() ? sheet.columnWidths().get(column) : widthOf(rows, column);
				xml.append("<col min=\"").append(column + 1).append("\" max=\"").append(column + 1)
					.append("\" width=\"").append(width).append("\" customWidth=\"1\"/>");
			}
			xml.append("</cols>");
		}
		xml.append("<sheetData>");
		for (int index = 0; index < rows.size(); index++)
		{
			Row row = rows.get(index);
			xml.append("<row r=\"").append(index + 1).append('"');
			if (row.folded())
			{
				xml.append(" outlineLevel=\"1\" hidden=\"1\"");
			}
			else if (index + 1 < rows.size() && rows.get(index + 1).folded())
			{
				xml.append(" collapsed=\"1\"");
			}
			if (row.height() > 0)
			{
				xml.append(" ht=\"").append(row.height()).append("\" customHeight=\"1\"");
			}
			xml.append('>');
			for (int column = 0; column < row.cells().size(); column++)
			{
				Cell cell = row.cells().get(column);
				int style = styles.computeIfAbsent(cell.style(), ignored -> styles.size());
				xml.append(cell(columnName(column) + (index + 1), style, cell.content()));
			}
			xml.append("</row>");
		}
		xml.append("</sheetData>");
		if (columns > 0 && !rows.isEmpty())
		{
			xml.append("<autoFilter ref=\"A1:").append(columnName(columns - 1)).append(rows.size()).append("\"/>");
		}
		xml.append("<pageMargins left=\"0.4\" right=\"0.4\" top=\"0.5\" bottom=\"0.5\" header=\"0.3\" footer=\"0.3\"/>");
		if (sheet.landscape())
		{
			xml.append("<pageSetup orientation=\"landscape\" fitToWidth=\"1\" fitToHeight=\"0\"/>");
		}
		return xml.append("</worksheet>").toString();
	}

	private static String cell(String reference, int style, Object content)
	{
		if (content instanceof Integer number)
		{
			return "<c r=\"" + reference + "\" s=\"" + style + "\"><v>" + number + "</v></c>";
		}
		String text = content == null ? "" : content.toString();
		return "<c r=\"" + reference + "\" s=\"" + style + "\" t=\"inlineStr\"><is><t xml:space=\"preserve\">"
			+ escaped(text) + "</t></is></c>";
	}

	/** Wide enough for the longest line of the column, within bounds. */
	private static int widthOf(List<Row> rows, int column)
	{
		int longest = rows.stream().filter(row -> column < row.cells().size()).map(row -> row.cells().get(column).content())
			.map(content -> content == null ? "" : content.toString())
			.flatMap(String::lines).mapToInt(String::length).max().orElse(0);
		return Math.max(NARROWEST_COLUMN, Math.min(WIDEST_COLUMN, longest + 2));
	}

	private static String columnName(int column)
	{
		StringBuilder name = new StringBuilder();
		for (int rest = column + 1; rest > 0; rest = (rest - 1) / ALPHABET)
		{
			name.insert(0, (char) ('A' + (rest - 1) % ALPHABET));
		}
		return name.toString();
	}

	/** Escapes XML markup and drops the control characters XML cannot hold. */
	private static String escaped(String text)
	{
		StringBuilder escaped = new StringBuilder(text.length());
		text.codePoints().forEach(codePoint -> {
			switch (codePoint)
			{
				case '&' -> escaped.append("&amp;");
				case '<' -> escaped.append("&lt;");
				case '>' -> escaped.append("&gt;");
				case '"' -> escaped.append("&quot;");
				default -> {
					if (codePoint >= ' ' || codePoint == '\n' || codePoint == '\t')
					{
						escaped.appendCodePoint(codePoint);
					}
				}
			}
		});
		return escaped.toString();
	}
}
