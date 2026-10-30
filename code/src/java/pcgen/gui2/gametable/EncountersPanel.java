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
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import pcgen.core.gametable.BestiaryPawn;
import pcgen.core.gametable.Encounter;
import pcgen.core.gametable.EncounterMonster;
import pcgen.core.gametable.Experience;
import pcgen.core.gametable.ForgeOutcome;
import pcgen.core.gametable.ImageCategory;
import pcgen.core.gametable.MonsterRecipe;
import pcgen.core.gametable.MonsterSource;
import pcgen.system.LanguageBundle;

/**
 * The encounters the game master prepares, each with its name, monsters and notes,
 * and launched into the fight with one click.
 */
final class EncountersPanel extends JPanel
{
	private final GameTablePanel table;
	private final DefaultListModel<String> names = new DefaultListModel<>();
	private final JList<String> encounterList = new JList<>(names);
	private final MonstersModel monstersModel = new MonstersModel();
	private final JTable monsterTable = new JTable(monstersModel);
	private final JTextArea notes = new JTextArea(4, 30);
	private final JLabel title = new JLabel();
	private final JLabel worth = new JLabel();
	private final ImagesPanel scenarioImages;
	private final ImagesPanel encounterImages;
	private boolean refreshing;

	EncountersPanel(GameTablePanel table)
	{
		super(new BorderLayout());
		this.table = table;
		encounterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		encounterList.addListSelectionListener(event -> {
			if (!event.getValueIsAdjusting() && !refreshing)
			{
				showSelected();
			}
		});
		JPanel left = new JPanel(new BorderLayout());
		left.add(new JScrollPane(encounterList), BorderLayout.CENTER);
		JPanel listButtons = new JPanel(new GridLayout(0, 1));
		listButtons.add(GameTablePanel.button("in_gametable_newEncounter", this::newEncounter));
		listButtons.add(GameTablePanel.button("in_gametable_renameEncounter", this::renameEncounter));
		listButtons.add(GameTablePanel.button("in_gametable_deleteEncounter", this::deleteEncounter));
		listButtons.add(GameTablePanel.button("in_gametable_moveUp", () -> moveEncounter(-1)));
		listButtons.add(GameTablePanel.button("in_gametable_moveDown", () -> moveEncounter(1)));
		left.add(listButtons, BorderLayout.SOUTH);
		scenarioImages = new ImagesPanel(table, "in_gametable_scenarioImages", Optional::empty,
			List.of(ImageCategory.SCENARIO_ILLUSTRATION, ImageCategory.GENERAL_MAP));
		encounterImages = new ImagesPanel(table, "in_gametable_encounterImages",
			() -> selected().map(Encounter::name),
			List.of(ImageCategory.LOCATION_ILLUSTRATION, ImageCategory.BATTLEMAP, ImageCategory.PAWN,
				ImageCategory.OTHER));
		JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scenarioImages, left);

		JPanel right = new JPanel(new BorderLayout(4, 4));
		JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT));
		head.add(title);
		head.add(worth);
		right.add(head, BorderLayout.NORTH);
		monsterTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		right.add(new JScrollPane(monsterTable), BorderLayout.CENTER);
		JPanel south = new JPanel(new BorderLayout());
		JPanel monsterButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
		monsterButtons.add(GameTablePanel.button("in_gametable_addFromBestiary", this::addFromBestiary));
		monsterButtons.add(GameTablePanel.button("in_gametable_addFromFile", this::addFromFile));
		monsterButtons.add(GameTablePanel.button("in_gametable_changeCount", this::changeCount));
		monsterButtons.add(GameTablePanel.button("in_gametable_retouch", this::retouch));
		monsterButtons.add(GameTablePanel.button("in_gametable_removeMonster", this::removeMonster));
		PawnButtons.of(table, this, this::selectedPawn).forEach(monsterButtons::add);
		south.add(monsterButtons, BorderLayout.NORTH);
		notes.setLineWrap(true);
		notes.setWrapStyleWord(true);
		notes.setBorder(BorderFactory.createTitledBorder(LanguageBundle.getString("in_gametable_notes")));
		notes.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent event)
			{
				selected().filter(encounter -> !encounter.notes().equals(notes.getText()))
					.ifPresent(encounter -> replaceSelected(encounter.withNotes(notes.getText())));
			}
		});
		JPanel notesAndImages = new JPanel(new GridLayout(1, 2));
		notesAndImages.add(new JScrollPane(notes));
		notesAndImages.add(encounterImages);
		south.add(notesAndImages, BorderLayout.CENTER);
		south.add(GameTablePanel.button("in_gametable_launch", this::launch), BorderLayout.SOUTH);
		right.add(south, BorderLayout.SOUTH);

		add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, right), BorderLayout.CENTER);
	}

	void refresh()
	{
		List<Encounter> encounters = table.session().scenario().encounters();
		int selected = encounterList.getSelectedIndex();
		refreshing = true;
		try
		{
			List<String> wanted = encounters.stream().map(Encounter::name).toList();
			if (!wanted.equals(currentNames()))
			{
				names.clear();
				wanted.forEach(names::addElement);
				if (selected >= names.size())
				{
					selected = names.size() - 1;
				}
				if (selected >= 0)
				{
					encounterList.setSelectedIndex(selected);
				}
			}
		}
		finally
		{
			refreshing = false;
		}
		showSelected();
	}

	/**
	 * @return the encounter the game master is working on.
	 */
	Optional<Encounter> selected()
	{
		int index = encounterList.getSelectedIndex();
		List<Encounter> encounters = table.session().scenario().encounters();
		return (index >= 0 && index < encounters.size()) ? Optional.of(encounters.get(index)) : Optional.empty();
	}

	/**
	 * @param monster monsters to add to the selected encounter.
	 * @return whether an encounter was selected to take them.
	 */
	boolean addToSelected(EncounterMonster monster)
	{
		Optional<Encounter> encounter = selected();
		encounter.ifPresent(found -> replaceSelected(found.withMonster(monster)));
		return encounter.isPresent();
	}

	private List<String> currentNames()
	{
		List<String> current = new ArrayList<>();
		for (int index = 0; index < names.size(); index++)
		{
			current.add(names.get(index));
		}
		return current;
	}

	private void showSelected()
	{
		Optional<Encounter> encounter = selected();
		monstersModel.show(encounter.map(Encounter::monsters).orElse(List.of()));
		title.setText(encounter.map(found -> "<html><b>" + escape(found.name()) + "</b></html>").orElse(""));
		if (!notes.hasFocus())
		{
			notes.setText(encounter.map(Encounter::notes).orElse(""));
		}
		notes.setEnabled(encounter.isPresent());
		scenarioImages.refresh(table.session().scenario().images(), true);
		encounterImages.refresh(encounter.map(Encounter::images).orElse(List.of()), encounter.isPresent());
		worth.setText("");
		encounter.ifPresent(found -> table.inBackground(() -> found.monsters().stream()
			.mapToLong(monster -> table.session().experienceOf(monster)).sum(), this::showWorth));
	}

	private void showWorth(long experience)
	{
		String rating = Experience.challengeRatingOf(experience).orElse("—");
		worth.setText(LanguageBundle.getFormattedString("in_gametable_encounterWorth", rating, experience));
	}

	private void replaceSelected(Encounter changed)
	{
		int index = encounterList.getSelectedIndex();
		List<Encounter> encounters = new ArrayList<>(table.session().scenario().encounters());
		encounters.set(index, changed);
		table.session().setEncounters(encounters);
	}

	private Optional<String> askName(String initial)
	{
		String name = JOptionPane.showInputDialog(this, LanguageBundle.getString("in_gametable_encounterName"), initial);
		if (name == null)
		{
			return Optional.empty();
		}
		if (name.isBlank())
		{
			table.showError(LanguageBundle.getString("in_gametable_encounterNeedsName"));
			return Optional.empty();
		}
		return Optional.of(name);
	}

	private void newEncounter()
	{
		askName("").ifPresent(name -> {
			List<Encounter> encounters = new ArrayList<>(table.session().scenario().encounters());
			encounters.add(Encounter.named(name));
			table.session().setEncounters(encounters);
			refresh();
			encounterList.setSelectedIndex(encounters.size() - 1);
		});
	}

	private void renameEncounter()
	{
		selected().ifPresent(encounter -> askName(encounter.name())
			.ifPresent(name -> replaceSelected(encounter.withName(name))));
	}

	private void deleteEncounter()
	{
		int index = encounterList.getSelectedIndex();
		if (index >= 0 && JOptionPane.showConfirmDialog(this,
			LanguageBundle.getFormattedString("in_gametable_deleteEncounterConfirm", names.get(index)),
			LanguageBundle.getString("in_gametable_title"), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
		{
			List<Encounter> encounters = new ArrayList<>(table.session().scenario().encounters());
			encounters.remove(index);
			table.session().setEncounters(encounters);
		}
	}

	private void moveEncounter(int step)
	{
		int index = encounterList.getSelectedIndex();
		int target = index + step;
		List<Encounter> encounters = new ArrayList<>(table.session().scenario().encounters());
		if (index < 0 || target < 0 || target >= encounters.size())
		{
			return;
		}
		encounters.add(target, encounters.remove(index));
		table.session().setEncounters(encounters);
		refresh();
		encounterList.setSelectedIndex(target);
	}

	private void addFromBestiary()
	{
		if (selected().isEmpty())
		{
			return;
		}
		table.bestiary().pick(this).ifPresent(this::addToSelected);
	}

	private void addFromFile()
	{
		if (selected().isEmpty())
		{
			return;
		}
		PartyPanel.chooseCharacter(this).ifPresent(file -> {
			String name = file.getName().replaceFirst("\\.pcg$", "");
			addToSelected(new EncounterMonster(name,
				MonsterRecipe.of(new MonsterSource.CharacterFile(file.getAbsolutePath())), 1));
		});
	}

	private Optional<Integer> selectedMonsterIndex()
	{
		int row = monsterTable.getSelectedRow();
		return (row < 0) ? Optional.empty() : Optional.of(row);
	}

	private void changeMonster(int index, EncounterMonster changed)
	{
		selected().ifPresent(encounter -> {
			List<EncounterMonster> monsters = new ArrayList<>(encounter.monsters());
			monsters.set(index, changed);
			replaceSelected(encounter.withMonsters(monsters));
		});
	}

	private void changeCount()
	{
		selectedMonsterIndex().ifPresent(index -> {
			EncounterMonster monster = monstersModel.monsters.get(index);
			CombatPanel.askNumber(this, LanguageBundle.getFormattedString("in_gametable_howMany", monster.name()),
				monster.count()).filter(count -> count > 0)
				.ifPresent(count -> changeMonster(index, monster.withCount(count)));
		});
	}

	private Optional<BestiaryPawn> selectedPawn()
	{
		return selectedMonsterIndex().flatMap(index -> BestiaryPawn.of(monstersModel.monsters.get(index).recipe().source()));
	}

	private void retouch()
	{
		selectedMonsterIndex().ifPresent(index -> {
			EncounterMonster monster = monstersModel.monsters.get(index);
			table.inBackground(() -> table.session().templateKeysOf(monster.recipe()), outcome -> {
				List<String> current = (outcome instanceof ForgeOutcome.Built<List<String>> built) ? built.value()
					: List.of();
				RetouchDialog.edit(this, monster.recipe(), current)
					.ifPresent(recipe -> changeMonster(index, monster.withRecipe(recipe)));
			});
		});
	}

	private void removeMonster()
	{
		selectedMonsterIndex().ifPresent(index -> selected().ifPresent(encounter -> {
			List<EncounterMonster> monsters = new ArrayList<>(encounter.monsters());
			monsters.remove(index.intValue());
			replaceSelected(encounter.withMonsters(monsters));
		}));
	}

	private void launch()
	{
		selected().ifPresent(encounter -> {
			if (!table.session().combat().order().isEmpty() && JOptionPane.showConfirmDialog(this,
				LanguageBundle.getString("in_gametable_joinFight"), LanguageBundle.getString("in_gametable_title"),
				JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
			{
				return;
			}
			table.inBackground(() -> table.session().launch(encounter), problems -> {
				table.reportProblems(problems);
				table.showCombat();
			});
		});
	}

	static String escape(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	static String touchesOf(MonsterRecipe recipe)
	{
		List<String> touches = new ArrayList<>();
		if (recipe.addedHitDice() > 0)
		{
			touches.add(LanguageBundle.getFormattedString("in_gametable_plusHitDice", recipe.addedHitDice()));
		}
		recipe.addedTemplates().forEach(template -> touches.add("+" + template));
		recipe.removedTemplates().forEach(template -> touches.add("−" + template));
		return String.join(", ", touches);
	}

	/** The monsters of the selected encounter. */
	private static final class MonstersModel extends AbstractTableModel
	{
		private static final String[] COLUMNS = {"in_gametable_monster", "in_gametable_count", "in_gametable_source",
			"in_gametable_touches"};

		private List<EncounterMonster> monsters = List.of();

		void show(List<EncounterMonster> shown)
		{
			if (!shown.equals(monsters))
			{
				monsters = shown;
				fireTableDataChanged();
			}
		}

		@Override
		public int getRowCount()
		{
			return monsters.size();
		}

		@Override
		public int getColumnCount()
		{
			return COLUMNS.length;
		}

		@Override
		public String getColumnName(int column)
		{
			return LanguageBundle.getString(COLUMNS[column]);
		}

		@Override
		public Object getValueAt(int row, int column)
		{
			EncounterMonster monster = monsters.get(row);
			return switch (column)
			{
				case 0 -> monster.name();
				case 1 -> monster.count();
				case 2 -> RetouchDialog.sourceText(monster.recipe().source());
				default -> touchesOf(monster.recipe());
			};
		}
	}
}
