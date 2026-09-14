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
package pcgen.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pcgen.session.PcgenSession;
import pcgen.session.api.Operation;
import pcgen.session.api.OperationRegistry;
import pcgen.session.service.ServiceResult;

/**
 * What the HTTP server answers, before any game data is loaded.
 */
public class HttpApiServerTest
{
	private static final String EMPTY_SCHEMA = """
		{ "type": "object", "properties": {}, "required": [] }
		""";

	private HttpApiServer server;
	private HttpClient client;

	@BeforeEach
	public void startServer() throws IOException
	{
		server = HttpApiServer.start(OperationRegistry.forSession(new PcgenSession()), 0);
		client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
	}

	@AfterEach
	public void stopServer()
	{
		server.stop();
	}

	@Test
	public void reportsItsHealthAndHowManyOperationsItServes() throws Exception
	{
		HttpResponse<String> response = get("/health");

		assertEquals(200, response.statusCode());
		assertTrue(response.body().contains("\"status\" : \"ok\""), response.body());
	}

	@Test
	public void describesEveryOperationItServes() throws Exception
	{
		HttpResponse<String> response = get("/api/operations");

		assertEquals(200, response.statusCode());
		assertTrue(response.body().contains("load_sources"), response.body());
		assertTrue(response.body().contains("input_schema"), response.body());
	}

	@Test
	public void answersNotFoundForAnOperationItDoesNotServe() throws Exception
	{
		HttpResponse<String> response = post("/api/summon_dragon", "{}");

		assertEquals(404, response.statusCode());
		assertTrue(response.body().contains("EntryNotFound"), response.body());
	}

	@Test
	public void answersBadRequestWhenARequiredArgumentIsMissing() throws Exception
	{
		HttpResponse<String> response = post("/api/load_sources", "{}");

		assertEquals(400, response.statusCode());
		assertTrue(response.body().contains("game_mode"), response.body());
	}

	@Test
	public void answersBadRequestWhenTheBodyIsNotAJsonObject() throws Exception
	{
		HttpResponse<String> response = post("/api/load_sources", "not json");

		assertEquals(400, response.statusCode());
	}

	@Test
	public void refusesACallFromAPageServedSomewhereElse() throws Exception
	{
		HttpRequest request = HttpRequest.newBuilder(uri("/health"))
			.header("Origin", "https://evil.example.com")
			.GET()
			.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		assertEquals(403, response.statusCode());
	}

	@Test
	public void refusesToCallAnOperationWithAnythingButPost() throws Exception
	{
		HttpResponse<String> response = get("/api/load_sources");

		assertEquals(405, response.statusCode());
	}

	/**
	 * The deadlock the single worker would cause if every operation queued behind
	 * every other: a call that opens a chooser waits for the answer, and the call
	 * carrying that answer has to reach the session while it waits.
	 */
	@Test
	public void answersAConcurrentCallWhileAnExclusiveOneIsBlocked() throws Exception
	{
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch released = new CountDownLatch(1);
		server.stop();
		server = HttpApiServer.start(OperationRegistry.of(List.of(
			Operation.exclusive("wait_for_a_choice", "blocks until the choice arrives", EMPTY_SCHEMA,
				arguments -> {
					started.countDown();
					awaitQuietly(released);
					return ServiceResult.success("unblocked");
				}),
			Operation.concurrent("send_the_choice", "answers the waiting call", EMPTY_SCHEMA,
				arguments -> {
					released.countDown();
					return ServiceResult.success("sent");
				}))), 0);

		CompletableFuture<HttpResponse<String>> blocked =
			CompletableFuture.supplyAsync(() -> postQuietly("/api/wait_for_a_choice"));
		assertTrue(started.await(5, TimeUnit.SECONDS), "the blocking call never started");

		HttpResponse<String> answer = post("/api/send_the_choice", "{}");

		assertEquals(200, answer.statusCode());
		assertEquals(200, blocked.get(5, TimeUnit.SECONDS).statusCode());
	}

	private HttpResponse<String> postQuietly(String path)
	{
		try
		{
			return post(path, "{}");
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
	}

	private static void awaitQuietly(CountDownLatch latch)
	{
		try
		{
			latch.await(5, TimeUnit.SECONDS);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
	}

	private HttpResponse<String> get(String path) throws Exception
	{
		return client.send(HttpRequest.newBuilder(uri(path)).GET().build(),
			HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> post(String path, String body) throws Exception
	{
		HttpRequest request = HttpRequest.newBuilder(uri(path))
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build();
		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private URI uri(String path)
	{
		return URI.create("http://127.0.0.1:" + server.port() + path);
	}
}
