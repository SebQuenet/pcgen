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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import pcgen.cdom.enumeration.Handed;
import pcgen.cdom.enumeration.PCStringKey;
import pcgen.core.NoteItem;
import pcgen.facade.core.CharacterFacade;
import pcgen.facade.core.DescriptionFacade;
import pcgen.session.PcgenSession;
import pcgen.session.service.model.Biography;
import pcgen.session.service.model.BiographySet;
import pcgen.session.service.model.ExperienceSet;

/**
 * Who a character is, rather than what they can do.
 *
 * <p>
 * The plain facts are fields on the character; the longer notes are entries
 * PCGen keeps in a list, each marked with the key that says which note it is.
 */
public final class BiographyService
{
	private static final Map<String, PCStringKey> NOTE_FIELDS = Map.of(
		"bio", PCStringKey.BIO,
		"description", PCStringKey.DESCRIPTION,
		"companions", PCStringKey.COMPANIONS,
		"assets", PCStringKey.ASSETS,
		"magic", PCStringKey.MAGIC,
		"gm_notes", PCStringKey.GMNOTES);

	private final CharacterLookup characters;

	public BiographyService(PcgenSession session)
	{
		this.characters = new CharacterLookup(session);
	}

	/**
	 * Set whichever fields the request carries, leaving the rest alone.
	 *
	 * @param changes the fields to change, keyed as the schema names them
	 */
	public ServiceResult<BiographySet> setBiography(String characterId, Map<String, Object> changes)
	{
		return characters.byId(characterId).map(character -> {
			List<String> changed = new ArrayList<>();
			setPlainFields(character, changes, changed);
			setNotes(character, changes, changed);
			return new BiographySet(List.copyOf(changed));
		});
	}

	public ServiceResult<Biography> getBiography(String characterId)
	{
		return characters.byId(characterId).map(character -> {
			Map<String, String> notes = notesOf(character);
			return new Biography(
				character.getNameRef().get(),
				character.getPlayersNameRef().get(),
				textOf(character.getGenderRef().get()),
				character.getAgeRef().get(),
				character.getAgeCategoryRef().get(),
				character.getWeightRef().get(),
				character.getHairColorRef().get(),
				character.getEyeColorRef().get(),
				character.getSkinColorRef().get(),
				textOf(character.getHandedRef().get()),
				notes.get("bio"),
				notes.get("description"),
				notes.get("companions"),
				notes.get("assets"),
				notes.get("magic"),
				notes.get("gm_notes"));
		});
	}

	public ServiceResult<ExperienceSet> setExperience(String characterId, int xp)
	{
		return characters.byId(characterId).map(character -> {
			character.setXP(xp);
			return new ExperienceSet(character.getXPRef().get(), character.getXPForNextLevelRef().get());
		});
	}

	private static void setPlainFields(CharacterFacade character, Map<String, Object> changes, List<String> changed)
	{
		applyText(changes, "gender", character::setGender, changed);
		applyText(changes, "hair_color", character::setHairColor, changed);
		applyText(changes, "eye_color", character::setEyeColor, changed);
		applyText(changes, "skin_color", character::setSkinColor, changed);
		applyText(changes, "players_name", character::setPlayersName, changed);
		applyNumber(changes, "age", character::setAge, changed);
		applyNumber(changes, "weight", character::setWeight, changed);

		if (changes.get("handed") instanceof String wanted)
		{
			for (Handed hand : character.getAvailableHands())
			{
				if (hand.name().equalsIgnoreCase(wanted) || hand.toString().equalsIgnoreCase(wanted))
				{
					character.setHanded(hand);
					changed.add("handed");
					break;
				}
			}
		}
	}

	private static void setNotes(CharacterFacade character, Map<String, Object> changes, List<String> changed)
	{
		DescriptionFacade descriptions = character.getDescriptionFacade();
		for (Map.Entry<String, PCStringKey> field : NOTE_FIELDS.entrySet())
		{
			if (!(changes.get(field.getKey()) instanceof String text))
			{
				continue;
			}
			for (NoteItem note : descriptions.getNotes())
			{
				if (note.getPCStringKey().isPresent() && note.getPCStringKey().get() == field.getValue())
				{
					descriptions.setNote(note, text);
					changed.add(field.getKey());
					break;
				}
			}
		}
	}

	private static Map<String, String> notesOf(CharacterFacade character)
	{
		Map<PCStringKey, String> fieldOfKey = new LinkedHashMap<>();
		for (Map.Entry<String, PCStringKey> field : NOTE_FIELDS.entrySet())
		{
			fieldOfKey.put(field.getValue(), field.getKey());
		}
		Map<String, String> notes = new LinkedHashMap<>();
		for (NoteItem note : character.getDescriptionFacade().getNotes())
		{
			if (note.getPCStringKey().isPresent())
			{
				String field = fieldOfKey.get(note.getPCStringKey().get());
				if (field != null)
				{
					notes.put(field, note.getValue());
				}
			}
		}
		return notes;
	}

	private static void applyText(Map<String, Object> changes, String field, Consumer<String> set,
		List<String> changed)
	{
		if (changes.get(field) instanceof String text)
		{
			set.accept(text);
			changed.add(field);
		}
	}

	private static void applyNumber(Map<String, Object> changes, String field,
		IntConsumer set, List<String> changed)
	{
		if (changes.get(field) instanceof Number number)
		{
			set.accept(number.intValue());
			changed.add(field);
		}
	}

	private static String textOf(Object value)
	{
		return value == null ? null : value.toString();
	}
}
