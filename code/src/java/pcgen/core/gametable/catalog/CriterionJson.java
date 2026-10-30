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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Criteria and presets as JSON, to keep them in the preferences.
 */
public final class CriterionJson
{
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final String FIELD = "field";
	private static final String OPERATOR = "operator";
	private static final String OPERANDS = "operands";
	private static final String JOINER = "joiner";
	private static final String MEMBERS = "members";
	private static final String NAME = "name";
	private static final String CRITERION = "criterion";

	private CriterionJson()
	{
	}

	/**
	 * @param criterion a criterion.
	 * @return it as JSON.
	 */
	public static String write(Criterion criterion)
	{
		return toNode(criterion).toString();
	}

	/**
	 * @param json a criterion as {@link #write} wrote it.
	 * @return the criterion, without tests on fields or operators this version lacks; an empty group when the text
	 *         is not a criterion.
	 */
	public static CriterionReading read(String json)
	{
		List<String> ignored = new ArrayList<>();
		try
		{
			return new CriterionReading(fromNode(MAPPER.readTree(json), ignored), ignored);
		}
		catch (JsonProcessingException | IllegalArgumentException unreadable)
		{
			return new CriterionReading(new Criterion.Group(Joiner.ALL, List.of()), ignored);
		}
	}

	/**
	 * @param presets presets.
	 * @return them as JSON.
	 */
	public static String writePresets(List<Preset> presets)
	{
		ArrayNode array = MAPPER.createArrayNode();
		for (Preset preset : presets)
		{
			ObjectNode node = array.addObject();
			node.put(NAME, preset.name());
			node.set(CRITERION, toNode(preset.criterion()));
		}
		return array.toString();
	}

	/**
	 * @param json presets as {@link #writePresets} wrote them.
	 * @return the presets; none when the text is not presets.
	 */
	public static List<Preset> readPresets(String json)
	{
		List<Preset> presets = new ArrayList<>();
		try
		{
			for (JsonNode node : MAPPER.readTree(json))
			{
				presets.add(new Preset(node.path(NAME).asText(), fromNode(node.path(CRITERION), new ArrayList<>())));
			}
			return presets;
		}
		catch (JsonProcessingException | IllegalArgumentException unreadable)
		{
			return List.of();
		}
	}

	private static ObjectNode toNode(Criterion criterion)
	{
		ObjectNode node = MAPPER.createObjectNode();
		switch (criterion)
		{
			case Criterion.Test test ->
			{
				node.put(FIELD, test.field().name());
				node.put(OPERATOR, test.operator().name());
				ArrayNode operands = node.putArray(OPERANDS);
				test.operands().forEach(operands::add);
			}
			case Criterion.Group group ->
			{
				node.put(JOINER, group.joiner().name());
				ArrayNode members = node.putArray(MEMBERS);
				group.members().forEach(member -> members.add(toNode(member)));
			}
		}
		return node;
	}

	private static Criterion fromNode(JsonNode node, List<String> ignored)
	{
		if (node.has(JOINER))
		{
			List<Criterion> members = new ArrayList<>();
			for (JsonNode member : node.path(MEMBERS))
			{
				Criterion read = fromNode(member, ignored);
				if (read != null)
				{
					members.add(read);
				}
			}
			return new Criterion.Group(Joiner.valueOf(node.path(JOINER).asText()), members);
		}
		String field = node.path(FIELD).asText();
		String operator = node.path(OPERATOR).asText();
		if (!isConstant(CatalogField.class, field) || !isConstant(Operator.class, operator))
		{
			ignored.add(isConstant(CatalogField.class, field) ? operator : field);
			return null;
		}
		List<String> operands = new ArrayList<>();
		node.path(OPERANDS).forEach(operand -> operands.add(operand.asText()));
		return new Criterion.Test(CatalogField.valueOf(field), Operator.valueOf(operator), operands);
	}

	private static <E extends Enum<E>> boolean isConstant(Class<E> type, String name)
	{
		for (E constant : type.getEnumConstants())
		{
			if (constant.name().equals(name))
			{
				return true;
			}
		}
		return false;
	}
}
