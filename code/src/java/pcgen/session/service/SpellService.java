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

import pcgen.facade.core.SpellFacade;
import pcgen.facade.core.SpellSupportFacade;
import pcgen.facade.core.SpellSupportFacade.SpellNode;
import pcgen.facade.core.SpellSupportFacade.SuperNode;
import pcgen.facade.util.ReferenceFacade;
import pcgen.session.PcgenSession;
import pcgen.session.service.model.BatchOutcome;
import pcgen.session.service.model.SpellEntry;
import pcgen.session.service.model.SpellForgotten;
import pcgen.session.service.model.SpellLearned;
import pcgen.session.service.model.SpellPrepared;
import pcgen.session.service.model.SpellWritten;
import pcgen.session.service.model.Spellbooks;

/**
 * The spells a character can learn, knows, has prepared, or has written down.
 *
 * <p>
 * Headless, PCGen does not refresh a caster's available spells on its own the
 * way the GUI does when a tab is opened, so anything that reads or searches the
 * available list asks for a refresh first.
 */
public final class SpellService
{
	private static final String FALLBACK_SPELL_LIST = "Prepared Spells";

	private final CharacterLookup characters;

	public SpellService(PcgenSession session)
	{
		this.characters = new CharacterLookup(session);
	}

	public ServiceResult<List<SpellEntry>> getAvailableSpells(String characterId, String classFilter,
		String levelFilter)
	{
		return characters.byId(characterId).map(character -> {
			SpellSupportFacade spells = character.getSpellSupport();
			spells.refreshAvailableKnownSpells();
			List<SpellEntry> available = new ArrayList<>();
			for (SpellNode node : spellNodesIn(spells.getAvailableSpellNodes()))
			{
				if (matchesClass(node, classFilter) && matchesLevel(node, levelFilter))
				{
					available.add(entryOf(node, false, false));
				}
			}
			return List.copyOf(available);
		});
	}

	public ServiceResult<List<SpellEntry>> getKnownSpells(String characterId)
	{
		return characters.byId(characterId).map(character -> {
			List<SpellEntry> known = new ArrayList<>();
			for (SpellNode node : spellNodesIn(character.getSpellSupport().getAllKnownSpellNodes()))
			{
				known.add(entryOf(node, false, false));
			}
			return List.copyOf(known);
		});
	}

	public ServiceResult<List<SpellEntry>> getPreparedSpells(String characterId)
	{
		return characters.byId(characterId).map(character -> {
			List<SpellEntry> prepared = new ArrayList<>();
			for (SpellNode node : spellNodesIn(character.getSpellSupport().getPreparedSpellNodes()))
			{
				prepared.add(entryOf(node, true, true));
			}
			return List.copyOf(prepared);
		});
	}

	public ServiceResult<SpellLearned> addKnownSpell(String characterId, String spellName, String classKey,
		String spellLevel)
	{
		return characters.byId(characterId).andThen(character -> {
			SpellSupportFacade spells = character.getSpellSupport();
			SpellNode node = findAvailable(spells, spellName, classKey, spellLevel);
			if (node == null)
			{
				return ServiceResult.failure(
					new ServiceError.EntryNotFound("Spell in available spells", spellName));
			}
			spells.addKnownSpell(node);
			return ServiceResult.success(new SpellLearned(
				node.getSpell().toString(), classNameOf(node), node.getSpellLevel()));
		});
	}

	public ServiceResult<SpellForgotten> removeKnownSpell(String characterId, String spellName)
	{
		return characters.byId(characterId).andThen(character -> {
			SpellSupportFacade spells = character.getSpellSupport();
			SpellNode node = findNamed(spells.getAllKnownSpellNodes(), spellName);
			if (node == null)
			{
				return ServiceResult.failure(new ServiceError.EntryNotFound("Spell in known spells", spellName));
			}
			spells.removeKnownSpell(node);
			return ServiceResult.success(new SpellForgotten(node.getSpell().toString()));
		});
	}

	public ServiceResult<SpellPrepared> addPreparedSpell(String characterId, String spellName, String spellList,
		String classKey)
	{
		return characters.byId(characterId).andThen(character -> {
			SpellSupportFacade spells = character.getSpellSupport();
			SpellNode node = findKnownOrAvailable(spells, spellName, classKey);
			if (node == null)
			{
				return ServiceResult.failure(new ServiceError.EntryNotFound("Spell", spellName));
			}
			String list = listToPrepareOn(spells, spellList);
			spells.addPreparedSpell(node, list, false);
			return ServiceResult.success(new SpellPrepared(node.getSpell().toString(), list));
		});
	}

	public ServiceResult<SpellForgotten> removePreparedSpell(String characterId, String spellName, String spellList)
	{
		return characters.byId(characterId).andThen(character -> {
			SpellSupportFacade spells = character.getSpellSupport();
			SpellNode node = findNamed(spells.getPreparedSpellNodes(), spellName);
			if (node == null)
			{
				return ServiceResult.failure(new ServiceError.EntryNotFound("Spell in prepared list", spellName));
			}
			String list = spellList == null || spellList.isBlank() ? rootNameOf(node) : spellList;
			spells.removePreparedSpell(node, list);
			return ServiceResult.success(new SpellForgotten(node.getSpell().toString()));
		});
	}

	public ServiceResult<Spellbooks> getSpellbooks(String characterId)
	{
		return characters.byId(characterId).map(character -> {
			SpellSupportFacade spells = character.getSpellSupport();
			List<String> books = new ArrayList<>();
			for (String book : spells.getSpellbooks())
			{
				books.add(book);
			}
			List<SpellEntry> written = new ArrayList<>();
			for (SpellNode node : spellNodesIn(spells.getBookSpellNodes()))
			{
				written.add(entryOf(node, false, true));
			}
			return new Spellbooks(List.copyOf(books), List.copyOf(written));
		});
	}

	public ServiceResult<SpellWritten> addToSpellbook(String characterId, String spellName, String spellbook)
	{
		return characters.byId(characterId).andThen(character -> {
			SpellSupportFacade spells = character.getSpellSupport();
			SpellNode node = findNamed(spells.getAllKnownSpellNodes(), spellName);
			if (node == null)
			{
				return ServiceResult.failure(new ServiceError.EntryNotFound("Spell in known spells", spellName));
			}
			spells.addToSpellBook(node, spellbook);
			return ServiceResult.success(new SpellWritten(node.getSpell().toString(), spellbook));
		});
	}

	/** Prepare several spells at once; one that cannot be prepared does not stop the rest. */
	public ServiceResult<BatchOutcome> batchAddPreparedSpells(String characterId, List<PreparedSpellRequest> requests)
	{
		return characters.byId(characterId).map(character -> {
			SpellSupportFacade spells = character.getSpellSupport();
			List<String> errors = new ArrayList<>();
			int accepted = 0;

			for (PreparedSpellRequest request : requests)
			{
				SpellNode node = findKnownOrAvailable(spells, request.spellName(), request.classKey());
				if (node == null)
				{
					errors.add("Spell not found: " + request.spellName());
					continue;
				}
				try
				{
					spells.addPreparedSpell(node, listToPrepareOn(spells, request.spellList()), false);
					accepted++;
				}
				catch (RuntimeException e)
				{
					errors.add("Failed: " + request.spellName() + " - " + e.getMessage());
				}
			}
			return new BatchOutcome(accepted, requests.size(), List.copyOf(errors));
		});
	}

	/** One line of a batch: which spell, cast as which class, onto which list. */
	public record PreparedSpellRequest(String spellName, String classKey, String spellList)
	{
	}

	private static SpellNode findKnownOrAvailable(SpellSupportFacade spells, String spellName, String classKey)
	{
		SpellNode known = findNamed(spells.getAllKnownSpellNodes(), spellName);
		return known != null ? known : findAvailable(spells, spellName, classKey, null);
	}

	private static SpellNode findAvailable(SpellSupportFacade spells, String name, String classKey, String level)
	{
		spells.refreshAvailableKnownSpells();
		for (SpellNode node : spellNodesIn(spells.getAvailableSpellNodes()))
		{
			if (isNamed(node, name) && matchesClass(node, classKey) && matchesLevel(node, level))
			{
				return node;
			}
		}
		return null;
	}

	private static SpellNode findNamed(Iterable<? extends SuperNode> nodes, String name)
	{
		for (SpellNode node : spellNodesIn(nodes))
		{
			if (isNamed(node, name))
			{
				return node;
			}
		}
		return null;
	}

	private static boolean isNamed(SpellNode node, String name)
	{
		return node.getSpell().toString().equalsIgnoreCase(name)
			|| node.getSpell().getKeyName().equalsIgnoreCase(name);
	}

	private static boolean matchesClass(SpellNode node, String classKey)
	{
		if (classKey == null || node.getSpellcastingClass() == null)
		{
			return true;
		}
		return node.getSpellcastingClass().getKeyName().equalsIgnoreCase(classKey)
			|| node.getSpellcastingClass().getDisplayName().equalsIgnoreCase(classKey);
	}

	private static boolean matchesLevel(SpellNode node, String level)
	{
		return level == null || level.equals(node.getSpellLevel());
	}

	/**
	 * The list a spell should be prepared on, created if the character has none by
	 * that name yet.
	 */
	private static String listToPrepareOn(SpellSupportFacade spells, String wanted)
	{
		String list = wanted;
		if (list == null || list.isBlank())
		{
			ReferenceFacade<String> defaultBook = spells.getDefaultSpellBookRef();
			String named = defaultBook == null ? null : defaultBook.get();
			list = named == null || named.isEmpty() ? FALLBACK_SPELL_LIST : named;
		}
		for (SpellNode node : spellNodesIn(spells.getPreparedSpellNodes()))
		{
			if (node.getRootNode() != null && list.equals(node.getRootNode().getName()))
			{
				return list;
			}
		}
		spells.addSpellList(list);
		return list;
	}

	private static List<SpellNode> spellNodesIn(Iterable<? extends SuperNode> nodes)
	{
		List<SpellNode> spells = new ArrayList<>();
		for (SuperNode node : nodes)
		{
			if (node instanceof SpellNode spell && spell.getSpell() != null)
			{
				spells.add(spell);
			}
		}
		return spells;
	}

	private static SpellEntry entryOf(SpellNode node, boolean withCount, boolean withList)
	{
		SpellFacade spell = node.getSpell();
		return new SpellEntry(
			spell.getKeyName(),
			spell.toString(),
			node.getSpellLevel(),
			classNameOf(node),
			spell.getSchool(),
			spell.getSubschool(),
			spell.getComponents(),
			spell.getRange(),
			spell.getDuration(),
			spell.getCastTime(),
			withCount ? node.getCount() : null,
			withList ? rootNameOf(node) : null);
	}

	private static String classNameOf(SpellNode node)
	{
		return node.getSpellcastingClass() == null ? null : node.getSpellcastingClass().getDisplayName();
	}

	private static String rootNameOf(SpellNode node)
	{
		return node.getRootNode() == null ? FALLBACK_SPELL_LIST : node.getRootNode().getName();
	}
}
