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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A list of names, such as the races or templates of the loaded sources, filtered as
 * the game master types.
 */
final class NamePicker extends JPanel
{
	private final JTextField filter = new JTextField();
	private final DefaultListModel<String> shown = new DefaultListModel<>();
	private final JList<String> list = new JList<>(shown);
	private Map<String, String> keysByName = Map.of();

	NamePicker()
	{
		super(new BorderLayout());
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		add(filter, BorderLayout.NORTH);
		add(new JScrollPane(list), BorderLayout.CENTER);
		filter.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent event)
			{
				applyFilter();
			}

			@Override
			public void removeUpdate(DocumentEvent event)
			{
				applyFilter();
			}

			@Override
			public void changedUpdate(DocumentEvent event)
			{
				applyFilter();
			}
		});
	}

	/**
	 * @param names the keys of the objects to pick from, by the name shown.
	 */
	void setNames(Map<String, String> names)
	{
		keysByName = Map.copyOf(names);
		applyFilter();
	}

	/**
	 * @param listener told the name picked whenever the selection changes.
	 */
	void onSelection(Consumer<Optional<String>> listener)
	{
		list.addListSelectionListener(event -> {
			if (!event.getValueIsAdjusting())
			{
				listener.accept(selectedName());
			}
		});
	}

	/**
	 * @return what the game master typed to filter the list.
	 */
	String filterText()
	{
		return filter.getText().strip();
	}

	Optional<String> selectedName()
	{
		return Optional.ofNullable(list.getSelectedValue());
	}

	Optional<String> selectedKey()
	{
		return selectedName().map(keysByName::get);
	}

	private void applyFilter()
	{
		String wanted = filter.getText().strip().toLowerCase(Locale.ROOT);
		shown.clear();
		keysByName.keySet().stream().filter(name -> name.toLowerCase(Locale.ROOT).contains(wanted))
			.sorted(String.CASE_INSENSITIVE_ORDER).forEach(shown::addElement);
	}
}
