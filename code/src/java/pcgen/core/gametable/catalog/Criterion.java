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

import java.text.Normalizer;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * What the game master asks of a bestiary entry: one test on a field, or a group of criteria.
 */
public sealed interface Criterion
{
	/**
	 * @param facts the entry's values.
	 * @return whether the entry meets this criterion; unknown while a value it needs is not known.
	 */
	Match evaluate(EntryFacts facts);

	/**
	 * @param field    the field tested.
	 * @param operator how its value is compared.
	 * @param operands what it is compared with; for scores, the score's name first.
	 */
	record Test(CatalogField field, Operator operator, List<String> operands) implements Criterion
	{
		public Test
		{
			Objects.requireNonNull(field);
			Objects.requireNonNull(operator);
			operands = List.copyOf(operands);
		}

		@Override
		public Match evaluate(EntryFacts facts)
		{
			return facts.value(field).map(value -> Match.of(meets(value))).orElse(Match.UNKNOWN);
		}

		private boolean meets(FieldValue value)
		{
			return switch (value)
			{
				case FieldValue.Number number -> meetsNumber(number.value(), operands);
				case FieldValue.Text text -> meetsText(text.value());
				case FieldValue.Items items -> meetsItems(items.values());
				case FieldValue.Scores scores -> meetsScores(scores.values());
				case FieldValue.Flag flag -> operator == (flag.value() ? Operator.IS_TRUE : Operator.IS_FALSE);
			};
		}

		private boolean meetsNumber(double value, List<String> bounds)
		{
			List<OptionalDouble> numbers = bounds.stream().map(Test::number).toList();
			if (numbers.isEmpty() || numbers.stream().anyMatch(OptionalDouble::isEmpty))
			{
				return false;
			}
			double first = numbers.getFirst().getAsDouble();
			return switch (operator)
			{
				case EQUALS -> value == first;
				case NOT_EQUALS -> value != first;
				case AT_LEAST -> value >= first;
				case AT_MOST -> value <= first;
				case BETWEEN -> numbers.size() == 2 && value >= first && value <= numbers.get(1).getAsDouble();
				default -> false;
			};
		}

		private boolean meetsText(String value)
		{
			String text = folded(value);
			Set<String> wanted = folded(operands);
			return switch (operator)
			{
				case EQUALS, ANY_OF -> wanted.contains(text);
				case NOT_EQUALS, NONE_OF -> !wanted.contains(text);
				case CONTAINS -> wanted.stream().anyMatch(text::contains);
				case STARTS_WITH -> wanted.stream().anyMatch(text::startsWith);
				case MATCHES -> operands.stream().anyMatch(pattern -> matches(pattern, value));
				default -> false;
			};
		}

		private boolean meetsItems(Collection<String> values)
		{
			Set<String> held = folded(values);
			Set<String> wanted = folded(operands);
			return switch (operator)
			{
				case ANY_OF -> wanted.stream().anyMatch(held::contains);
				case ALL_OF -> held.containsAll(wanted);
				case NONE_OF -> wanted.stream().noneMatch(held::contains);
				default -> false;
			};
		}

		private boolean meetsScores(Map<String, Integer> scores)
		{
			Map<String, Integer> byName = scores.entrySet().stream()
				.collect(Collectors.toMap(entry -> folded(entry.getKey()), Map.Entry::getValue, Math::max));
			if (operator == Operator.ANY_OF || operator == Operator.ALL_OF || operator == Operator.NONE_OF)
			{
				return meetsItems(byName.keySet());
			}
			if (operands.isEmpty())
			{
				return false;
			}
			Integer score = byName.get(folded(operands.getFirst()));
			return score != null && meetsNumber(score, operands.subList(1, operands.size()));
		}

		private static boolean matches(String pattern, String value)
		{
			try
			{
				return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(value).find();
			}
			catch (PatternSyntaxException invalid)
			{
				return false;
			}
		}

		/**
		 * @param text a number as the game master types it: 7, -2, 2.5 or a fraction such as 1/3.
		 * @return its value; empty when it is not a number.
		 */
		static OptionalDouble number(String text)
		{
			String trimmed = text.strip().replace(',', '.');
			try
			{
				int slash = trimmed.indexOf('/');
				if (slash > 0)
				{
					return OptionalDouble.of(Double.parseDouble(trimmed.substring(0, slash))
						/ Double.parseDouble(trimmed.substring(slash + 1)));
				}
				return OptionalDouble.of(Double.parseDouble(trimmed));
			}
			catch (NumberFormatException notANumber)
			{
				return OptionalDouble.empty();
			}
		}
	}

	/**
	 * @param joiner  how the members combine.
	 * @param members the criteria grouped; an empty group is met by every entry.
	 */
	record Group(Joiner joiner, List<Criterion> members) implements Criterion
	{
		public Group
		{
			Objects.requireNonNull(joiner);
			members = List.copyOf(members);
		}

		@Override
		public Match evaluate(EntryFacts facts)
		{
			if (members.isEmpty())
			{
				return Match.YES;
			}
			Optional<Match> combined = members.stream().map(member -> member.evaluate(facts))
				.reduce(joiner == Joiner.ALL ? Match::and : Match::or);
			return combined.orElse(Match.YES);
		}
	}

	/**
	 * @param text a text.
	 * @return the text in lower case, without accents, to compare as the game master reads it.
	 */
	static String folded(String text)
	{
		return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).strip();
	}

	private static Set<String> folded(Collection<String> texts)
	{
		return texts.stream().map(Criterion::folded).collect(Collectors.toSet());
	}

}
