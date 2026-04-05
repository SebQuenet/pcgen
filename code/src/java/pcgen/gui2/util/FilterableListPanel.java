/*
 * Copyright 2026 (C) Seb Quenet
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
package pcgen.gui2.util;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionListener;

import pcgen.facade.core.InfoFacade;
import pcgen.facade.util.ListFacade;
import pcgen.gui2.filter.FilteredListFacade;
import pcgen.gui2.tools.Icons;
import pcgen.system.LanguageBundle;

import org.apache.commons.lang3.StringUtils;

/**
 * A panel containing a text filter field and a scrollable {@link JList}.
 * As the user types in the filter field, the list is filtered to show
 * only items matching the search text (case-insensitive substring match
 * on {@code toString()} and {@code getType()} for {@link InfoFacade} elements).
 *
 * @param <E> The type of elements in the list
 */
@SuppressWarnings("serial")
public class FilterableListPanel<E> extends JPanel implements DocumentListener
{

	private final JTextField filterField;
	private final JButton clearButton;
	private final JList<E> list;
	private final FilteredListFacade<Object, E> filteredFacade;
	private final FacadeListModel<E> listModel;

	public FilterableListPanel()
	{
		filterField = new JTextField();
		clearButton = new JButton(Icons.CloseX9.getImageIcon());
		list = new JList<>();
		filteredFacade = new FilteredListFacade<>();
		listModel = new FacadeListModel<>();

		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setVisibleRowCount(6);

		filterField.getDocument().addDocumentListener(this);
		clearButton.addActionListener(e -> filterField.setText(""));

		JPanel filterPanel = new JPanel(new BorderLayout());
		filterPanel.add(
			new javax.swing.JLabel(LanguageBundle.getString("in_filterLabel")),
			BorderLayout.WEST
		);
		filterPanel.add(filterField, BorderLayout.CENTER);
		filterPanel.add(clearButton, BorderLayout.EAST);

		setLayout(new BorderLayout());
		add(filterPanel, BorderLayout.NORTH);
		add(new JScrollPane(list), BorderLayout.CENTER);

		filteredFacade.setFilter(this::acceptFilter);
	}

	/**
	 * Set the data source for this panel.
	 *
	 * @param source the list of items to display
	 */
	public void setListFacade(ListFacade<E> source)
	{
		filteredFacade.setDelegate(source);
		listModel.setListFacade(filteredFacade);
		list.setModel(listModel);
	}

	/**
	 * Set the cell renderer for the list.
	 */
	@SuppressWarnings("unchecked")
	public void setCellRenderer(ListCellRenderer renderer)
	{
		list.setCellRenderer(renderer);
	}

	/**
	 * @return the currently selected item, or {@code null} if none
	 */
	public E getSelectedItem()
	{
		return list.getSelectedValue();
	}

	/**
	 * Programmatically select an item in the list.
	 * If the item is present in the current (filtered) model, it is selected
	 * and scrolled into view. Otherwise the selection is cleared.
	 *
	 * @param item the item to select
	 */
	public void setSelectedItem(E item)
	{
		if (item == null)
		{
			list.clearSelection();
			return;
		}
		for (int i = 0; i < listModel.getSize(); i++)
		{
			if (item.equals(listModel.getElementAt(i)))
			{
				list.setSelectedIndex(i);
				list.ensureIndexIsVisible(i);
				return;
			}
		}
		list.clearSelection();
	}

	public void addListSelectionListener(ListSelectionListener listener)
	{
		list.addListSelectionListener(listener);
	}

	public void removeListSelectionListener(ListSelectionListener listener)
	{
		list.removeListSelectionListener(listener);
	}

	/**
	 * @return the filter text field, for focus management
	 */
	public JTextField getFilterField()
	{
		return filterField;
	}

	/**
	 * @return the internal JList, for adding FocusListeners
	 */
	public JList<E> getList()
	{
		return list;
	}

	private boolean acceptFilter(Object context, E element)
	{
		String searchText = filterField.getText();
		if (searchText == null || searchText.isEmpty())
		{
			return true;
		}
		if (StringUtils.containsIgnoreCase(element.toString(), searchText))
		{
			return true;
		}
		if (element instanceof InfoFacade)
		{
			String typeStr = ((InfoFacade) element).getType();
			if (StringUtils.containsIgnoreCase(typeStr, searchText))
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public void insertUpdate(DocumentEvent e)
	{
		refilter();
	}

	@Override
	public void removeUpdate(DocumentEvent e)
	{
		refilter();
	}

	@Override
	public void changedUpdate(DocumentEvent e)
	{
		refilter();
	}

	private void refilter()
	{
		E previousSelection = list.getSelectedValue();
		filteredFacade.refilter();
		if (previousSelection != null)
		{
			setSelectedItem(previousSelection);
		}
	}

}
