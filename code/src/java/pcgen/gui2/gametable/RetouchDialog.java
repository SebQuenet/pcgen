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
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import pcgen.core.gametable.CombatantSheets;
import pcgen.core.gametable.MonsterRecipe;
import pcgen.core.gametable.MonsterSource;
import pcgen.system.LanguageBundle;

/**
 * Touches a monster: racial hit dice added, templates laid or taken off.
 */
final class RetouchDialog extends JDialog
{
	private static final int MOST_HIT_DICE = 60;

	private MonsterRecipe recipe;
	private boolean accepted;
	private final DefaultListModel<String> templates = new DefaultListModel<>();
	private final JList<String> templateList = new JList<>(templates);
	private final NamePicker available = new NamePicker();
	private final SpinnerNumberModel hitDice;
	private final JTextField name = new JTextField(20);
	private final JLabel summary = new JLabel(" ");
	private final Function<MonsterRecipe, String> summarize;

	/**
	 * A creature made unique: its name, and how it is built.
	 *
	 * @param name   its name.
	 * @param recipe its recipe.
	 */
	record Unique(String name, MonsterRecipe recipe)
	{
	}

	private RetouchDialog(Component parent, MonsterRecipe start, List<String> currentTemplates)
	{
		this(parent, start, currentTemplates, CombatantSheets.templates(), Optional.empty(), recipe -> "");
	}

	private RetouchDialog(Component parent, MonsterRecipe start, List<String> currentTemplates,
		Map<String, String> offered, Optional<String> startName, Function<MonsterRecipe, String> summarize)
	{
		super(SwingUtilities.getWindowAncestor(parent), LanguageBundle.getString("in_gametable_retouch"),
			ModalityType.APPLICATION_MODAL);
		this.recipe = start;
		currentTemplates.forEach(templates::addElement);
		start.addedTemplates().stream().filter(key -> !templates.contains(key)).forEach(templates::addElement);
		this.summarize = summarize;
		available.setNames(offered);
		hitDice = new SpinnerNumberModel(start.addedHitDice(), 0, MOST_HIT_DICE, 1);

		JPanel lists = new JPanel(new GridLayout(1, 2, 8, 0));
		JPanel laid = new JPanel(new BorderLayout());
		laid.setBorder(BorderFactory.createTitledBorder(LanguageBundle.getString("in_gametable_templatesLaid")));
		laid.add(new JScrollPane(templateList), BorderLayout.CENTER);
		laid.add(GameTablePanel.button("in_gametable_takeOffTemplate", this::takeOff), BorderLayout.SOUTH);
		JPanel more = new JPanel(new BorderLayout());
		more.setBorder(BorderFactory.createTitledBorder(LanguageBundle.getString("in_gametable_templatesAvailable")));
		more.add(available, BorderLayout.CENTER);
		more.add(GameTablePanel.button("in_gametable_layTemplate", this::lay), BorderLayout.SOUTH);
		lists.add(laid);
		lists.add(more);

		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
		top.add(new JLabel(sourceText(start.source())));
		top.add(new JLabel(LanguageBundle.getString("in_gametable_addedHitDice")));
		top.add(new JSpinner(hitDice));
		startName.ifPresent(given -> {
			name.setText(given);
			top.add(new JLabel(LanguageBundle.getString("in_gametable_uniqueName")));
			top.add(name);
			top.add(summary);
			hitDice.addChangeListener(event -> showSummary());
			showSummary();
		});

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton ok = GameTablePanel.button("in_gametable_ok", () -> {
			accepted = true;
			dispose();
		});
		buttons.add(ok);
		buttons.add(GameTablePanel.button("in_gametable_cancel", this::dispose));
		getRootPane().setDefaultButton(ok);

		setLayout(new BorderLayout(4, 4));
		add(top, BorderLayout.NORTH);
		add(lists, BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);
		setPreferredSize(new Dimension(640, 420));
		pack();
		setLocationRelativeTo(parent);
	}

	/**
	 * @param parent           the component the dialog opens over.
	 * @param recipe           the monster as it is.
	 * @param currentTemplates the keys of the templates the monster has now.
	 * @return the monster touched; empty when the game master cancels.
	 */
	static Optional<MonsterRecipe> edit(Component parent, MonsterRecipe recipe, List<String> currentTemplates)
	{
		RetouchDialog dialog = new RetouchDialog(parent, recipe, currentTemplates);
		dialog.setVisible(true);
		if (!dialog.accepted)
		{
			return Optional.empty();
		}
		return Optional.of(dialog.recipe.withHitDice(dialog.hitDice.getNumber().intValue()));
	}

	/**
	 * @param parent           the component the dialog opens over.
	 * @param recipe           the bestiary entry as it is.
	 * @param currentTemplates the keys of the templates it has now.
	 * @param offered          the templates it may take, by name.
	 * @param startName        the name it starts with.
	 * @param summarize        its hit points and challenge rating once touched, computed off the event thread.
	 * @return the creature made unique; empty when the game master cancels or leaves the name blank.
	 */
	static Optional<Unique> editUnique(Component parent, MonsterRecipe recipe, List<String> currentTemplates,
		Map<String, String> offered, String startName, Function<MonsterRecipe, String> summarize)
	{
		RetouchDialog dialog = new RetouchDialog(parent, recipe, currentTemplates, offered, Optional.of(startName), summarize);
		dialog.setVisible(true);
		String chosen = dialog.name.getText().strip();
		if (!dialog.accepted || chosen.isEmpty())
		{
			return Optional.empty();
		}
		return Optional.of(new Unique(chosen, dialog.recipe.withHitDice(dialog.hitDice.getNumber().intValue())));
	}

	private void showSummary()
	{
		MonsterRecipe touched = recipe.withHitDice(hitDice.getNumber().intValue());
		summary.setText("…");
		new SwingWorker<String, Void>()
		{
			@Override
			protected String doInBackground()
			{
				return summarize.apply(touched);
			}

			@Override
			protected void done()
			{
				try
				{
					summary.setText(get());
				}
				catch (InterruptedException interrupted)
				{
					Thread.currentThread().interrupt();
				}
				catch (ExecutionException failed)
				{
					summary.setText(String.valueOf(failed.getCause().getMessage()));
				}
			}
		}.execute();
	}

	static String sourceText(MonsterSource source)
	{
		return switch (source)
		{
			case MonsterSource.BestiaryRace race -> LanguageBundle.getFormattedString("in_gametable_fromRace",
				race.raceKey());
			case MonsterSource.CharacterFile file -> LanguageBundle.getFormattedString("in_gametable_fromFile",
				new File(file.path()).getName());
			case MonsterSource.NpcKit kit -> LanguageBundle.getFormattedString("in_gametable_fromKit", kit.kitKey());
		};
	}

	private void lay()
	{
		available.selectedKey().ifPresent(key -> {
			recipe = recipe.withTemplate(key);
			if (!templates.contains(key))
			{
				templates.addElement(key);
			}
			showSummary();
		});
	}

	private void takeOff()
	{
		String key = templateList.getSelectedValue();
		if (key != null)
		{
			recipe = recipe.withoutTemplate(key);
			templates.removeElement(key);
			showSummary();
		}
	}
}
