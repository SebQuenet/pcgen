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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * The blocks that let a sheet carry every capability a character has, the
 * items behind them, and the buffs that change the numbers.
 */
class TacticalCapabilitySyntaxTest
{

	private static TacticalBlock firstBlock(String source)
	{
		return switch (TacticalPlanParser.parse(source))
		{
			case TacticalParseSuccess success -> success.sheet().sections().getFirst().blocks().getFirst();
			case TacticalParseFailure failure -> fail("Expected a parse, got errors: " + failure.errors());
		};
	}

	private static void assertErrorOnLine(int expectedLine, String source)
	{
		List<TacticalParseError> errors = switch (TacticalPlanParser.parse(source))
		{
			case TacticalParseSuccess success -> fail("Expected errors, got a sheet");
			case TacticalParseFailure failure -> failure.errors();
		};

		assertTrue(errors.stream().anyMatch(error -> error.line() == expectedLine),
			"Expected an error on line " + expectedLine + ", got " + errors);
	}

	@Test
	void readsACapabilityListWithItsTags()
	{
		String source = """
			## Repertoire
			capabilities: Everything Miros can do
			  power: Touch of Good | buff, support | standard | 10/day | +3 sacred to attacks and saves
			  power: Channel positive energy | healing | standard | 4/day | 6d6, DC 14 Will for half
			""";

		assertEquals(new TacticalCapabilityList("Everything Miros can do",
			List.of(new TacticalCapability("Touch of Good", List.of("buff", "support"), "standard", "10/day",
				"+3 sacred to attacks and saves"),
				new TacticalCapability("Channel positive energy", List.of("healing"), "standard", "4/day",
					"6d6, DC 14 Will for half"))),
			firstBlock(source));
	}

	@Test
	void readsACapabilityWithoutActionUsesOrEffect()
	{
		String source = """
			## Repertoire
			capabilities: Short list
			  power: Hard to Kill | survival
			""";

		TacticalCapability only = ((TacticalCapabilityList) firstBlock(source)).capabilities().getFirst();

		assertEquals("", only.action());
		assertEquals("", only.uses());
		assertEquals("", only.effect());
	}

	@Test
	void collectsEveryTagACapabilityListUses()
	{
		String source = """
			## Repertoire
			capabilities: List
			  power: One | buff, support
			  power: Two | damage
			  power: Three | buff
			""";

		assertEquals(List.of("buff", "damage", "support"), ((TacticalCapabilityList) firstBlock(source)).tags());
	}

	@Test
	void refusesACapabilityListWithoutAPower()
	{
		assertErrorOnLine(2, """
			## Repertoire
			capabilities: Empty
			""");
	}

	@Test
	void refusesACapabilityWithoutATag()
	{
		assertErrorOnLine(3, """
			## Repertoire
			capabilities: List
			  power: Touch of Good
			""");
	}

	@Test
	void readsAnItemCard()
	{
		String source = """
			## Gear
			item: Sceptre of Timeon
			  row: Aura | moderate evocation, CL 10
			  row: Price | 104,000 gp
			""";

		assertEquals(new TacticalItem("Sceptre of Timeon", List.of(new TacticalRow("Aura", "moderate evocation, CL 10"),
			new TacticalRow("Price", "104,000 gp"))), firstBlock(source));
	}

	@Test
	void refusesAnItemWithoutARow()
	{
		assertErrorOnLine(2, """
			## Gear
			item: Sceptre of Timeon
			""");
	}

	@Test
	void readsABuffThatDeclaresItsDeltas()
	{
		String source = """
			## Buffs
			buff: Aura of holiness | 15 rounds
			  gives: ac +4 | will +4
			  note: only against evil creatures
			""";

		assertEquals(new TacticalBuff("Aura of holiness", "15 rounds",
			List.of(new TacticalDelta(DeltaTarget.AC, 4), new TacticalDelta(DeltaTarget.WILL, 4)), Optional.empty(),
			"only against evil creatures"), firstBlock(source));
	}

	@Test
	void readsABuffThatLeansOnATemporaryBonus()
	{
		String source = """
			## Buffs
			buff: Divine favour | 1 min
			  applies: @tempbonus(Divine Favor)
			""";

		assertEquals(new TacticalBuff("Divine favour", "1 min", List.of(),
			Optional.of(new TacticalReference(ReferenceKind.TEMPBONUS, "Divine Favor")), ""), firstBlock(source));
	}

	@Test
	void readsANegativeDelta()
	{
		String source = """
			## Buffs
			buff: Fatigued | until rested
			  gives: attack -2
			""";

		assertEquals(List.of(new TacticalDelta(DeltaTarget.ATTACK, -2)),
			((TacticalBuff) firstBlock(source)).gives());
	}

	@Test
	void refusesABuffThatChangesNothing()
	{
		assertErrorOnLine(2, """
			## Buffs
			buff: Does nothing | 1 min
			""");
	}

	@Test
	void refusesADeltaAgainstSomethingTheSheetCannotChange()
	{
		assertErrorOnLine(3, """
			## Buffs
			buff: Odd | 1 min
			  gives: charisma +2
			""");
	}

	@Test
	void refusesADeltaWithoutANumber()
	{
		assertErrorOnLine(3, """
			## Buffs
			buff: Odd | 1 min
			  gives: ac
			""");
	}
}
