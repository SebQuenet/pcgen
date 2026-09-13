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
package pcgen.core.tactics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Reads the source text of a tactical plan into a {@link TacticalSheet}.
 *
 * <p>
 * The syntax is line oriented. {@code ## } opens a section; one of the block
 * keys at column zero opens a block; a sub key indented two spaces adds to the
 * block above it; anything else indented is prose belonging to a note. Fields
 * within a line are separated by {@code |}, and {@code @kind(name)} points at
 * something the character owns.
 *
 * <p>
 * A sub key is only a key when it is one this parser knows, so prose is free to
 * contain a colon.
 */
public final class TacticalPlanParser
{
	private static final String SECTION_PREFIX = "## ";
	private static final String INDENT = "  ";
	private static final String FIELD_SEPARATOR = "|";
	private static final int WRITTEN_ATTACK_FIELDS = 4;
	private static final String ESCAPED_NEWLINE = "\\n";

	private TacticalPlanParser()
	{
	}

	/**
	 * Read a plan.
	 *
	 * @param source the plan's source text
	 * @return the sheet it describes, or every error that stopped it becoming one
	 */
	public static TacticalParseResult parse(String source)
	{
		return new Reading(source).read();
	}

	/**
	 * Read a plan for a caller with nowhere to report an error, such as an
	 * export token.
	 *
	 * <p>
	 * Callers that can show an error — the editor, the MCP server — use
	 * {@link #parse(String)} and look at the failure instead.
	 *
	 * @param source the plan's source text
	 * @return the sheet it describes, empty when it does not read
	 */
	public static Optional<TacticalSheet> sheetOf(String source)
	{
		return switch (parse(source))
		{
			case TacticalParseSuccess success -> Optional.of(success.sheet());
			case TacticalParseFailure ignored -> Optional.empty();
		};
	}

	/**
	 * One pass over the source text, gathering sections and errors as it goes.
	 */
	private static final class Reading
	{
		private final List<String> lines;
		private final List<TacticalParseError> errors = new ArrayList<>();
		private final List<TacticalSection> sections = new ArrayList<>();
		private final Set<String> resourceLabels = new HashSet<>();
		private List<TacticalBlock> blocks = new ArrayList<>();
		private String sectionTitle;
		private boolean sectionHadBlockLine;
		private int cursor;

		private Reading(String source)
		{
			lines = (source == null) ? List.of() : List.of(source.split("\n", -1));
		}

		private TacticalParseResult read()
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
				if (line.startsWith(SECTION_PREFIX))
				{
					closeSection();
					sectionTitle = unescape(line.substring(SECTION_PREFIX.length()).strip());
					continue;
				}
				if (line.startsWith(INDENT))
				{
					errors.add(new TacticalParseError(lineNumber, "This line is indented but no block opens above it"));
					continue;
				}
				readBlock(line, lineNumber);
			}
			closeSection();
			return result();
		}

		private void readBlock(String line, int lineNumber)
		{
			if (sectionTitle == null)
			{
				errors.add(new TacticalParseError(lineNumber, "A plan opens with a section, written '## title'"));
				skipSubLines();
				return;
			}
			sectionHadBlockLine = true;
			Optional<TacticalBlock> block = buildBlock(line, lineNumber);
			block.ifPresent(blocks::add);
		}

		private Optional<TacticalBlock> buildBlock(String line, int lineNumber)
		{
			Optional<String> step = valueOf(line, "step");
			if (step.isPresent())
			{
				return readStep(step.get(), lineNumber);
			}
			Optional<String> note = valueOf(line, "note");
			if (note.isPresent())
			{
				return readNote(note.get(), lineNumber);
			}
			Optional<String> resource = valueOf(line, "resource");
			if (resource.isPresent())
			{
				return readResource(resource.get(), lineNumber);
			}
			Optional<String> attack = valueOf(line, "attack");
			if (attack.isPresent())
			{
				return readAttack(attack.get(), lineNumber);
			}
			Optional<String> creature = valueOf(line, "creature");
			if (creature.isPresent())
			{
				return readCreature(creature.get(), lineNumber);
			}
			Optional<String> spells = valueOf(line, "spells");
			if (spells.isPresent())
			{
				return readSpells(spells.get(), lineNumber);
			}
			errors.add(new TacticalParseError(lineNumber, "Unknown block: " + keyOf(line)));
			skipSubLines();
			return Optional.empty();
		}

		private Optional<TacticalBlock> readStep(String trigger, int lineNumber)
		{
			String actions = null;
			String note = "";
			for (SubLine sub : subLines("step", List.of("do", "note")))
			{
				if ("do".equals(sub.key()))
				{
					actions = sub.value();
				}
				else
				{
					note = sub.value();
				}
			}
			if (actions == null)
			{
				errors.add(new TacticalParseError(lineNumber, "A step needs a 'do:' line saying what to do"));
				return Optional.empty();
			}
			String stepActions = actions;
			String stepNote = note;
			return block(() -> new TacticalStep(trigger, stepActions, stepNote), lineNumber);
		}

		private Optional<TacticalBlock> readNote(String title, int lineNumber)
		{
			List<String> prose = new ArrayList<>();
			while (cursor < lines.size() && lines.get(cursor).startsWith(INDENT))
			{
				prose.add(lines.get(cursor).strip());
				cursor++;
			}
			if (prose.isEmpty())
			{
				errors.add(new TacticalParseError(lineNumber, "A note needs at least one indented line of prose"));
				return Optional.empty();
			}
			return block(() -> new TacticalNote(title, String.join("\n", prose)), lineNumber);
		}

		private Optional<TacticalBlock> readResource(String head, int lineNumber)
		{
			refuseSubLines("resource");
			List<String> fields = fieldsOf(head);
			if (fields.size() < 2)
			{
				errors.add(new TacticalParseError(lineNumber,
					"A resource is written 'resource: label | maximum | action', and the maximum is not optional"));
				return Optional.empty();
			}
			Optional<TacticalSubject> maximum = subject(fields.get(1), lineNumber);
			if (maximum.isEmpty())
			{
				return Optional.empty();
			}
			String label = fields.get(0);
			if (!resourceLabels.add(label.strip()))
			{
				errors.add(new TacticalParseError(lineNumber, "Another resource is already labelled '" + label + "'"));
				return Optional.empty();
			}
			String action = (fields.size() > 2) ? fields.get(2) : "";
			return block(() -> new TacticalResource(label, maximum.get(), action), lineNumber);
		}

		private Optional<TacticalBlock> readAttack(String head, int lineNumber)
		{
			List<String> fields = fieldsOf(head);
			Optional<TacticalSubject> subject = subject(fields.getFirst(), lineNumber);
			List<TacticalVariant> variants = new ArrayList<>();
			String note = "";
			for (SubLine sub : subLines("attack", List.of("target", "note")))
			{
				if ("note".equals(sub.key()))
				{
					note = sub.value();
					continue;
				}
				List<String> parts = fieldsOf(sub.value());
				if (parts.size() < 2)
				{
					errors.add(new TacticalParseError(sub.line(),
						"A target variant is written 'target: kind of target | what changes'"));
					continue;
				}
				variants.add(new TacticalVariant(parts.get(0), parts.get(1)));
			}
			if (subject.isEmpty())
			{
				return Optional.empty();
			}
			if (fields.size() > 1 && fields.size() < WRITTEN_ATTACK_FIELDS)
			{
				errors.add(new TacticalParseError(lineNumber,
					"An attack written out needs all of 'attack: name | to hit | damage | crit'"));
				return Optional.empty();
			}
			Optional<String> toHit = fieldAt(fields, 1);
			Optional<String> damage = fieldAt(fields, 2);
			Optional<String> critical = fieldAt(fields, 3);
			String attackNote = note;
			return block(() -> new TacticalAttack(subject.get(), toHit, damage, critical, variants, attackNote),
				lineNumber);
		}

		private Optional<TacticalBlock> readCreature(String head, int lineNumber)
		{
			List<String> fields = fieldsOf(head);
			List<TacticalRow> rows = new ArrayList<>();
			for (SubLine sub : subLines("creature", List.of("row")))
			{
				List<String> parts = fieldsOf(sub.value());
				if (parts.size() < 2)
				{
					errors.add(new TacticalParseError(sub.line(), "A row is written 'row: heading | contents'"));
					continue;
				}
				rows.add(new TacticalRow(parts.get(0), parts.get(1)));
			}
			if (rows.isEmpty())
			{
				errors.add(new TacticalParseError(lineNumber, "A creature needs at least one 'row:' line"));
				return Optional.empty();
			}
			String name = fields.getFirst();
			String source = fieldAt(fields, 1).orElse("");
			String duration = fieldAt(fields, 2).orElse("");
			return block(() -> new TacticalCreature(name, source, duration, rows), lineNumber);
		}

		private Optional<TacticalBlock> readSpells(String head, int lineNumber)
		{
			List<TacticalTag> tags = new ArrayList<>();
			for (SubLine sub : subLines("spells", List.of("tag")))
			{
				List<String> parts = fieldsOf(sub.value());
				if (parts.size() < 2)
				{
					errors.add(new TacticalParseError(sub.line(), "A tag is written 'tag: spell | tag, tag'"));
					continue;
				}
				tags.add(new TacticalTag(parts.get(0), List.of(parts.get(1).split(","))));
			}
			Optional<SpellSource> source = spellSource(head, lineNumber);
			if (source.isEmpty())
			{
				return Optional.empty();
			}
			return block(() -> new TacticalSpellList(source.get(), tags), lineNumber);
		}

		private Optional<SpellSource> spellSource(String head, int lineNumber)
		{
			return switch (head.strip())
			{
				case "@prepared" -> Optional.of(SpellSource.PREPARED);
				case "@known" -> Optional.of(SpellSource.KNOWN);
				default ->
				{
					errors.add(new TacticalParseError(lineNumber,
						"A spell repertoire reads '@prepared' or '@known', not '" + head.strip() + "'"));
					yield Optional.empty();
				}
			};
		}

		private Optional<TacticalSubject> subject(String text, int lineNumber)
		{
			String candidate = text.strip();
			if (!candidate.startsWith("@"))
			{
				return Optional.of(new TacticalLiteral(candidate));
			}
			int open = candidate.indexOf('(');
			if (open < 0 || !candidate.endsWith(")"))
			{
				errors.add(new TacticalParseError(lineNumber,
					"A reference is written '@kind(name)', which '" + candidate + "' is not"));
				return Optional.empty();
			}
			String kindName = candidate.substring(1, open).strip().toUpperCase(java.util.Locale.ROOT);
			String key = candidate.substring(open + 1, candidate.length() - 1).strip();
			for (ReferenceKind kind : ReferenceKind.values())
			{
				if (kind.name().equals(kindName))
				{
					return Optional.of(new TacticalReference(kind, key));
				}
			}
			errors.add(new TacticalParseError(lineNumber, "Unknown reference kind '" + kindName + "'"));
			return Optional.empty();
		}

		/**
		 * Consume the indented lines following the block just opened.
		 *
		 * @param blockName the block they belong to, to name it in an error
		 * @param keys      the sub keys that block accepts
		 * @return the sub lines that belonged to it, in reading order
		 */
		private List<SubLine> subLines(String blockName, List<String> keys)
		{
			List<SubLine> taken = new ArrayList<>();
			while (cursor < lines.size() && lines.get(cursor).startsWith(INDENT))
			{
				String line = lines.get(cursor).strip();
				int lineNumber = cursor + 1;
				cursor++;
				Optional<SubLine> matched = keys.stream()
					.flatMap(key -> valueOf(line, key).map(value -> new SubLine(key, value, lineNumber)).stream())
					.findFirst();
				if (matched.isPresent())
				{
					taken.add(matched.get());
				}
				else
				{
					errors.add(new TacticalParseError(lineNumber,
						"A " + blockName + " takes " + quoted(keys) + ", not " + keyOf(line)));
				}
			}
			return taken;
		}

		private void refuseSubLines(String blockName)
		{
			subLines(blockName, List.of());
		}

		private void skipSubLines()
		{
			while (cursor < lines.size() && lines.get(cursor).startsWith(INDENT))
			{
				cursor++;
			}
		}

		private Optional<TacticalBlock> block(java.util.function.Supplier<TacticalBlock> built, int lineNumber)
		{
			try
			{
				return Optional.of(built.get());
			}
			catch (IllegalArgumentException e)
			{
				errors.add(new TacticalParseError(lineNumber, e.getMessage()));
				return Optional.empty();
			}
		}

		private void closeSection()
		{
			if (sectionTitle == null)
			{
				return;
			}
			if (blocks.isEmpty())
			{
				if (!sectionHadBlockLine)
				{
					errors.add(new TacticalParseError(1, "Section '" + sectionTitle + "' holds no block"));
				}
			}
			else
			{
				sections.add(new TacticalSection(sectionTitle, List.copyOf(blocks)));
			}
			blocks = new ArrayList<>();
			sectionTitle = null;
			sectionHadBlockLine = false;
		}

		private TacticalParseResult result()
		{
			if (!errors.isEmpty())
			{
				errors.sort(java.util.Comparator.comparingInt(TacticalParseError::line));
				return new TacticalParseFailure(errors);
			}
			if (sections.isEmpty())
			{
				return new TacticalParseFailure(
					List.of(new TacticalParseError(1, "A plan holds at least one section")));
			}
			return new TacticalParseSuccess(new TacticalSheet(sections));
		}

		private static Optional<String> valueOf(String line, String key)
		{
			String prefix = key + ":";
			if (!line.startsWith(prefix))
			{
				return Optional.empty();
			}
			String rest = line.substring(prefix.length());
			return (rest.isEmpty() || rest.startsWith(" ")) ? Optional.of(unescape(rest.strip())) : Optional.empty();
		}

		/**
		 * A single line field cannot hold a real line break, so the two
		 * characters {@code \n} stand for one.
		 *
		 * @param value the field as written
		 * @return the field with its escapes turned back into line breaks
		 */
		private static String unescape(String value)
		{
			return value.replace(ESCAPED_NEWLINE, "\n");
		}

		private static List<String> fieldsOf(String value)
		{
			List<String> fields = new ArrayList<>();
			for (String field : value.split("\\" + FIELD_SEPARATOR, -1))
			{
				fields.add(field.strip());
			}
			return fields;
		}

		private static Optional<String> fieldAt(List<String> fields, int position)
		{
			return (position < fields.size()) ? Optional.of(fields.get(position)).filter(field -> !field.isEmpty())
				: Optional.empty();
		}

		private static String keyOf(String line)
		{
			int colon = line.indexOf(':');
			return "'" + ((colon < 0) ? line.strip() : line.substring(0, colon).strip()) + "'";
		}

		private static String quoted(List<String> keys)
		{
			return keys.isEmpty() ? "no indented line"
				: keys.stream().map(key -> "'" + key + ":'").collect(java.util.stream.Collectors.joining(" and "));
		}
	}

	/**
	 * An indented line that belonged to the block above it.
	 *
	 * @param key   the sub key it carried
	 * @param value everything after the key
	 * @param line  the one based line it sat on
	 */
	private record SubLine(String key, String value, int line)
	{
	}
}
