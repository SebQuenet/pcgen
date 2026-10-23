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
import java.util.OptionalInt;
import java.util.stream.Collectors;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import pcgen.core.conjurations.BonusSourceKey;
import pcgen.core.gametable.Combatant;
import pcgen.core.gametable.Condition;
import pcgen.system.LanguageBundle;

/**
 * Asks which condition, temporary bonus or negative property to lay on a combatant,
 * and for how many rounds. On a monster, one drawn from the data changes its numbers;
 * anything else is a label.
 */
final class ConditionDialog
{
	private static final int MOST_ROUNDS = 999;

	private ConditionDialog()
	{
	}

	/**
	 * @param parent    the component the dialog opens over.
	 * @param combatant the combatant the condition goes on.
	 * @param sources   the conditions and bonuses the data holds, by name.
	 * @return the condition; empty when the game master cancels.
	 */
	static Optional<Condition> ask(Component parent, Combatant combatant, Map<String, BonusSourceKey> sources)
	{
		NamePicker names = new NamePicker();
		names.setNames(sources.entrySet().stream()
			.collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().text())));
		names.setPreferredSize(new Dimension(380, 260));
		SpinnerNumberModel rounds = new SpinnerNumberModel(0, 0, MOST_ROUNDS, 1);
		JPanel form = new JPanel(new BorderLayout(4, 4));
		form.add(new JLabel(LanguageBundle.getFormattedString("in_gametable_conditionOn", combatant.name())),
			BorderLayout.NORTH);
		form.add(names, BorderLayout.CENTER);
		JPanel lasting = new JPanel(new FlowLayout(FlowLayout.LEFT));
		lasting.add(new JLabel(LanguageBundle.getString("in_gametable_conditionRounds")));
		lasting.add(new JSpinner(rounds));
		form.add(lasting, BorderLayout.SOUTH);
		int answer = JOptionPane.showConfirmDialog(parent, form, LanguageBundle.getString("in_gametable_condition"),
			JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (answer != JOptionPane.OK_OPTION)
		{
			return Optional.empty();
		}
		// A name picked from the list comes from the data; anything typed alone is a label
		Optional<String> picked = names.selectedName();
		String conditionName = picked.orElse(names.filterText());
		if (conditionName.isBlank())
		{
			return Optional.empty();
		}
		int roundCount = rounds.getNumber().intValue();
		OptionalInt roundsLeft = (roundCount > 0) ? OptionalInt.of(roundCount) : OptionalInt.empty();
		Optional<BonusSourceKey> bonus = combatant.kind().isGameMasters()
			? picked.map(sources::get) : Optional.empty();
		return Optional.of(new Condition(conditionName, bonus, roundsLeft));
	}
}
