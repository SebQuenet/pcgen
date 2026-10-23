/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package pcgen.gui2.tabs.conjurations;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import pcgen.facade.core.ConjurationSheetFacade;
import pcgen.io.conjurations.PathfinderSummonPortraits;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConjurationBridgeTest
{
	@Test
	void catalogCalculationDoesNotBlockTheWebView() throws Exception
	{
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		ConjurationSheetFacade sheet = (ConjurationSheetFacade) Proxy.newProxyInstance(
			getClass().getClassLoader(), new Class<?>[] {ConjurationSheetFacade.class}, (proxy, method, args) -> {
				if ("getCatalogSource".equals(method.getName()))
				{
					return "";
				}
				if ("catalogStatsOf".equals(method.getName()))
				{
					started.countDown();
					assertTrue(release.await(5, TimeUnit.SECONDS));
					return Optional.empty();
				}
				throw new UnsupportedOperationException(method.getName());
			});
		try (var caller = Executors.newSingleThreadExecutor())
		{
			try
			{
				try (ConjurationBridge bridge = new ConjurationBridge(sheet))
				{
					var first = caller.submit(() -> bridge.catalogStats("Wolf", 20));
					assertTrue(started.await(3, TimeUnit.SECONDS));
					assertEquals("{\"pending\":true}", first.get(300, TimeUnit.MILLISECONDS));
					release.countDown();
					String result = "{\"pending\":true}";
					for (int attempt = 0; attempt < 100 && "{\"pending\":true}".equals(result); attempt++)
					{
						Thread.sleep(10);
						result = bridge.catalogStats("Wolf", 20);
					}
					assertEquals("null", result);
				}
			}
			finally
			{
				release.countDown();
			}
		}
	}

	@Test
	void openingAReferenceDoesNotBlockTheWebView() throws Exception
	{
		CountDownLatch browsing = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		ConjurationSheetFacade sheet = (ConjurationSheetFacade) Proxy.newProxyInstance(
			getClass().getClassLoader(), new Class<?>[] {ConjurationSheetFacade.class}, (proxy, method, args) -> {
				throw new UnsupportedOperationException(method.getName());
			});
		ConjurationBridge.ReferenceBrowser stuckBrowser = reference -> {
			browsing.countDown();
			try
			{
				release.await(5, TimeUnit.SECONDS);
			}
			catch (InterruptedException interrupted)
			{
				Thread.currentThread().interrupt();
			}
		};
		try (var caller = Executors.newSingleThreadExecutor();
			ConjurationBridge bridge = new ConjurationBridge(sheet, ignored -> false, () -> true, stuckBrowser))
		{
			try
			{
				var opening = caller.submit(() -> bridge.openReference("https://www.d20pfsrd.com/bestiary/"));
				assertTrue(browsing.await(3, TimeUnit.SECONDS));
				opening.get(300, TimeUnit.MILLISECONDS);
			}
			finally
			{
				release.countDown();
			}
		}
	}

	@Test
	void hiddenCatalogDoesNotStartBuildingCreatures() throws Exception
	{
		AtomicBoolean visible = new AtomicBoolean(false);
		AtomicInteger builds = new AtomicInteger();
		ConjurationSheetFacade sheet = (ConjurationSheetFacade) Proxy.newProxyInstance(
			getClass().getClassLoader(), new Class<?>[] {ConjurationSheetFacade.class}, (proxy, method, args) -> {
				if ("getCatalogSource".equals(method.getName()))
				{
					return "";
				}
				if ("catalogStatsOf".equals(method.getName()))
				{
					builds.incrementAndGet();
					return Optional.empty();
				}
				throw new UnsupportedOperationException(method.getName());
			});
		try (ConjurationBridge bridge = new ConjurationBridge(sheet, ignored -> false, visible::get))
		{
			assertEquals("{\"pending\":true}", bridge.catalogStats("Wolf", 20));
			assertEquals(0, builds.get());
			visible.set(true);
			String result = bridge.catalogStats("Wolf", 20);
			for (int attempt = 0; attempt < 100 && "{\"pending\":true}".equals(result); attempt++)
			{
				Thread.sleep(10);
				result = bridge.catalogStats("Wolf", 20);
			}
			assertEquals(1, builds.get());
			assertEquals("null", result);
		}
	}

	@Test
	void raceWithoutPortraitGivesTheWebViewAnEmptyAddress()
	{
		ConjurationSheetFacade sheet = (ConjurationSheetFacade) Proxy.newProxyInstance(
			getClass().getClassLoader(), new Class<?>[] {ConjurationSheetFacade.class}, (proxy, method, args) -> {
				throw new UnsupportedOperationException(method.getName());
			});
		try (ConjurationBridge bridge = new ConjurationBridge(sheet))
		{
			assertEquals("", bridge.portrait("Unknown Race"));
		}
	}

	@Test
	void creatureWithoutCardIllustrationGivesTheWebViewAnEmptyAddress()
	{
		try (ConjurationBridge bridge = new ConjurationBridge(unusedSheet()))
		{
			assertEquals("", bridge.cardPortrait("Archon (Hound)", 1));
		}
	}

	@Test
	void savedPortraitIsTheIllustrationShownOnThePage(@TempDir Path folder) throws Exception
	{
		Path chosen = folder.resolve("hound.jpg");
		String shown = PathfinderSummonPortraits.dataUriFor("Archon (Hound)").orElse("");
		try (ConjurationBridge bridge = new ConjurationBridge(unusedSheet(), ignored -> false, () -> true,
			reference -> { }, suggested -> Optional.of(chosen)))
		{
			bridge.savePortrait("Archon (Hound)");
		}

		assertEquals(shown, "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(chosen)));
	}

	@Test
	void cancelledSaveWritesNothing(@TempDir Path folder) throws Exception
	{
		try (ConjurationBridge bridge = new ConjurationBridge(unusedSheet(), ignored -> false, () -> true,
			reference -> { }, suggested -> Optional.empty()))
		{
			bridge.savePortrait("Archon (Hound)");
		}

		try (var written = Files.list(folder))
		{
			assertEquals(0, written.count());
		}
	}

	@Test
	void savedSpreadsheetIsTheWorkbookTheSheetBuilds(@TempDir Path folder) throws Exception
	{
		Path chosen = folder.resolve("summons.xlsx");
		byte[] workbook = {80, 75, 3, 4};
		ConjurationSheetFacade sheet = (ConjurationSheetFacade) Proxy.newProxyInstance(
			getClass().getClassLoader(), new Class<?>[] {ConjurationSheetFacade.class}, (proxy, method, args) -> {
				if ("catalogWorkbook".equals(method.getName()))
				{
					return workbook;
				}
				throw new UnsupportedOperationException(method.getName());
			});
		try (ConjurationBridge bridge = new ConjurationBridge(sheet, ignored -> false, () -> true,
			reference -> { }, suggested -> Optional.of(chosen)))
		{
			bridge.saveSpreadsheet();
			for (int attempt = 0; attempt < 500 && !bridge.spreadsheetStatus().contains("\"saved\""); attempt++)
			{
				Thread.sleep(10);
			}
		}

		assertArrayEquals(workbook, Files.readAllBytes(chosen));
	}

	@Test
	void reportsHowManyCreaturesTheTableHasBuiltSoFar(@TempDir Path folder) throws Exception
	{
		CountDownLatch halfway = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		ConjurationSheetFacade sheet = (ConjurationSheetFacade) Proxy.newProxyInstance(
			getClass().getClassLoader(), new Class<?>[] {ConjurationSheetFacade.class}, (proxy, method, args) -> {
				if ("catalogWorkbook".equals(method.getName()))
				{
					@SuppressWarnings("unchecked")
					BiConsumer<Integer, Integer> progress = (BiConsumer<Integer, Integer>) args[0];
					progress.accept(57, 200);
					halfway.countDown();
					assertTrue(release.await(5, TimeUnit.SECONDS));
					return new byte[0];
				}
				throw new UnsupportedOperationException(method.getName());
			});
		try (ConjurationBridge bridge = new ConjurationBridge(sheet, ignored -> false, () -> true,
			reference -> { }, suggested -> Optional.of(folder.resolve("summons.xlsx"))))
		{
			bridge.saveSpreadsheet();
			assertTrue(halfway.await(3, TimeUnit.SECONDS));
			try
			{
				assertEquals("{\"state\":\"building\",\"done\":57,\"total\":200,\"file\":\"\"}",
					bridge.spreadsheetStatus());
			}
			finally
			{
				release.countDown();
			}
		}
	}

	@Test
	void reportsWhereTheTableWasSaved(@TempDir Path folder) throws Exception
	{
		Path chosen = folder.resolve("summons.xlsx");
		ConjurationSheetFacade sheet = (ConjurationSheetFacade) Proxy.newProxyInstance(
			getClass().getClassLoader(), new Class<?>[] {ConjurationSheetFacade.class}, (proxy, method, args) -> {
				if ("catalogWorkbook".equals(method.getName()))
				{
					return new byte[0];
				}
				throw new UnsupportedOperationException(method.getName());
			});
		String status;
		try (ConjurationBridge bridge = new ConjurationBridge(sheet, ignored -> false, () -> true,
			reference -> { }, suggested -> Optional.of(chosen)))
		{
			bridge.saveSpreadsheet();
			status = bridge.spreadsheetStatus();
			for (int attempt = 0; attempt < 500 && status.contains("building"); attempt++)
			{
				Thread.sleep(10);
				status = bridge.spreadsheetStatus();
			}
		}

		assertEquals("{\"state\":\"saved\",\"done\":0,\"total\":0,\"file\":\"" + chosen.toString().replace("\\", "\\\\") + "\"}", status);
	}

	@Test
	void opensAFirstResultSearchInTheBrowser() throws Exception
	{
		CountDownLatch opened = new CountDownLatch(1);
		String address = "https://duckduckgo.com/?q=%5CWolf%20monster%20site%3Ad20pfsrd.com";
		try (ConjurationBridge bridge = new ConjurationBridge(unusedSheet(), ignored -> false, () -> true, reference -> {
			if (address.equals(reference.toString()))
			{
				opened.countDown();
			}
		}))
		{
			bridge.openReference(address);

			assertTrue(opened.await(3, TimeUnit.SECONDS));
		}
	}

		private ConjurationSheetFacade unusedSheet()
	{
		return (ConjurationSheetFacade) Proxy.newProxyInstance(
			getClass().getClassLoader(), new Class<?>[] {ConjurationSheetFacade.class}, (proxy, method, args) -> {
				throw new UnsupportedOperationException(method.getName());
			});
	}
}
