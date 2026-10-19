/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package pcgen.gui2.tabs.conjurations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import pcgen.facade.core.ConjurationSheetFacade;

import org.junit.jupiter.api.Test;

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
}
