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

import java.awt.Component;
import java.awt.GridLayout;
import java.util.Optional;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import pcgen.system.LanguageBundle;

/**
 * Asks for a monster no loaded bestiary has: only what the fight needs to track it.
 */
final class FreehandMonsterDialog
{
	private static final int DEFAULT_HIT_POINTS = 10;
	private static final int MOST_HIT_POINTS = 9999;
	private static final int LOWEST_MODIFIER = -20;
	private static final int HIGHEST_MODIFIER = 50;
	private static final int MOST_AT_ONCE = 50;

	private FreehandMonsterDialog()
	{
	}

	/**
	 * What the game master wrote.
	 *
	 * @param name               the name of one monster
	 * @param maximumHitPoints   the full hit points of each
	 * @param initiativeModifier their initiative modifier
	 * @param count              how many enter
	 */
	record Request(String name, int maximumHitPoints, int initiativeModifier, int count)
	{
	}

	/**
	 * @param parent the component the dialog opens over.
	 * @return the monsters to add; empty when the game master cancels or leaves the name blank.
	 */
	static Optional<Request> ask(Component parent)
	{
		JTextField name = new JTextField(20);
		JSpinner hitPoints = new JSpinner(new SpinnerNumberModel(DEFAULT_HIT_POINTS, 1, MOST_HIT_POINTS, 1));
		JSpinner modifier = new JSpinner(new SpinnerNumberModel(0, LOWEST_MODIFIER, HIGHEST_MODIFIER, 1));
		JSpinner count = new JSpinner(new SpinnerNumberModel(1, 1, MOST_AT_ONCE, 1));
		JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
		form.add(new JLabel(LanguageBundle.getString("in_gametable_name")));
		form.add(name);
		form.add(new JLabel(LanguageBundle.getString("in_gametable_hitPoints")));
		form.add(hitPoints);
		form.add(new JLabel(LanguageBundle.getString("in_gametable_initiativeModifier")));
		form.add(modifier);
		form.add(new JLabel(LanguageBundle.getString("in_gametable_count")));
		form.add(count);
		int answer = JOptionPane.showConfirmDialog(parent, form, LanguageBundle.getString("in_gametable_addFreehand"),
			JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (answer != JOptionPane.OK_OPTION || name.getText().isBlank())
		{
			return Optional.empty();
		}
		return Optional.of(new Request(name.getText().strip(), (Integer) hitPoints.getValue(),
			(Integer) modifier.getValue(), (Integer) count.getValue()));
	}
}
