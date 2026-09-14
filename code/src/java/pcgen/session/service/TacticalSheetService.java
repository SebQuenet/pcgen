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
package pcgen.session.service;

import java.util.ArrayList;
import java.util.List;

import pcgen.cdom.base.Constants;
import pcgen.core.Equipment;
import pcgen.core.PlayerCharacter;
import pcgen.core.tactics.SpellSource;
import pcgen.core.tactics.TacticalParseError;
import pcgen.core.tactics.TacticalParseFailure;
import pcgen.core.tactics.TacticalParseSuccess;
import pcgen.core.tactics.TacticalPlanParser;
import pcgen.core.tactics.TacticalSessionState;
import pcgen.io.tactics.ResolvedSpell;
import pcgen.io.tactics.TacticalResolver;
import pcgen.session.PcgenSession;
import pcgen.session.service.model.SpellReference;
import pcgen.session.service.model.TacticalReferences;
import pcgen.session.service.model.TacticalSheet;
import pcgen.session.service.model.TacticalSheetStored;
import pcgen.session.service.model.WeaponReference;

/**
 * The tactical sheet: what a character should do, round by round and situation
 * by situation.
 *
 * <p>
 * The plan is source text in the syntax the Tactical tab shows, so what is
 * written here is what a player reads and corrects there. Text that does not
 * read is refused with the offending line named, rather than half stored.
 */
public final class TacticalSheetService
{
	private final CharacterLookup characters;

	public TacticalSheetService(PcgenSession session)
	{
		this.characters = new CharacterLookup(session);
	}

	public ServiceResult<TacticalSheetStored> setTacticalSheet(String characterId, String source)
	{
		return characters.playerCharacterById(characterId).andThen(character -> {
			if (source == null || source.isBlank())
			{
				return ServiceResult.failure(new ServiceError.InvalidArgument("source",
					"a tactical sheet needs text; use clear_tactical_sheet to remove one"));
			}
			return switch (TacticalPlanParser.parse(source))
			{
				case TacticalParseSuccess read -> {
					character.setTacticalPlan(source);
					yield ServiceResult.success(new TacticalSheetStored(
						read.sheet().sections().size(),
						read.sheet().sections().stream().mapToInt(section -> section.blocks().size()).sum()));
				}
				case TacticalParseFailure refused -> ServiceResult.failure(
					new ServiceError.InvalidArgument("source", "the plan does not read:\n" + listed(refused)));
			};
		});
	}

	public ServiceResult<TacticalSheet> getTacticalSheet(String characterId)
	{
		return characters.playerCharacterById(characterId).map(character -> {
			TacticalSessionState played = character.getDisplay().getTacticalSession();
			return new TacticalSheet(character.getDisplay().getTacticalPlan().orElse(""),
				played.damageTaken(), played.resourcesSpent());
		});
	}

	public ServiceResult<TacticalSheetStored> clearTacticalSheet(String characterId)
	{
		return characters.playerCharacterById(characterId).map(character -> {
			character.clearTacticalSheet();
			return new TacticalSheetStored(0, 0);
		});
	}

	public ServiceResult<TacticalReferences> listTacticalReferences(String characterId)
	{
		return characters.playerCharacterById(characterId).map(character -> new TacticalReferences(
			weaponsOf(character),
			character.getVariableNames().stream().sorted().toList(),
			spellsOf(character, SpellSource.PREPARED),
			spellsOf(character, SpellSource.KNOWN)));
	}

	private static List<WeaponReference> weaponsOf(PlayerCharacter character)
	{
		TacticalResolver resolver = new TacticalResolver(character);
		List<WeaponReference> weapons = new ArrayList<>();
		for (Equipment weapon : character.getExpandedWeapons(Constants.MERGE_ALL))
		{
			resolver.weapon(weapon.getName()).ifPresent(resolved -> weapons.add(new WeaponReference(
				resolved.name(), resolved.toHit(), resolved.damage(), resolved.critical())));
		}
		return List.copyOf(weapons);
	}

	private static List<SpellReference> spellsOf(PlayerCharacter character, SpellSource source)
	{
		List<SpellReference> spells = new ArrayList<>();
		for (ResolvedSpell spell : new TacticalResolver(character).spells(source))
		{
			spells.add(new SpellReference(spell.name(), spell.level(), spell.times()));
		}
		return List.copyOf(spells);
	}

	private static String listed(TacticalParseFailure refused)
	{
		StringBuilder lines = new StringBuilder();
		for (TacticalParseError error : refused.errors())
		{
			lines.append("line ").append(error.line()).append(": ").append(error.message()).append('\n');
		}
		return lines.toString();
	}
}
