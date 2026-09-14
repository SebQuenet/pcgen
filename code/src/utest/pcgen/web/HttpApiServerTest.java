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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pcgen.session.PcgenSession;
import pcgen.session.api.OperationRegistry;

/**
 * What the HTTP server answers, before any game data is loaded.
 */
public class HttpApiServerTest
{
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
