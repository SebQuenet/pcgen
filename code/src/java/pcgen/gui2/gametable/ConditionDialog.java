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
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import pcgen.core.bonus.IntRange;
import pcgen.core.conjurations.BonusSourceKey;
import pcgen.core.gametable.Condition;
import pcgen.facade.core.BonusChoice;
import pcgen.system.LanguageBundle;

/**
 * Asks the game master for a condition and the combatants it goes on: one, the rows
 * picked, a monster's group or every monster, as the conjurations lay a bonus on
 * every creature at once.
 */
final class ConditionDialog
{
	private static final int MOST_ROUNDS = 999;

	/**
	 * @param label what the game master reads, such as "the Goblin group (4)"
	 * @param ids   the combatants it names
	 */
	record Scope(String label, List<Integer> ids)
	{
		Scope
		{
			ids = List.copyOf(ids);
		}
	}

	/**
	 * @param scope     the combatants the condition goes on.
	 * @param condition the condition.
	 */
	record Laying(Scope scope, Condition condition)
	{
	}

	private ConditionDialog()
	{
	}

	/**
	 * @param parent  the component the dialog opens over.
	 * @param title   what the dialog is for, such as laying a condition or casting a spell.
	 * @param scopes  the combatants it can go on, the first picked at first. Never empty.
	 * @param choices the conditions and bonuses the data holds for some combatants.
	 * @return the condition and where it goes; empty when the game master cancels.
	 */
	static Optional<Laying> ask(Component parent, String title, List<Scope> scopes, Function<List<Integer>, List<BonusChoice>> choices)
	{
		Map<String, BonusChoice> shown = new LinkedHashMap<>();
		NamePicker names = new NamePicker();
		names.setPreferredSize(new Dimension(380, 260));
		SpinnerNumberModel rounds = new SpinnerNumberModel(0, 0, MOST_ROUNDS, 1);
		SpinnerNumberModel chosen = new SpinnerNumberModel(0, 0, 0, 1);
		JSpinner chosenSpinner = new JSpinner(chosen);
		JLabel chosenLabel = new JLabel(LanguageBundle.getString("in_gametable_conditionValue"));
		names.onSelection(name -> {
			Optional<IntRange> range = name.map(shown::get).flatMap(BonusChoice::range);
			chosenSpinner.setEnabled(range.isPresent());
			chosenLabel.setEnabled(range.isPresent());
			range.ifPresent(found -> {
				chosen.setMinimum(found.minimum());
				chosen.setMaximum(found.maximum());
				chosen.setValue(found.maximum());
			});
		});

		ButtonGroup scopeButtons = new ButtonGroup();
		JPanel scopePanel = new JPanel(new GridLayout(0, 1));
		Scope[] picked = {scopes.getFirst()};
		for (Scope scope : scopes)
		{
			JRadioButton button = new JRadioButton(scope.label(), scope == picked[0]);
			button.addActionListener(event -> {
				picked[0] = scope;
				show(names, shown, choices.apply(scope.ids()));
			});
			scopeButtons.add(button);
			scopePanel.add(button);
		}
		show(names, shown, choices.apply(picked[0].ids()));

		JPanel lasting = new JPanel(new FlowLayout(FlowLayout.LEFT));
		lasting.add(new JLabel(LanguageBundle.getString("in_gametable_conditionRounds")));
		lasting.add(new JSpinner(rounds));
		lasting.add(chosenLabel);
		lasting.add(chosenSpinner);
		chosenSpinner.setEnabled(false);
		chosenLabel.setEnabled(false);
		JPanel form = new JPanel(new BorderLayout(4, 4));
		form.add(scopePanel, BorderLayout.NORTH);
		form.add(names, BorderLayout.CENTER);
		form.add(lasting, BorderLayout.SOUTH);
		int answer = JOptionPane.showConfirmDialog(parent, form, title,
			JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (answer != JOptionPane.OK_OPTION)
		{
			return Optional.empty();
		}
		// A name picked from the list comes from the data; anything typed alone is a label
		Optional<BonusChoice> choice = names.selectedName().map(shown::get);
		String conditionName = choice.map(BonusChoice::label).orElse(names.filterText());
		if (conditionName.isBlank())
		{
			return Optional.empty();
		}
		int roundCount = rounds.getNumber().intValue();
		OptionalInt roundsLeft = (roundCount > 0) ? OptionalInt.of(roundCount) : OptionalInt.empty();
		Optional<BonusSourceKey> bonus = choice.flatMap(found -> BonusSourceKey.parse(found.key()));
		OptionalInt value = choice.flatMap(BonusChoice::range).isPresent()
			? OptionalInt.of(chosen.getNumber().intValue()) : OptionalInt.empty();
		return Optional.of(new Laying(picked[0], new Condition(conditionName, bonus, roundsLeft, value)));
	}

	private static void show(NamePicker names, Map<String, BonusChoice> shown, List<BonusChoice> choices)
	{
		shown.clear();
		choices.forEach(choice -> shown.putIfAbsent(choice.label(), choice));
		Map<String, String> keys = new LinkedHashMap<>();
		shown.forEach((label, choice) -> keys.put(label, choice.key()));
		names.setNames(keys);
	}
}
