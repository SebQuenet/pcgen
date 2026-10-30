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
package pcgen.core.gametable.catalog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import pcgen.util.Logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The values of bestiary entries computed once and kept, each with the stamp of what it was computed from.
 *
 * @param entries the indexed entries.
 */
public record BestiaryIndex(Map<IndexKey, Indexed> entries)
{
	/** Where the index of the data's races and kits is delivered with PCGen. */
	public static final String DELIVERED = "/pcgen/bestiary/index.json";

	private static final ObjectMapper MAPPER = new ObjectMapper();

	/**
	 * @param facts the entry's values.
	 * @param stamp what they were computed from: a file's modification time, 0 for the data.
	 */
	public record Indexed(EntryFacts facts, long stamp)
	{
	}

	public BestiaryIndex
	{
		entries = Map.copyOf(entries);
	}

	public static BestiaryIndex empty()
	{
		return new BestiaryIndex(Map.of());
	}

	/**
	 * @return the index of the data's races and kits delivered with PCGen; empty when none is.
	 */
	public static BestiaryIndex delivered()
	{
		try (InputStream delivered = BestiaryIndex.class.getResourceAsStream(DELIVERED))
		{
			return delivered == null ? empty() : parse(new String(delivered.readAllBytes(), StandardCharsets.UTF_8));
		}
		catch (IOException unreadable)
		{
			Logging.errorPrint("The delivered bestiary index cannot be read", unreadable);
			return empty();
		}
	}

	/**
	 * @param file an index written by {@link #write}.
	 * @return the index; empty when the file is absent or not an index.
	 */
	public static BestiaryIndex read(Path file)
	{
		if (!Files.isRegularFile(file))
		{
			return empty();
		}
		try
		{
			return parse(Files.readString(file));
		}
		catch (IOException unreadable)
		{
			Logging.errorPrint("The bestiary index " + file + " cannot be read", unreadable);
			return empty();
		}
	}

	/**
	 * @param file where to keep this index.
	 * @throws IOException when it cannot be written.
	 */
	public void write(Path file) throws IOException
	{
		Path folder = file.toAbsolutePath().getParent();
		if (folder != null)
		{
			Files.createDirectories(folder);
		}
		Path written = file.resolveSibling(file.getFileName() + ".tmp");
		Files.writeString(written, toJson());
		Files.move(written, file, StandardCopyOption.REPLACE_EXISTING);
	}

	public Optional<EntryFacts> facts(IndexKey key)
	{
		return Optional.ofNullable(entries.get(key)).map(Indexed::facts);
	}

	/**
	 * @param key an entry.
	 * @return the stamp it was indexed with; empty when never indexed.
	 */
	public Optional<Long> stamp(IndexKey key)
	{
		return Optional.ofNullable(entries.get(key)).map(Indexed::stamp);
	}

	public BestiaryIndex with(IndexKey key, EntryFacts facts, long stamp)
	{
		Map<IndexKey, Indexed> more = new HashMap<>(entries);
		more.put(key, new Indexed(facts, stamp));
		return new BestiaryIndex(more);
	}

	/**
	 * @param others another index, whose entries win.
	 * @return both indexes in one.
	 */
	public BestiaryIndex merged(BestiaryIndex others)
	{
		Map<IndexKey, Indexed> merged = new HashMap<>(entries);
		merged.putAll(others.entries());
		return new BestiaryIndex(merged);
	}

	private String toJson()
	{
		ArrayNode array = MAPPER.createArrayNode();
		Map<IndexKey, Indexed> sorted = new TreeMap<>((first, second) -> first.kind().equals(second.kind())
			? first.key().compareTo(second.key()) : first.kind().compareTo(second.kind()));
		sorted.putAll(entries);
		for (Map.Entry<IndexKey, Indexed> entry : sorted.entrySet())
		{
			ObjectNode node = array.addObject();
			node.put("kind", entry.getKey().kind());
			node.put("key", entry.getKey().key());
			node.put("stamp", entry.getValue().stamp());
			entry.getValue().facts().buildFailure().ifPresent(failure -> node.put("failure", failure));
			ObjectNode values = node.putObject("values");
			new TreeMap<>(entry.getValue().facts().values()).forEach((field, value) -> write(values, field.name(), value));
		}
		return array.toString();
	}

	private static void write(ObjectNode values, String name, FieldValue value)
	{
		switch (value)
		{
			case FieldValue.Number number -> values.put(name, number.value());
			case FieldValue.Text text -> values.put(name, text.value());
			case FieldValue.Items items -> items.values().stream().sorted().forEach(values.putArray(name)::add);
			case FieldValue.Scores scores ->
			{
				ObjectNode byName = values.putObject(name);
				new TreeMap<>(scores.values()).forEach(byName::put);
			}
			case FieldValue.Flag flag -> values.put(name, flag.value());
		}
	}

	private static BestiaryIndex parse(String json)
	{
		try
		{
			Map<IndexKey, Indexed> entries = new HashMap<>();
			for (JsonNode node : MAPPER.readTree(json))
			{
				Map<CatalogField, FieldValue> values = new LinkedHashMap<>();
				node.path("values").properties().forEach(property -> field(property.getKey())
					.flatMap(field -> read(field, property.getValue())).ifPresent(read -> values.put(read.field(), read.value())));
				Optional<String> failure = node.hasNonNull("failure") ? Optional.of(node.get("failure").asText()) : Optional.empty();
				entries.put(new IndexKey(node.path("kind").asText(), node.path("key").asText()),
					new Indexed(new EntryFacts(values, failure), node.path("stamp").asLong()));
			}
			return new BestiaryIndex(entries);
		}
		catch (JsonProcessingException | RuntimeException notAnIndex)
		{
			Logging.errorPrint("A bestiary index is not readable", notAnIndex);
			return empty();
		}
	}

	private record FieldAndValue(CatalogField field, FieldValue value)
	{
	}

	private static Optional<CatalogField> field(String name)
	{
		for (CatalogField field : CatalogField.values())
		{
			if (field.name().equals(name))
			{
				return Optional.of(field);
			}
		}
		return Optional.empty();
	}

	private static Optional<FieldAndValue> read(CatalogField field, JsonNode node)
	{
		FieldValue value = switch (field.kind())
		{
			case NUMBER -> new FieldValue.Number(node.asDouble());
			case TEXT -> new FieldValue.Text(node.asText());
			case ITEMS ->
			{
				Set<String> items = new HashSet<>();
				node.forEach(item -> items.add(item.asText()));
				yield new FieldValue.Items(items);
			}
			case SCORES ->
			{
				Map<String, Integer> scores = new HashMap<>();
				node.properties().forEach(score -> scores.put(score.getKey(), score.getValue().asInt()));
				yield new FieldValue.Scores(scores);
			}
			case FLAG -> new FieldValue.Flag(node.asBoolean());
		};
		return Optional.of(new FieldAndValue(field, value));
	}
}
