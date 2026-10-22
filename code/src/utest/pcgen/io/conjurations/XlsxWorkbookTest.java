/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package pcgen.io.conjurations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;

/**
 * A one-sheet workbook written as the parts of an Office Open XML package.
 */
class XlsxWorkbookTest
{
	private static Map<String, String> partsOf(byte[] workbook) throws IOException
	{
		Map<String, String> parts = new HashMap<>();
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(workbook)))
		{
			for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry())
			{
				parts.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
			}
		}
		return parts;
	}

	@Test
	void holdsEveryPartASpreadsheetApplicationNeeds() throws IOException
	{
		byte[] workbook = XlsxWorkbook.of("Summons", List.of(List.of("Name")));

		assertEquals(Set.of("[Content_Types].xml", "_rels/.rels", "xl/workbook.xml", "xl/_rels/workbook.xml.rels",
			"xl/styles.xml", "xl/worksheets/sheet1.xml"), partsOf(workbook).keySet());
	}

	@Test
	void writesTextEscapedAndKeepsItsLineBreaks() throws IOException
	{
		byte[] workbook = XlsxWorkbook.of("Summons", List.of(List.of("Name"), List.of("Bite <1d6> & claw\nrake")));

		assertTrue(partsOf(workbook).get("xl/worksheets/sheet1.xml")
			.contains("<t xml:space=\"preserve\">Bite &lt;1d6&gt; &amp; claw\nrake</t>"));
	}

	@Test
	void writesAWholeNumberAsANumber() throws IOException
	{
		byte[] workbook = XlsxWorkbook.of("Summons", List.of(List.of("Max"), List.of(7)));

		assertTrue(partsOf(workbook).get("xl/worksheets/sheet1.xml").contains("<c r=\"A2\" s=\"0\"><v>7</v></c>"));
	}

	@Test
	void setsTheFirstRowAsABoldHeaderWithAFilter() throws IOException
	{
		byte[] workbook = XlsxWorkbook.of("Summons", List.of(List.of("Level", "Name"), List.of("I", "Dog")));

		String sheet = partsOf(workbook).get("xl/worksheets/sheet1.xml");
		assertTrue(sheet.contains("<c r=\"B1\" s=\"1\" t=\"inlineStr\">") && sheet.contains("<autoFilter ref=\"A1:B2\"/>"));
	}

	@Test
	void foldsARowUnderTheRowAboveIt() throws IOException
	{
		byte[] workbook = XlsxWorkbook.of("Summons", List.of(List.of("Name"), List.of("Bear"), List.of("Bear · Savage")),
			Set.of(2));

		String sheet = partsOf(workbook).get("xl/worksheets/sheet1.xml");
		assertTrue(sheet.contains("<row r=\"2\" collapsed=\"1\">") && sheet.contains("<row r=\"3\" outlineLevel=\"1\" hidden=\"1\">"));
	}

	@Test
	void putsTheFoldButtonOnTheRowAboveTheFoldedOnes() throws IOException
	{
		byte[] workbook = XlsxWorkbook.of("Summons", List.of(List.of("Name"), List.of("Bear"), List.of("Bear · Savage")),
			Set.of(2));

		assertTrue(partsOf(workbook).get("xl/worksheets/sheet1.xml").contains("<outlinePr summaryBelow=\"0\"/>"));
	}

	private static XlsxWorkbook.Sheet styledSheet()
	{
		XlsxStyle variant = XlsxStyle.BODY.asSubordinate().withFill("FFF2F6FA");
		return new XlsxWorkbook.Sheet("Summons", List.of(
			new XlsxWorkbook.Row(List.of(new XlsxWorkbook.Cell("Level", XlsxStyle.HEADER),
				new XlsxWorkbook.Cell("Name", XlsxStyle.HEADER)), false, 30),
			new XlsxWorkbook.Row(List.of(new XlsxWorkbook.Cell("VI", XlsxStyle.BODY.asLevelStart()),
				new XlsxWorkbook.Cell("Bear", XlsxStyle.BODY.asLevelStart())), false, 0),
			new XlsxWorkbook.Row(List.of(new XlsxWorkbook.Cell("VI", variant),
				new XlsxWorkbook.Cell("Bear · Savage", variant)), true, 0)), List.of(6, 30), 2, true);
	}

	@Test
	void keepsTheFirstColumnsInViewWhenScrollingRight() throws IOException
	{
		String sheet = partsOf(XlsxWorkbook.of(styledSheet())).get("xl/worksheets/sheet1.xml");

		assertTrue(sheet.contains("<pane xSplit=\"2\" ySplit=\"1\" topLeftCell=\"C2\" activePane=\"bottomRight\" state=\"frozen\"/>"));
	}

	@Test
	void repeatsTheHeaderOnEachPrintedPage() throws IOException
	{
		String workbook = partsOf(XlsxWorkbook.of(styledSheet())).get("xl/workbook.xml");

		assertTrue(workbook.contains("<definedName name=\"_xlnm.Print_Titles\" localSheetId=\"0\">'Summons'!$1:$1</definedName>"));
	}

	@Test
	void printsInLandscapeFittedToThePagesWidth() throws IOException
	{
		String sheet = partsOf(XlsxWorkbook.of(styledSheet())).get("xl/worksheets/sheet1.xml");

		assertTrue(sheet.contains("<pageSetup orientation=\"landscape\" fitToWidth=\"1\" fitToHeight=\"0\"/>"));
	}

	@Test
	void writesAVariantInItalicMovedRight() throws IOException
	{
		String styles = partsOf(XlsxWorkbook.of(styledSheet())).get("xl/styles.xml");

		assertTrue(styles.contains("<font><i/><sz val=\"10\"/>") && styles.contains("indent=\"1\""));
	}

	@Test
	void setsTheChosenColumnWidths() throws IOException
	{
		String sheet = partsOf(XlsxWorkbook.of(styledSheet())).get("xl/worksheets/sheet1.xml");

		assertTrue(sheet.contains("<col min=\"2\" max=\"2\" width=\"30\" customWidth=\"1\"/>"));
	}
}
