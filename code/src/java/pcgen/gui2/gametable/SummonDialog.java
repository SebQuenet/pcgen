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
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import pcgen.core.Globals;
import pcgen.core.gametable.Combatant;
import pcgen.core.gametable.CombatantSheets;
import pcgen.core.gametable.MonsterRecipe;
import pcgen.core.gametable.MonsterSource;
import pcgen.system.LanguageBundle;

/**
 * Asks what a combatant conjures into the fight. A player's creatures are only named,
 * since the player tracks them; a monster's are built from a race and a template.
 */
final class SummonDialog
{
	/** The templates a conjured creature usually takes. */
	private static final List<String> SUMMON_TEMPLATES = List.of("Celestial Creature", "Fiendish Creature",
		"Entropic Creature", "Resolute Creature");
	private static final int MOST_AT_ONCE = 20;
	private static final int MOST_ROUNDS = 999;

	private SummonDialog()
	{
	}

	/**
	 * What the game master asked for.
	 *
	 * @param name   the name of one creature
	 * @param recipe how to build each, for a monster's creatures
	 * @param count  how many arrive
	 * @param rounds rounds before they leave; empty when they stay
	 */
	record Request(String name, Optional<MonsterRecipe> recipe, int count, OptionalInt rounds)
	{
	}

	/**
	 * @param parent   the component the dialog opens over.
	 * @param summoner who conjures.
	 * @return what arrives; empty when the game master cancels.
	 */
	static Optional<Request> ask(Component parent, Combatant summoner)
	{
		boolean builds = summoner.kind().isGameMasters();
		JTextField name = new JTextField(20);
		SpinnerNumberModel count = new SpinnerNumberModel(1, 1, MOST_AT_ONCE, 1);
		SpinnerNumberModel rounds = new SpinnerNumberModel(0, 0, MOST_ROUNDS, 1);
		NamePicker races = new NamePicker();
		JComboBox<String> template = new JComboBox<>();
		template.addItem(LanguageBundle.getString("in_gametable_noTemplate"));
		Map<String, String> templates = CombatantSheets.templates();
		List<String> available = new ArrayList<>();
		for (String wanted : SUMMON_TEMPLATES)
		{
			templates.entrySet().stream().filter(entry -> entry.getValue().equals(wanted)).findFirst()
				.ifPresent(entry -> available.add(entry.getValue()));
		}
		available.forEach(template::addItem);

		JPanel fields = new JPanel(new GridLayout(0, 2, 4, 4));
		fields.add(new JLabel(LanguageBundle.getString("in_gametable_name")));
		fields.add(name);
		fields.add(new JLabel(LanguageBundle.getString("in_gametable_count")));
		fields.add(new JSpinner(count));
		fields.add(new JLabel(LanguageBundle.getString("in_gametable_conditionRounds")));
		fields.add(new JSpinner(rounds));
		JPanel form = new JPanel(new BorderLayout(4, 4));
		form.add(new JLabel(LanguageBundle.getFormattedString("in_gametable_conjuredBy", summoner.name())),
			BorderLayout.NORTH);
		form.add(fields, BorderLayout.CENTER);
		if (builds)
		{
			races.setNames(Globals.getContext() == null ? Map.of() : CombatantSheets.races());
			races.onSelection(picked -> picked.ifPresent(name::setText));
			races.setPreferredSize(new Dimension(360, 260));
			JPanel building = new JPanel(new BorderLayout(4, 4));
			building.add(races, BorderLayout.CENTER);
			building.add(template, BorderLayout.SOUTH);
			form.add(building, BorderLayout.SOUTH);
		}
		int answer = JOptionPane.showConfirmDialog(parent, form, LanguageBundle.getString("in_gametable_conjure"),
			JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (answer != JOptionPane.OK_OPTION || name.getText().isBlank() || (builds && races.selectedKey().isEmpty()))
		{
			return Optional.empty();
		}
		Optional<MonsterRecipe> recipe = Optional.empty();
		if (builds)
		{
			MonsterRecipe built = MonsterRecipe.of(new MonsterSource.BestiaryRace(races.selectedKey().get()));
			if (template.getSelectedIndex() > 0)
			{
				built = built.withTemplate(String.valueOf(template.getSelectedItem()));
			}
			recipe = Optional.of(built);
		}
		int lasting = rounds.getNumber().intValue();
		return Optional.of(new Request(name.getText().strip(), recipe, count.getNumber().intValue(),
			(lasting > 0) ? OptionalInt.of(lasting) : OptionalInt.empty()));
	}
}
