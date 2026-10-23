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
package pcgen.gui2.gametable;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileNameExtensionFilter;

import pcgen.core.gametable.PartyMember;
import pcgen.system.LanguageBundle;
import pcgen.system.PCGenSettings;

/**
 * The party: the players' characters, each with the saved character the game master
 * glances at, when there is one.
 */
final class PartyPanel extends JPanel
{
	/** The share of the height the party list takes; the loot takes the rest. */
	private static final double PARTY_SHARE = 0.25;

	private final GameTablePanel table;
	private final DefaultListModel<PartyMember> members = new DefaultListModel<>();
	private final JList<PartyMember> list = new JList<>(members);
	private final LootPanel lootPanel;

	PartyPanel(GameTablePanel table)
	{
		super(new BorderLayout());
		this.table = table;
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setCellRenderer((view, member, index, selected, focused) -> {
			String file = member.characterFile().map(path -> " — " + new File(path).getName()).orElse("");
			return new DefaultListCellRenderer().getListCellRendererComponent(view, member.name() + file,
				index, selected, focused);
		});
		JPanel members = new JPanel(new BorderLayout());
		members.add(new JScrollPane(list), BorderLayout.CENTER);
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
		buttons.add(GameTablePanel.button("in_gametable_addMember", this::addMember));
		buttons.add(GameTablePanel.button("in_gametable_addMemberFromFile", this::addMemberFromFile));
		buttons.add(GameTablePanel.button("in_gametable_linkCharacter", this::linkCharacter));
		buttons.add(GameTablePanel.button("in_gametable_removeMember", this::removeMember));
		members.add(buttons, BorderLayout.SOUTH);
		this.lootPanel = new LootPanel(table);
		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, members, lootPanel);
		split.setResizeWeight(PARTY_SHARE);
		add(split, BorderLayout.CENTER);
	}

	void refresh()
	{
		lootPanel.refresh();
		List<PartyMember> party = table.session().scenario().party();
		if (party.equals(current()))
		{
			return;
		}
		int selected = list.getSelectedIndex();
		members.clear();
		party.forEach(members::addElement);
		if (selected >= 0 && selected < members.size())
		{
			list.setSelectedIndex(selected);
		}
	}

	private List<PartyMember> current()
	{
		List<PartyMember> party = new ArrayList<>();
		for (int index = 0; index < members.size(); index++)
		{
			party.add(members.get(index));
		}
		return party;
	}

	private void addMember()
	{
		String name = JOptionPane.showInputDialog(this, LanguageBundle.getString("in_gametable_memberName"));
		if (name != null && !name.isBlank())
		{
			List<PartyMember> party = current();
			party.add(new PartyMember(name, Optional.empty()));
			table.session().setParty(party);
		}
	}

	private void addMemberFromFile()
	{
		chooseCharacter().ifPresent(file -> {
			String suggested = file.getName().replaceFirst("\\.pcg$", "");
			String name = JOptionPane.showInputDialog(this, LanguageBundle.getString("in_gametable_memberName"),
				suggested);
			if (name != null && !name.isBlank())
			{
				List<PartyMember> party = current();
				party.add(new PartyMember(name, Optional.of(file.getAbsolutePath())));
				table.session().setParty(party);
			}
		});
	}

	private void linkCharacter()
	{
		int selected = list.getSelectedIndex();
		if (selected < 0)
		{
			return;
		}
		chooseCharacter().ifPresent(file -> {
			List<PartyMember> party = current();
			party.set(selected, new PartyMember(party.get(selected).name(), Optional.of(file.getAbsolutePath())));
			table.session().setParty(party);
		});
	}

	private void removeMember()
	{
		int selected = list.getSelectedIndex();
		if (selected >= 0)
		{
			List<PartyMember> party = current();
			party.remove(selected);
			table.session().setParty(party);
		}
	}

	/**
	 * @param parent the component the chooser opens over.
	 * @return a saved character the game master picks.
	 */
	static Optional<File> chooseCharacter(Component parent)
	{
		JFileChooser chooser = new JFileChooser(PCGenSettings.getPcgDir());
		chooser.setFileFilter(new FileNameExtensionFilter(LanguageBundle.getString("in_gametable_characterFiles"),
			"pcg"));
		if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION)
		{
			return Optional.empty();
		}
		return Optional.of(chooser.getSelectedFile());
	}

	private Optional<File> chooseCharacter()
	{
		return chooseCharacter(this);
	}
}
