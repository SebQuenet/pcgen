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
package pcgen.gui2.facade;

import java.util.ArrayList;
import java.util.List;

import pcgen.core.PlayerCharacter;
import pcgen.core.tactics.TacticalEntry;
import pcgen.core.tactics.TacticalSection;
import pcgen.core.tactics.TacticalSheet;
import pcgen.facade.core.TacticalSheetFacade;
import pcgen.facade.util.DefaultListFacade;
import pcgen.facade.util.ListFacade;
import pcgen.system.LanguageBundle;

/**
 * Edits the tactical sheet of a character on behalf of the Tactical tab.
 *
 * <p>
 * The sections the tab shows are kept in a list facade so the tab is told when
 * they change; the character is the source of truth and is rewritten after each
 * edit.
 */
class TacticalSheetFacadeImpl implements TacticalSheetFacade
{

	private final PlayerCharacter theCharacter;
	private final DefaultListFacade<TacticalSection> sections;

	TacticalSheetFacadeImpl(PlayerCharacter pc)
	{
		theCharacter = pc;
		sections = new DefaultListFacade<>();
		pc.getDisplay().getTacticalSheet()
			.ifPresent(sheet -> sheet.sections().forEach(sections::addElement));
	}

	@Override
	public ListFacade<TacticalSection> getSections()
	{
		return sections;
	}

	@Override
	public void addSection(String title)
	{
		sections.addElement(new TacticalSection(title, List.of(placeholderEntry())));
		storeOnCharacter();
	}

	@Override
	public void replaceSection(int position, TacticalSection replacement)
	{
		if (isOutsideTheSheet(position))
		{
			return;
		}
		sections.removeElement(position);
		sections.addElement(position, replacement);
		storeOnCharacter();
	}

	@Override
	public void removeSection(int position)
	{
		if (isOutsideTheSheet(position))
		{
			return;
		}
		sections.removeElement(position);
		storeOnCharacter();
	}

	private boolean isOutsideTheSheet(int position)
	{
		return position < 0 || position >= sections.getSize();
	}

	/**
	 * The line a brand new section starts with, since a section is never empty.
	 *
	 * @return a line whose text tells the user what to replace it with.
	 */
	private static TacticalEntry placeholderEntry()
	{
		return new TacticalEntry(LanguageBundle.getString("in_tactical_when"),
			LanguageBundle.getString("in_tactical_do"), "");
	}

	private void storeOnCharacter()
	{
		List<TacticalSection> current = new ArrayList<>(sections.getContents());
		if (current.isEmpty())
		{
			theCharacter.clearTacticalSheet();
		}
		else
		{
			theCharacter.setTacticalSheet(new TacticalSheet(current));
		}
	}
}
