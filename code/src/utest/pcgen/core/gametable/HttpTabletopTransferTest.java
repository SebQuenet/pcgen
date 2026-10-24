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
package pcgen.core.gametable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Images sent to a stand-in for the TTS-cloud-manager server, which runs on the local machine.
 */
class HttpTabletopTransferTest
{
	private static final ScenarioImage CLEARING = ScenarioImageTest.image(3, "Clearing", ImageCategory.BATTLEMAP);

	private final List<String> received = new ArrayList<>();
	private HttpServer tool;
	private int uploadStatus = 200;
	private String uploadAnswer = "{\"key\":\"abc-Clearing.png\",\"url\":\"https://steam/abc\"}";

	@BeforeEach
	void startTheTool() throws IOException
	{
		tool = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		tool.createContext("/files", exchange -> {
			received.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1));
			answer(exchange, uploadStatus, uploadAnswer);
		});
		tool.createContext("/spawn", exchange -> {
			received.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			answer(exchange, 200, "{\"spawned\":1}");
		});
		tool.start();
	}

	@AfterEach
	void stopTheTool()
	{
		tool.stop(0);
	}

	private static void answer(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException
	{
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(status, bytes.length);
		try (OutputStream out = exchange.getResponseBody())
		{
			out.write(bytes);
		}
	}

	private TabletopTransfer transfer()
	{
		return new HttpTabletopTransfer(URI.create("http://127.0.0.1:" + tool.getAddress().getPort()));
	}

	@Test
	void anImageGoesToTheCloud()
	{
		assertEquals(new TabletopOutcome.Sent("abc-Clearing.png", Optional.of("https://steam/abc"), false),
			transfer().send(CLEARING.forTabletop(), false));
	}

	@Test
	void anImageIsFiledInThePcgenFolderUnderItsFileName()
	{
		transfer().send(CLEARING.forTabletop(), false);

		assertTrue(received.getFirst().contains("PCGen") && received.getFirst().contains("filename=\"Clearing.png\""),
			received.getFirst());
	}

	@Test
	void anImageAlreadyInTheCloudKeepsItsKey()
	{
		uploadStatus = 409;
		uploadAnswer = "{\"failure\":{\"kind\":\"file-already-exists\",\"key\":\"abc-Clearing.png\"}}";

		assertEquals(new TabletopOutcome.Sent("abc-Clearing.png", Optional.empty(), false), transfer().send(CLEARING.forTabletop(), false));
	}

	@Test
	void anImagePlacedOnTheTableIsSpawnedAsATile()
	{
		transfer().send(CLEARING.forTabletop(), true);

		assertEquals("{\"keys\":[\"abc-Clearing.png\"],\"kind\":\"Custom_Tile\"}", received.getLast());
	}

	@Test
	void aPawnPlacedOnTheTableIsSpawnedAsAFigurine()
	{
		TabletopUpload pawn = new TabletopUpload("aboleth.jpg", ImageFormat.JPEG, new byte[]{1}, TabletopPiece.FIGURINE);

		transfer().send(pawn, true);

		assertTrue(received.getLast().contains("\"kind\":\"Figurine_Custom\""), received.getLast());
	}

	@Test
	void aRefusalIsReportedWithItsReason()
	{
		uploadStatus = 503;
		uploadAnswer = "{\"failure\":{\"kind\":\"steam-unavailable\"}}";

		TabletopOutcome outcome = transfer().send(CLEARING.forTabletop(), false);

		assertTrue(outcome instanceof TabletopOutcome.Failed failed && failed.reason().contains("steam-unavailable"),
			outcome.toString());
	}

	@Test
	void anUnreachableToolIsReported()
	{
		tool.stop(0);

		assertInstanceOf(TabletopOutcome.Failed.class, transfer().send(CLEARING.forTabletop(), false));
	}

	@Test
	void anUnreachableToolIsReportedWithAReadableReason()
	{
		tool.stop(0);

		TabletopOutcome outcome = transfer().send(CLEARING.forTabletop(), false);

		assertTrue(outcome instanceof TabletopOutcome.Failed failed && !failed.reason().endsWith("null"), outcome.toString());
	}
}
