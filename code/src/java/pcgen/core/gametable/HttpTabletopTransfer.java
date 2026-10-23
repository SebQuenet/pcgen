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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Sends images to the local server of TTS-cloud-manager, the game master's tool that puts files
 * in the Tabletop Simulator cloud ({@code POST /files}) and lays them on the table
 * ({@code POST /spawn}).
 */
public final class HttpTabletopTransfer implements TabletopTransfer
{
	/** Where the tool listens unless {@value #ADDRESS_VARIABLE} says otherwise. */
	public static final String DEFAULT_ADDRESS = "http://127.0.0.1:8788";
	public static final String ADDRESS_VARIABLE = "PCGEN_TABLETOP_URL";
	private static final String CLOUD_FOLDER = "PCGen";
	private static final String TILE = "Custom_Tile";
	private static final String ALREADY_THERE = "file-already-exists";
	private static final int SUCCESS_LIMIT = 300;
	private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(5);
	private static final Duration UPLOAD_TIMEOUT = Duration.ofMinutes(5);

	private final URI address;
	private final HttpClient client = HttpClient.newBuilder().connectTimeout(CONNECTION_TIMEOUT).build();
	private final ObjectMapper json = new ObjectMapper();

	/**
	 * @param address where the tool's server listens, such as http://127.0.0.1:8788.
	 */
	public HttpTabletopTransfer(URI address)
	{
		this.address = address;
	}

	/**
	 * @return a transfer to the tool at {@value #ADDRESS_VARIABLE}, or at {@value #DEFAULT_ADDRESS}.
	 */
	public static HttpTabletopTransfer fromEnvironment()
	{
		String configured = System.getenv(ADDRESS_VARIABLE);
		return new HttpTabletopTransfer(URI.create(configured == null || configured.isBlank() ? DEFAULT_ADDRESS
			: configured.strip()));
	}

	@Override
	public TabletopOutcome send(ScenarioImage image, boolean placeOnTable)
	{
		try
		{
			HttpResponse<String> uploaded = post("/files", multipart(image), UPLOAD_TIMEOUT);
			JsonNode answer = parse(uploaded.body());
			String key;
			Optional<String> url;
			if (uploaded.statusCode() < SUCCESS_LIMIT)
			{
				key = answer.path("key").asText();
				url = Optional.of(answer.path("url").asText()).filter(found -> !found.isBlank());
			}
			else if (ALREADY_THERE.equals(answer.path("failure").path("kind").asText()))
			{
				key = answer.path("failure").path("key").asText();
				url = Optional.empty();
			}
			else
			{
				return refused(uploaded, answer);
			}
			if (!placeOnTable)
			{
				return new TabletopOutcome.Sent(key, url, false);
			}
			String spawn = json.writeValueAsString(spawnRequest(key));
			HttpResponse<String> spawned = post("/spawn", new Body(spawn.getBytes(StandardCharsets.UTF_8),
				"application/json"), CONNECTION_TIMEOUT.multipliedBy(6));
			return spawned.statusCode() < SUCCESS_LIMIT ? new TabletopOutcome.Sent(key, url, true)
				: refused(spawned, parse(spawned.body()));
		}
		catch (IOException unreachable)
		{
			String why = unreachable.getMessage() == null ? unreachable.getClass().getSimpleName() : unreachable.getMessage();
			return new TabletopOutcome.Failed("Tabletop Simulator's cloud manager does not answer at " + address + " ("
				+ why + "); is it running?");
		}
		catch (InterruptedException interrupted)
		{
			Thread.currentThread().interrupt();
			return new TabletopOutcome.Failed("The transfer was interrupted");
		}
	}

	private ObjectNode spawnRequest(String key)
	{
		ObjectNode request = json.createObjectNode();
		request.putArray("keys").add(key);
		request.put("kind", TILE);
		return request;
	}

	private record Body(byte[] bytes, String contentType)
	{
	}

	private HttpResponse<String> post(String path, Body body, Duration timeout) throws IOException, InterruptedException
	{
		HttpRequest request = HttpRequest.newBuilder(address.resolve(path)).timeout(timeout)
			.header("Content-Type", body.contentType()).POST(HttpRequest.BodyPublishers.ofByteArray(body.bytes())).build();
		return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
	}

	private static Body multipart(ScenarioImage image) throws IOException
	{
		String boundary = "pcgen-" + UUID.randomUUID();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"folder\"\r\n\r\n" + CLOUD_FOLDER + "\r\n")
			.getBytes(StandardCharsets.UTF_8));
		out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + image.fileName()
			+ "\"\r\nContent-Type: " + image.format().mediaType() + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
		out.write(image.bytes());
		out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
		return new Body(out.toByteArray(), "multipart/form-data; boundary=" + boundary);
	}

	private JsonNode parse(String body)
	{
		try
		{
			return json.readTree(body.isBlank() ? "{}" : body);
		}
		catch (IOException notJson)
		{
			return json.createObjectNode();
		}
	}

	private static TabletopOutcome refused(HttpResponse<String> response, JsonNode answer)
	{
		String kind = answer.path("failure").path("kind").asText();
		return new TabletopOutcome.Failed("Tabletop Simulator's cloud manager refused the image (" + response.statusCode()
			+ (kind.isBlank() ? "" : ", " + kind) + ")");
	}
}
