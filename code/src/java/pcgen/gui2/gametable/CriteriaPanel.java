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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import pcgen.core.gametable.catalog.BestiaryPreferences;
import pcgen.core.gametable.catalog.CatalogField;
import pcgen.core.gametable.catalog.Criterion;
import pcgen.core.gametable.catalog.FieldKind;
import pcgen.core.gametable.catalog.Joiner;
import pcgen.core.gametable.catalog.Operator;
import pcgen.core.gametable.catalog.Preset;
import pcgen.system.LanguageBundle;

/**
 * The game master's criteria on the bestiary: tests on fields, in groups met all or at least one, nested at will, and
 * the presets the game master named and kept.
 */
final class CriteriaPanel extends JPanel
{
	private static final String OPERAND_SEPARATOR = ";";
	private static final int TYPING_PAUSE = 300;

	private final BestiaryPreferences preferences;
	private final Timer changed;
	private final JPanel rootHolder = new JPanel(new BorderLayout());
	private final JComboBox<String> presets = new JComboBox<>();
	private GroupEditor root;

	/**
	 * @param preferences where the presets are kept.
	 * @param onChange    told when the criteria change, once the game master pauses typing.
	 */
	CriteriaPanel(BestiaryPreferences preferences, Runnable onChange)
	{
		super(new BorderLayout(4, 4));
		this.preferences = preferences;
		this.changed = new Timer(TYPING_PAUSE, event -> onChange.run());
		changed.setRepeats(false);
		root = new GroupEditor(null, new Criterion.Group(Joiner.ALL, List.of()));
		rootHolder.add(root, BorderLayout.NORTH);

		JPanel presetBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
		presets.setPrototypeDisplayValue("Sous-sol FP 5-8 volants");
		refreshPresets();
		presets.addActionListener(event -> loadPreset());
		presetBar.add(presets);
		presetBar.add(GameTablePanel.button("in_gametable_presetSave", this::savePreset));
		presetBar.add(GameTablePanel.button("in_gametable_presetDelete", this::deletePreset));
		presetBar.add(GameTablePanel.button("in_gametable_criteriaClear", () -> show(new Criterion.Group(Joiner.ALL, List.of()))));

		add(presetBar, BorderLayout.NORTH);
		add(new JScrollPane(rootHolder), BorderLayout.CENTER);
	}

	/**
	 * @return the criteria as the game master set them.
	 */
	Criterion criterion()
	{
		return root.criterion();
	}

	/**
	 * @param criterion criteria to show and edit.
	 */
	void show(Criterion criterion)
	{
		Criterion.Group group = criterion instanceof Criterion.Group asGroup ? asGroup
			: new Criterion.Group(Joiner.ALL, List.of(criterion));
		rootHolder.removeAll();
		root = new GroupEditor(null, group);
		rootHolder.add(root, BorderLayout.NORTH);
		rootHolder.revalidate();
		rootHolder.repaint();
		fireChange();
	}

	private void fireChange()
	{
		changed.restart();
	}

	private void refreshPresets()
	{
		DefaultComboBoxModel<String> names = new DefaultComboBoxModel<>();
		names.addElement("");
		preferences.presets().forEach(preset -> names.addElement(preset.name()));
		presets.setModel(names);
	}

	private void loadPreset()
	{
		Object chosen = presets.getSelectedItem();
		preferences.presets().stream().filter(preset -> preset.name().equals(chosen)).findFirst()
			.ifPresent(preset -> show(preset.criterion()));
	}

	private void savePreset()
	{
		Object current = presets.getSelectedItem();
		String name = JOptionPane.showInputDialog(this, LanguageBundle.getString("in_gametable_presetName"),
			current == null ? "" : current.toString());
		if (name == null || name.isBlank())
		{
			return;
		}
		List<Preset> kept = new ArrayList<>(preferences.presets());
		kept.removeIf(preset -> preset.name().equals(name.strip()));
		kept.add(new Preset(name.strip(), criterion()));
		preferences.savePresets(kept);
		refreshPresets();
		presets.setSelectedItem(name.strip());
	}

	private void deletePreset()
	{
		Object chosen = presets.getSelectedItem();
		if (chosen == null || chosen.toString().isEmpty())
		{
			return;
		}
		List<Preset> kept = new ArrayList<>(preferences.presets());
		kept.removeIf(preset -> preset.name().equals(chosen));
		preferences.savePresets(kept);
		refreshPresets();
	}

	/**
	 * A group of criteria, met all or at least one.
	 */
	private final class GroupEditor extends JPanel
	{
		private final JComboBox<Joiner> joiner = new JComboBox<>(Joiner.values());
		private final JPanel members = new JPanel();

		GroupEditor(GroupEditor parent, Criterion.Group group)
		{
			super(new BorderLayout(2, 2));
			setBorder(BorderFactory.createEtchedBorder());
			joiner.setSelectedItem(group.joiner());
			joiner.setRenderer(new DefaultListCellRenderer()
			{
				@Override
				public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean hasFocus)
				{
					String label = value == Joiner.ANY ? LanguageBundle.getString("in_gametable_joinerAny")
						: LanguageBundle.getString("in_gametable_joinerAll");
					return super.getListCellRendererComponent(list, label, index, isSelected, hasFocus);
				}
			});
			joiner.addActionListener(event -> fireChange());
			members.setLayout(new BoxLayout(members, BoxLayout.Y_AXIS));
			for (Criterion member : group.members())
			{
				members.add(editorOf(member));
			}

			JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
			bar.add(joiner);
			bar.add(GameTablePanel.button("in_gametable_addCriterion", () -> addMember(new TestEditor(this,
				new Criterion.Test(CatalogField.NAME, Operator.CONTAINS, List.of())))));
			bar.add(GameTablePanel.button("in_gametable_addGroup", () -> addMember(new GroupEditor(this,
				new Criterion.Group(Joiner.ANY, List.of())))));
			if (parent != null)
			{
				bar.add(GameTablePanel.button("in_gametable_removeCriterion", () -> parent.removeMember(this)));
			}
			add(bar, BorderLayout.NORTH);
			add(members, BorderLayout.CENTER);
		}

		private Component editorOf(Criterion member)
		{
			return switch (member)
			{
				case Criterion.Test test -> new TestEditor(this, test);
				case Criterion.Group group -> new GroupEditor(this, group);
			};
		}

		private void addMember(JPanel editor)
		{
			members.add(editor);
			members.revalidate();
			fireChange();
		}

		private void removeMember(JPanel editor)
		{
			members.remove(editor);
			members.revalidate();
			members.repaint();
			fireChange();
		}

		Criterion.Group criterion()
		{
			List<Criterion> criteria = new ArrayList<>();
			for (Component member : members.getComponents())
			{
				if (member instanceof TestEditor test)
				{
					test.criterion().ifPresent(criteria::add);
				}
				else if (member instanceof GroupEditor group)
				{
					criteria.add(group.criterion());
				}
			}
			return new Criterion.Group((Joiner) joiner.getSelectedItem(), criteria);
		}
	}

	/**
	 * One test: a field, an operator, operands separated by semicolons.
	 */
	private final class TestEditor extends JPanel
	{
		private final JComboBox<CatalogField> field = new JComboBox<>(CatalogField.values());
		private final JComboBox<Operator> operator = new JComboBox<>();
		private final JTextField operands = new JTextField(12);

		TestEditor(GroupEditor parent, Criterion.Test test)
		{
			super(new FlowLayout(FlowLayout.LEFT, 2, 0));
			field.setRenderer(labelled(value -> LanguageBundle.getString(((CatalogField) value).group().labelKey())
				+ " — " + LanguageBundle.getString(((CatalogField) value).labelKey())));
			operator.setRenderer(labelled(value -> LanguageBundle.getString(((Operator) value).labelKey())));
			field.setSelectedItem(test.field());
			fillOperators(test.operator());
			operands.setText(String.join(OPERAND_SEPARATOR, test.operands()));
			field.addActionListener(event -> {
				fillOperators((Operator) operator.getSelectedItem());
				fireChange();
			});
			operator.addActionListener(event -> fireChange());
			operands.getDocument().addDocumentListener(new DocumentListener()
			{
				@Override
				public void insertUpdate(DocumentEvent event)
				{
					fireChange();
				}

				@Override
				public void removeUpdate(DocumentEvent event)
				{
					fireChange();
				}

				@Override
				public void changedUpdate(DocumentEvent event)
				{
					fireChange();
				}
			});
			add(field);
			add(operator);
			add(operands);
			add(GameTablePanel.button("in_gametable_removeCriterion", () -> parent.removeMember(this)));
			add(Box.createHorizontalStrut(4));
		}

		private void fillOperators(Operator wanted)
		{
			FieldKind kind = ((CatalogField) field.getSelectedItem()).kind();
			Operator[] applicable = Arrays.stream(Operator.values()).filter(candidate -> candidate.appliesTo(kind))
				.toArray(Operator[]::new);
			operator.setModel(new DefaultComboBoxModel<>(applicable));
			operator.setSelectedItem(Arrays.asList(applicable).contains(wanted) ? wanted : applicable[0]);
			operands.setEnabled(kind != FieldKind.FLAG);
			operands.setToolTipText(LanguageBundle.getString("in_gametable_operandsHint_" + kind.name().toLowerCase(Locale.ROOT)));
		}

		/**
		 * @return the test; empty while it waits for operands.
		 */
		Optional<Criterion> criterion()
		{
			CatalogField chosen = (CatalogField) field.getSelectedItem();
			Operator comparison = (Operator) operator.getSelectedItem();
			List<String> typed = Arrays.stream(operands.getText().split(OPERAND_SEPARATOR)).map(String::strip)
				.filter(operand -> !operand.isEmpty()).toList();
			if (chosen == null || comparison == null || (typed.isEmpty() && chosen.kind() != FieldKind.FLAG))
			{
				return Optional.empty();
			}
			return Optional.of(new Criterion.Test(chosen, comparison, typed));
		}
	}

	private static DefaultListCellRenderer labelled(Function<Object, String> label)
	{
		return new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean hasFocus)
			{
				return super.getListCellRendererComponent(list, value == null ? "" : label.apply(value), index, isSelected,
					hasFocus);
			}
		};
	}
}
