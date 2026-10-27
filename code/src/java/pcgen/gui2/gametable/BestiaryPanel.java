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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Map;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import pcgen.core.Globals;
import pcgen.core.gametable.BestiaryPawn;
import pcgen.core.gametable.CombatantSheets;
import pcgen.core.gametable.EncounterMonster;
import pcgen.core.gametable.ForgeOutcome;
import pcgen.core.gametable.MonsterRecipe;
import pcgen.core.gametable.MonsterSource;
import pcgen.core.gametable.SheetPreview;
import pcgen.system.LanguageBundle;

/**
 * The races of the loaded sources, searched by name, with the numbers of the one
 * picked; a race goes into the selected encounter or straight into the fight.
 */
final class BestiaryPanel extends JPanel
{
	private static final int MOST_AT_ONCE = 99;

	private final GameTablePanel table;
	private final NamePicker races = new NamePicker();
	private final JEditorPane preview = new JEditorPane("text/html", "");
	private final SpinnerNumberModel count = new SpinnerNumberModel(1, 1, MOST_AT_ONCE, 1);

	BestiaryPanel(GameTablePanel table)
	{
		super(new BorderLayout());
		this.table = table;
		preview.setEditable(false);
		preview.addHyperlinkListener(GameTablePanel::browse);
		races.onSelection(name -> showPreview());
		JPanel right = new JPanel(new BorderLayout());
		right.add(new JScrollPane(preview), BorderLayout.CENTER);
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
		buttons.add(new JLabel(LanguageBundle.getString("in_gametable_count")));
		buttons.add(new JSpinner(count));
		buttons.add(GameTablePanel.button("in_gametable_toEncounter", this::toEncounter));
		buttons.add(GameTablePanel.button("in_gametable_toFight", this::toFight));
		PawnButtons.of(table, this, this::pickedPawn).forEach(buttons::add);
		right.add(buttons, BorderLayout.SOUTH);
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, races, right);
		split.setResizeWeight(0.35);
		add(split, BorderLayout.CENTER);
	}

	/** Lists the races of the sources loaded now. */
	void refreshSources()
	{
		races.setNames(racesLoaded());
	}

	private static Map<String, String> racesLoaded()
	{
		return Globals.getContext() == null ? Map.of() : CombatantSheets.races();
	}

	private Optional<EncounterMonster> picked()
	{
		return races.selectedKey().map(key -> new EncounterMonster(races.selectedName().orElse(key),
			MonsterRecipe.of(new MonsterSource.BestiaryRace(key)), count.getNumber().intValue()));
	}

	private Optional<BestiaryPawn> pickedPawn()
	{
		return picked().flatMap(monster -> BestiaryPawn.of(monster.recipe().source()));
	}

	private void showPreview()
	{
		preview.setText("");
		picked().ifPresent(monster -> table.inBackground(() -> table.session().previewSheet(monster.recipe()), outcome -> {
			switch (outcome)
			{
				case ForgeOutcome.Built<SheetPreview> built -> preview.setText(SheetHtml.preview(built.value().sheet(),
					BestiaryPawn.of(monster.recipe().source()), TacticsHtml.of(built.value().monster())));
				case ForgeOutcome.Failed<SheetPreview> failed -> preview.setText(
					EncountersPanel.escape(failed.reason()));
			}
			preview.setCaretPosition(0);
		}));
	}

	private void toEncounter()
	{
		picked().ifPresent(monster -> {
			if (!table.encounters().addToSelected(monster))
			{
				table.showError(LanguageBundle.getString("in_gametable_pickEncounterFirst"));
			}
		});
	}

	private void toFight()
	{
		picked().ifPresent(monster -> table.inBackground(
			() -> table.session().addMonsters(monster.name(), monster.recipe(), monster.count()), problems -> {
				table.reportProblems(problems);
				table.showCombat();
			}));
	}

	/**
	 * @param parent the component the dialog opens over.
	 * @return the race and number the game master picks; empty when cancelled.
	 */
	static Optional<EncounterMonster> pickRace(Component parent)
	{
		JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
			LanguageBundle.getString("in_gametable_bestiary"), JDialog.ModalityType.APPLICATION_MODAL);
		NamePicker picker = new NamePicker();
		picker.setNames(racesLoaded());
		SpinnerNumberModel number = new SpinnerNumberModel(1, 1, MOST_AT_ONCE, 1);
		boolean[] accepted = {false};
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(new JLabel(LanguageBundle.getString("in_gametable_count")));
		buttons.add(new JSpinner(number));
		JButton ok = GameTablePanel.button("in_gametable_ok", () -> {
			accepted[0] = true;
			dialog.dispose();
		});
		buttons.add(ok);
		buttons.add(GameTablePanel.button("in_gametable_cancel", dialog::dispose));
		dialog.getRootPane().setDefaultButton(ok);
		dialog.setLayout(new BorderLayout());
		dialog.add(picker, BorderLayout.CENTER);
		dialog.add(buttons, BorderLayout.SOUTH);
		dialog.setPreferredSize(new Dimension(420, 480));
		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
		if (!accepted[0])
		{
			return Optional.empty();
		}
		return picker.selectedKey().map(key -> new EncounterMonster(picker.selectedName().orElse(key),
			MonsterRecipe.of(new MonsterSource.BestiaryRace(key)), number.getNumber().intValue()));
	}
}
