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
package pcgen.core.conjurations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the source text of a conjuration catalog into a {@link SummonCatalog}.
 *
 * <p>
 * The syntax is line oriented, like the tactical plan's. {@code ## } opens a
 * group; {@code summon:} at column zero names a creature's race; an indented
 * {@code template:} line under it lists the templates laid on that race,
 * separated by commas. The two characters {@code \n} in a value stand for a
 * line break.
 */
public final class SummonCatalogParser
{
	private static final String GROUP_PREFIX = "## ";
	private static final String INDENT = "  ";
	private static final String SUMMON_KEY = "summon";
	private static final String TEMPLATE_KEY = "template";
	private static final String TEMPLATE_SEPARATOR = ",";
	private static final String ROLE_KEY = "role";
	private static final Set<String> METADATA_KEYS = Set.of("race", "level", "kit", "source", "speed", "hp", "ac",
		"attack", "damage", "special", "cmb", "cmd", "ref", "fort", "will", "init", "spells", "note");
	private static final String ESCAPED_NEWLINE = "\\n";
	private static final Pattern KEYED_LINE = Pattern.compile("([A-Za-z]+):(?:\\s(.*))?");

	private SummonCatalogParser()
	{
	}

	/**
	 * Read a catalog.
	 *
	 * @param source the catalog's source text
	 * @return the catalog it describes, or every error that stopped it becoming
	 *         one
	 */
	public static SummonParseResult parse(String source)
	{
		return new Reading(source).read();
	}

	/**
	 * Read a catalog for a caller with nowhere to report an error.
	 *
	 * @param source the catalog's source text
	 * @return the catalog it describes, empty when it does not read
	 */
	public static Optional<SummonCatalog> catalogOf(String source)
	{
		return switch (parse(source))
		{
			case SummonParseSuccess success -> Optional.of(success.catalog());
			case SummonParseFailure ignored -> Optional.empty();
		};
	}

	/**
	 * One pass over the source text, gathering groups and errors as it goes.
	 */
	private static final class Reading
	{
		private final List<String> lines;
		private final List<SummonParseError> errors = new ArrayList<>();
		private final List<SummonGroup> groups = new ArrayList<>();
		private final Set<String> takenNames = new HashSet<>();
		private List<SummonTemplate> summons = new ArrayList<>();
		private String groupTitle;
		private int groupLine;
		private boolean groupHadSummonLine;
		private int cursor;

		private Reading(String source)
		{
			lines = (source == null) ? List.of() : List.of(source.split("\n", -1));
		}

		private SummonParseResult read()
		{
			while (cursor < lines.size())
			{
				String line = lines.get(cursor);
				int lineNumber = cursor + 1;
				cursor++;
				if (line.isBlank())
				{
					continue;
				}
				if (line.startsWith("# versatile-sm: "))
				{
					String template = line.substring("# versatile-sm: ".length()).strip();
					if (!SummonSelections.isMonsterTemplate(template))
					{
						errors.add(new SummonParseError(lineNumber, "Unknown Versatile Summon Monster template: "
							+ template));
					}
					continue;
				}
				if (line.startsWith(GROUP_PREFIX))
				{
					closeGroup();
					groupTitle = unescape(line.substring(GROUP_PREFIX.length()).strip());
					groupLine = lineNumber;
					continue;
				}
				if (line.startsWith(INDENT))
				{
					errors.add(new SummonParseError(lineNumber, "This line is indented but no summon opens above it"));
					continue;
				}
				readBlock(line, lineNumber);
			}
			closeGroup();
			return result();
		}

		private void readBlock(String line, int lineNumber)
		{
			if (groupTitle == null)
			{
				errors.add(new SummonParseError(lineNumber, "A catalog opens with a group, written '## title'"));
				skipFieldLines();
				return;
			}
			groupHadSummonLine = true;
			Optional<KeyedValue> block = keyed(line);
			if (block.isEmpty() || !SUMMON_KEY.equals(block.get().key()))
			{
				errors.add(new SummonParseError(lineNumber,
					"Unknown block " + keyOf(line) + "; a creature is written 'summon: race'"));
				skipFieldLines();
				return;
			}
			readSummon(block.get().value(), lineNumber);
		}

		private void readSummon(String raceKey, int lineNumber)
		{
			List<String> templates = List.of();
			boolean templatesWritten = false;
			List<String> roles = List.of();
			boolean rolesWritten = false;
			Map<String, String> metadata = new LinkedHashMap<>();
			while (cursor < lines.size() && lines.get(cursor).startsWith(INDENT))
			{
				String line = lines.get(cursor).strip();
				int fieldLine = cursor + 1;
				cursor++;
				if (line.isEmpty())
				{
					continue;
				}
				Optional<KeyedValue> keyedValue = keyed(line);
				String key = keyedValue.map(KeyedValue::key).orElse("");
				if (TEMPLATE_KEY.equals(key))
				{
					if (templatesWritten)
					{
						errors.add(new SummonParseError(fieldLine,
							"'" + TEMPLATE_KEY + ":' is written twice for '" + raceKey + "'"));
					}
					else
					{
						templatesWritten = true;
						templates = listOf(keyedValue.get().value());
					}
				}
				else if (ROLE_KEY.equals(key))
				{
					if (rolesWritten)
					{
						errors.add(new SummonParseError(fieldLine,
							"'role:' is written twice for '" + raceKey + "'"));
					}
					else
					{
						rolesWritten = true;
						roles = listOf(keyedValue.get().value());
					}
				}
				else if (METADATA_KEYS.contains(key))
				{
					if (metadata.putIfAbsent(key, keyedValue.get().value()) != null)
					{
						errors.add(new SummonParseError(fieldLine,
							"'" + key + ":' is written twice for '" + raceKey + "'"));
					}
				}
				else
				{
					errors.add(new SummonParseError(fieldLine,
						"A summon takes 'template:', 'role:' or a known stat field, not " + keyOf(line)));
				}
			}
			if (raceKey.isBlank())
			{
				errors.add(new SummonParseError(lineNumber, "A summon needs a race, written 'summon: race'"));
				return;
			}
			SummonTemplate summon = new SummonTemplate(raceKey, templates, lineNumber, roles, metadata);
			if (!takenNames.add(summon.name().toLowerCase(Locale.ROOT)))
			{
				errors.add(new SummonParseError(lineNumber, "Another summon is already named '" + summon.name() + "'"));
				return;
			}
			summons.add(summon);
		}

		private void skipFieldLines()
		{
			while (cursor < lines.size() && lines.get(cursor).startsWith(INDENT))
			{
				cursor++;
			}
		}

		private void closeGroup()
		{
			if (groupTitle == null)
			{
				return;
			}
			if (groupTitle.isBlank())
			{
				errors.add(new SummonParseError(groupLine, "A group needs a title, written '## title'"));
			}
			else if (!summons.isEmpty())
			{
				groups.add(new SummonGroup(groupTitle, summons));
			}
			else if (!groupHadSummonLine)
			{
				errors.add(new SummonParseError(groupLine, "Group '" + groupTitle + "' holds no summon"));
			}
			summons = new ArrayList<>();
			groupTitle = null;
			groupHadSummonLine = false;
		}

		private SummonParseResult result()
		{
			if (!errors.isEmpty())
			{
				errors.sort(Comparator.comparingInt(SummonParseError::line));
				return new SummonParseFailure(errors);
			}
			if (groups.isEmpty() && lines.stream().noneMatch(line -> line.startsWith("# versatile-sm: ")))
			{
				return new SummonParseFailure(
					List.of(new SummonParseError(1, "A catalog holds at least one group, written '## title'")));
			}
			return new SummonParseSuccess(new SummonCatalog(groups));
		}

		private static List<String> listOf(String value)
		{
			return Arrays.stream(value.split(TEMPLATE_SEPARATOR)).map(String::strip).filter(key -> !key.isEmpty())
				.toList();
		}

		private static Optional<KeyedValue> keyed(String line)
		{
			Matcher matcher = KEYED_LINE.matcher(line);
			if (!matcher.matches())
			{
				return Optional.empty();
			}
			String value = (matcher.group(2) == null) ? "" : unescape(matcher.group(2).strip());
			return Optional.of(new KeyedValue(matcher.group(1), value));
		}

		private static String unescape(String value)
		{
			return value.replace(ESCAPED_NEWLINE, "\n");
		}

		private static String keyOf(String line)
		{
			int colon = line.indexOf(':');
			return "'" + ((colon < 0) ? line.strip() : line.substring(0, colon).strip()) + "'";
		}
	}

	/**
	 * A line written {@code key: value}.
	 *
	 * @param key   what comes before the colon
	 * @param value everything after it, stripped and unescaped
	 */
	private record KeyedValue(String key, String value)
	{
	}
}
