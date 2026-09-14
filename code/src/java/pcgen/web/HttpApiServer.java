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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import pcgen.session.api.Arguments;
import pcgen.session.api.JsonPayload;
import pcgen.session.api.Operation;
import pcgen.session.api.OperationRegistry;
import pcgen.session.service.ServiceError;
import pcgen.session.service.ServiceResult;
import pcgen.util.Logging;

/**
 * Serves the registry over HTTP for a web front end running on the same machine.
 *
 * <p>
 * PCGen's game state is process-wide, so an operation that touches it runs alone,
 * on one worker thread. The requests themselves are handled on a pool: an
 * operation that opens a chooser blocks its worker until the choice comes back,
 * and the call that carries that choice has to get through in the meantime.
 */
public final class HttpApiServer
{
	private static final String API_PREFIX = "/api/";
	private static final int STOP_DELAY_SECONDS = 1;

	private final OperationRegistry registry;
	private final HttpServer server;
	private final ExecutorService oneAtATime;
	private final LoopbackGuard guard;
	private final ObjectMapper requestReader = new ObjectMapper();
	private final StaticFiles frontEnd;

	private HttpApiServer(OperationRegistry registry, HttpServer server, LoopbackGuard guard, StaticFiles frontEnd)
	{
		this.registry = registry;
		this.server = server;
		this.guard = guard;
		this.frontEnd = frontEnd;
		this.oneAtATime = Executors.newSingleThreadExecutor(runnable -> {
			Thread worker = new Thread(runnable, "pcgen-operations");
			worker.setDaemon(true);
			return worker;
		});
	}

	/**
	 * Start listening on the loopback address.
	 *
	 * @param port the port to listen on, or 0 to let the system pick one
	 */
	public static HttpApiServer start(OperationRegistry registry, int port) throws IOException
	{
		return start(registry, port, Path.of("web"));
	}

	/**
	 * Start listening on the loopback address, serving a front end's files from
	 * {@code frontEndRoot} when that directory exists.
	 *
	 * @param port the port to listen on, or 0 to let the system pick one
	 */
	public static HttpApiServer start(OperationRegistry registry, int port, Path frontEndRoot) throws IOException
	{
		HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
		HttpApiServer api = new HttpApiServer(registry, server,
			new LoopbackGuard(server.getAddress().getPort()), new StaticFiles(frontEndRoot));
		server.createContext("/health", api::handleHealth);
		server.createContext("/api/operations", api::handleOperations);
		server.createContext(API_PREFIX, api::handleCall);
		server.createContext("/", api::handleFrontEnd);
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();
		Logging.log(Level.INFO, "PCGen HTTP API listening on http://127.0.0.1:" + api.port());
		return api;
	}

	public int port()
	{
		return server.getAddress().getPort();
	}

	public void stop()
	{
		server.stop(STOP_DELAY_SECONDS);
		oneAtATime.shutdownNow();
	}

	private void handleFrontEnd(HttpExchange exchange) throws IOException
	{
		if (!allowed(exchange))
		{
			return;
		}
		Optional<StaticFiles.Served> file = frontEnd.at(exchange.getRequestURI().getPath());
		if (file.isEmpty())
		{
			respond(exchange, 404, ApiResponse.of(
				new ServiceError.EntryNotFound("Path", exchange.getRequestURI().getPath())));
			return;
		}
		exchange.getResponseHeaders().add("Content-Type", file.get().contentType());
		exchange.sendResponseHeaders(200, file.get().bytes().length);
		try (OutputStream out = exchange.getResponseBody())
		{
			out.write(file.get().bytes());
		}
	}

	private void handleHealth(HttpExchange exchange) throws IOException
	{
		if (!allowed(exchange))
		{
			return;
		}
		respond(exchange, 200, ApiResponse.of(Map.of("status", "ok", "operations", registry.all().size())));
	}

	private void handleOperations(HttpExchange exchange) throws IOException
	{
		if (!allowed(exchange))
		{
			return;
		}
		List<Map<String, Object>> described = new ArrayList<>();
		for (Operation operation : registry.all())
		{
			described.add(Map.of(
				"name", operation.name(),
				"description", operation.description(),
				"input_schema", operation.inputSchema()));
		}
		respond(exchange, 200, ApiResponse.of(described));
	}

	private void handleCall(HttpExchange exchange) throws IOException
	{
		if (!allowed(exchange))
		{
			return;
		}
		if (!"POST".equalsIgnoreCase(exchange.getRequestMethod()))
		{
			respond(exchange, 405, ApiResponse.of(new ServiceError.NotAllowed("Operations are called with POST")));
			return;
		}

		String name = exchange.getRequestURI().getPath().substring(API_PREFIX.length());
		Operation operation = registry.find(name).orElse(null);
		if (operation == null)
		{
			respond(exchange, 404, ApiResponse.of(new ServiceError.EntryNotFound("Operation", name)));
			return;
		}

		Arguments arguments;
		try
		{
			arguments = Arguments.of(readArguments(exchange));
		}
		catch (IOException e)
		{
			respond(exchange, 400, ApiResponse.of(new ServiceError.InvalidArgument("body", "must be a JSON object")));
			return;
		}

		respondTo(exchange, call(operation, arguments));
	}

	private ServiceResult<?> call(Operation operation, Arguments arguments)
	{
		if (!operation.needsExclusiveAccess())
		{
			return operation.call(arguments);
		}
		try
		{
			return oneAtATime.submit(() -> operation.call(arguments)).get();
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			return ServiceResult.failure(new ServiceError.DataFailure("The call was interrupted"));
		}
		catch (ExecutionException e)
		{
			Throwable cause = e.getCause() == null ? e : e.getCause();
			Logging.errorPrint("Operation " + operation.name() + " failed", cause);
			return ServiceResult.failure(new ServiceError.DataFailure(String.valueOf(cause.getMessage())));
		}
	}

	private Map<String, Object> readArguments(HttpExchange exchange) throws IOException
	{
		byte[] body = exchange.getRequestBody().readAllBytes();
		if (body.length == 0)
		{
			return Map.of();
		}
		return requestReader.readValue(body, new TypeReference<Map<String, Object>>()
		{
		});
	}

	private void respondTo(HttpExchange exchange, ServiceResult<?> result) throws IOException
	{
		switch (result)
		{
			case ServiceResult.Success<?> success -> respond(exchange, 200, ApiResponse.of(success.value()));
			case ServiceResult.Failure<?> failure ->
				respond(exchange, ApiResponse.statusFor(failure.error()), ApiResponse.of(failure.error()));
		}
	}

	private boolean allowed(HttpExchange exchange) throws IOException
	{
		String host = exchange.getRequestHeaders().getFirst("Host");
		String origin = exchange.getRequestHeaders().getFirst("Origin");
		if (guard.accepts(host, origin))
		{
			return true;
		}
		respond(exchange, 403, ApiResponse.of(new ServiceError.NotAllowed("This server answers the local machine only")));
		return false;
	}

	private void respond(HttpExchange exchange, int status, ApiResponse.Envelope envelope) throws IOException
	{
		byte[] body = JsonPayload.write(envelope).getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
		exchange.sendResponseHeaders(status, body.length);
		try (OutputStream out = exchange.getResponseBody())
		{
			out.write(body);
		}
	}
}
