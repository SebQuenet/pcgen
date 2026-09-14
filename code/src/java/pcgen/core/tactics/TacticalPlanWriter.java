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

import java.util.List;

/**
 * Writes a tactical sheet back out as source text.
 *
 * <p>
 * The source text is what a character stores, so nothing writes the model back
 * during ordinary editing. This writer exists to migrate characters saved under
 * the tags that came before the syntax, and what it writes reads back
 * identically.
 */
public final class TacticalPlanWriter
{
	private TacticalPlanWriter()
	{
	}

	/**
	 * @param sheet the sheet to write out
	 * @return source text that reads back as the same sheet
	 */
	public static String write(TacticalSheet sheet)
	{
		StringBuilder out = new StringBuilder();
		for (TacticalSection section : sheet.sections())
		{
			if (!out.isEmpty())
			{
				out.append('\n');
			}
			out.append("## ").append(escape(section.title())).append('\n');
			for (TacticalBlock block : section.blocks())
			{
				writeBlock(out, block);
			}
		}
		return out.toString();
	}

	private static void writeBlock(StringBuilder out, TacticalBlock block)
	{
		switch (block)
		{
			case TacticalStep step -> writeStep(out, step);
			case TacticalNote note -> writeNote(out, note);
			case TacticalResource resource -> writeResource(out, resource);
			case TacticalAttack attack -> writeAttack(out, attack);
			case TacticalCreature creature -> writeCreature(out, creature);
			case TacticalSpellList spells -> writeSpells(out, spells);
			case TacticalCapabilityList capabilities -> writeCapabilities(out, capabilities);
			case TacticalItem item -> writeItem(out, item);
			case TacticalBuff buff -> writeBuff(out, buff);
		}
	}

	private static void writeStep(StringBuilder out, TacticalStep step)
	{
		out.append("step: ").append(escape(step.trigger())).append('\n');
		out.append("  do: ").append(escape(step.actions())).append('\n');
		appendNote(out, step.note());
	}

	private static void writeNote(StringBuilder out, TacticalNote note)
	{
		out.append("note: ").append(escape(note.title())).append('\n');
		for (String line : note.prose().split("\n", -1))
		{
			out.append("  ").append(line).append('\n');
		}
	}

	private static void writeResource(StringBuilder out, TacticalResource resource)
	{
		out.append("resource: ").append(escape(resource.label())).append(" | ").append(subject(resource.maximum()));
		if (!resource.action().isEmpty())
		{
			out.append(" | ").append(escape(resource.action()));
		}
		out.append('\n');
	}

	private static void writeAttack(StringBuilder out, TacticalAttack attack)
	{
		out.append("attack: ").append(subject(attack.subject()));
		if (attack.toHit().isPresent() || attack.damage().isPresent() || attack.critical().isPresent())
		{
			out.append(" | ").append(escape(attack.toHit().orElse("")));
			out.append(" | ").append(escape(attack.damage().orElse("")));
			out.append(" | ").append(escape(attack.critical().orElse("")));
		}
		out.append('\n');
		for (TacticalVariant variant : attack.variants())
		{
			out.append("  target: ").append(escape(variant.label())).append(" | ").append(escape(variant.effect()))
				.append('\n');
		}
		appendNote(out, attack.note());
	}

	private static void writeCreature(StringBuilder out, TacticalCreature creature)
	{
		out.append("creature: ").append(escape(creature.name()));
		if (!creature.source().isEmpty() || !creature.duration().isEmpty())
		{
			out.append(" | ").append(escape(creature.source()));
			out.append(" | ").append(escape(creature.duration()));
		}
		out.append('\n');
		for (TacticalRow row : creature.rows())
		{
			out.append("  row: ").append(escape(row.label())).append(" | ").append(escape(row.content())).append('\n');
		}
	}

	private static void writeSpells(StringBuilder out, TacticalSpellList spells)
	{
		out.append("spells: @").append(spells.source().name().toLowerCase(java.util.Locale.ROOT)).append('\n');
		for (TacticalTag tag : spells.tags())
		{
			out.append("  tag: ").append(escape(tag.spellName())).append(" | ")
				.append(escape(String.join(", ", tag.tags()))).append('\n');
		}
	}

	private static void writeCapabilities(StringBuilder out, TacticalCapabilityList list)
	{
		out.append("capabilities: ").append(escape(list.title())).append('\n');
		for (TacticalCapability capability : list.capabilities())
		{
			out.append("  power: ").append(escape(capability.name()));
			out.append(" | ").append(escape(String.join(", ", capability.tags())));
			if (!capability.action().isEmpty() || !capability.uses().isEmpty() || !capability.effect().isEmpty())
			{
				out.append(" | ").append(escape(capability.action()));
				out.append(" | ").append(escape(capability.uses()));
				out.append(" | ").append(escape(capability.effect()));
			}
			out.append('\n');
		}
	}

	private static void writeItem(StringBuilder out, TacticalItem item)
	{
		out.append("item: ").append(escape(item.name())).append('\n');
		for (TacticalRow row : item.rows())
		{
			out.append("  row: ").append(escape(row.label())).append(" | ").append(escape(row.content())).append('\n');
		}
	}

	private static void writeBuff(StringBuilder out, TacticalBuff buff)
	{
		out.append("buff: ").append(escape(buff.label()));
		if (!buff.duration().isEmpty())
		{
			out.append(" | ").append(escape(buff.duration()));
		}
		out.append('\n');
		if (!buff.gives().isEmpty())
		{
			out.append("  gives: ");
			for (int index = 0; index < buff.gives().size(); index++)
			{
				TacticalDelta delta = buff.gives().get(index);
				out.append((index == 0) ? "" : " | ");
				out.append(delta.target().name().toLowerCase(java.util.Locale.ROOT)).append(' ')
					.append(delta.signed());
			}
			out.append('\n');
		}
		buff.applies().ifPresent(reference -> out.append("  applies: ").append(subject(reference)).append('\n'));
		appendNote(out, buff.note());
	}

	private static void appendNote(StringBuilder out, String note)
	{
		if (!note.isEmpty())
		{
			out.append("  note: ").append(escape(note)).append('\n');
		}
	}

	/**
	 * A single line field cannot hold a real line break, so one is written as
	 * the two characters {@code \n}, which the parser reads back.
	 *
	 * @param value the text to write on one line
	 * @return the text with its line breaks escaped
	 */
	private static String escape(String value)
	{
		return value.replace("\n", "\\n");
	}

	private static String subject(TacticalSubject subject)
	{
		return switch (subject)
		{
			case TacticalReference reference ->
				"@" + reference.kind().name().toLowerCase(java.util.Locale.ROOT) + "(" + reference.key() + ")";
			case TacticalLiteral literal -> literal.text();
		};
	}

	/**
	 * @param sections the sections to write out
	 * @return source text for them, empty when there are none
	 */
	public static String writeSections(List<TacticalSection> sections)
	{
		return sections.isEmpty() ? "" : write(new TacticalSheet(sections));
	}
}
