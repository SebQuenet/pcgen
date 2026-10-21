/*
 * Copyright 2008 Connor Petty <cpmeister@users.sourceforge.net>
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
 * 
 */
package pcgen.gui2.tabs;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import pcgen.core.AbilityCategory;
import pcgen.facade.core.AbilityFacade;
import pcgen.facade.core.CharacterFacade;
import pcgen.facade.util.DefaultListFacade;
import pcgen.facade.util.ListFacade;
import pcgen.facade.util.MapFacade;
import pcgen.facade.util.event.ListEvent;
import pcgen.facade.util.event.ListListener;
import pcgen.gui2.util.SharedTabPane;
import pcgen.system.LanguageBundle;
import pcgen.util.Logging;
import pcgen.util.enumeration.Tab;

/**
 * This component is the tabbed pane which contains all of the
 * AbilityChooserTabs. This component doesn't actually display any character
 * information, that is the job of the AbilityChooserTab. All this class does is
 * manage the states of all the AbilityChooserTab.
 *
 * @see AbilityChooserTab
 */
@SuppressWarnings("serial")
public class AbilitiesInfoTab extends SharedTabPane implements CharacterInfoTab, TodoHandler
{

	private final AbilityChooserTab abilityTab;
	private final TabTitle tabTitle;

	private void addAbilityTab(String type)
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setName(type);
		addTab(LanguageBundle.getTerm("in_abilityLocation_", type), panel);
	}

	private int indexOfAbilityTab(String type)
	{
		for (int i = 0; i < getTabCount(); i++)
		{
			if (type.equals(getComponentAt(i).getName()))
			{
				return i;
			}
		}
		return -1;
	}

	public AbilitiesInfoTab()
	{
		this.abilityTab = new AbilityChooserTab();
		this.tabTitle = new TabTitle(Tab.ABILITIES);
		setSharedComponent(abilityTab);
	}

	@Override
	public ModelMap createModels(CharacterFacade character)
	{
		ModelMap models = new ModelMap();
		models.put(AbilityTabsModel.class, new AbilityTabsModel(character));
		return models;
	}

	@Override
	public void storeModels(ModelMap models)
	{
		models.get(AbilityTabsModel.class).uninstall();
	}

	@Override
	public void restoreModels(ModelMap models)
	{
		models.get(AbilityTabsModel.class).install();
	}

	@Override
	public TabTitle getTabTitle()
	{
		return tabTitle;
	}

	private class AbilityTabsModel implements ListListener<AbilityCategory>, ChangeListener
	{

		private final Map<String, TabInfo> typeMap = new HashMap<>();
		private final List<TabInfo> tabs = new ArrayList<>();
		private final MapFacade<AbilityCategory, ListFacade<AbilityFacade>> categoryMap;
		private final CharacterFacade character;
		private boolean isInstalled = false;
		private String selectedTitle;
		private final ListFacade<AbilityCategory> activeCategories;

		public AbilityTabsModel(CharacterFacade character)
		{
			this.character = character;
			this.activeCategories = character.getActiveAbilityCategories();
			this.categoryMap = character.getDataSet().getAbilities();
			synchronizeTabs();
			selectedTitle = tabs.get(0).title;
		}

		/**
		 * Populate the fullCategoryList for a TabInfo object with all 
		 * categories of the specified type.
		 * 
		 * @param type The type to be scanned for.
		 * @param tabInfo The TabInfo to be populated.
		 */
		private void populateFullCategoryList(String type, TabInfo tabInfo)
		{
			for (AbilityCategory category : categoryMap.getKeys())
			{
				if (type.equals(category.getType()))
				{
					tabInfo.fullCategoryList.addElement(category);
				}
			}
		}

		/**
		 * Rebuild the tab list from the character's active categories, one tab
		 * per category type, in the order of the categories. The model only
		 * listens while installed, so it may have missed changes.
		 */
		private void synchronizeTabs()
		{
			Map<String, List<AbilityCategory>> categoriesByType = new LinkedHashMap<>();
			for (AbilityCategory category : activeCategories)
			{
				categoriesByType.computeIfAbsent(category.getType(), type -> new ArrayList<>()).add(category);
			}
			tabs.clear();
			typeMap.keySet().retainAll(categoriesByType.keySet());
			for (Map.Entry<String, List<AbilityCategory>> entry : categoriesByType.entrySet())
			{
				String type = entry.getKey();
				TabInfo tabInfo = typeMap.get(type);
				if (tabInfo == null)
				{
					tabInfo = new TabInfo(type, character);
					populateFullCategoryList(type, tabInfo);
				}
				for (AbilityCategory category : tabInfo.categoryList.getContents())
				{
					if (!entry.getValue().contains(category))
					{
						tabInfo.categoryList.removeElement(category);
					}
				}
				for (AbilityCategory category : entry.getValue())
				{
					if (!tabInfo.categoryList.containsElement(category))
					{
						tabInfo.categoryList.addElement(category);
					}
				}
				tabs.add(tabInfo);
			}
		}

		private List<String> displayedTitles()
		{
			List<String> titles = new ArrayList<>();
			for (int i = 0; i < getTabCount(); i++)
			{
				titles.add(getComponentAt(i).getName());
			}
			return titles;
		}

		private void displayTabs()
		{
			for (TabInfo tabInfo : tabs)
			{
				addAbilityTab(tabInfo.title);
			}
			if (!typeMap.containsKey(selectedTitle) && !tabs.isEmpty())
			{
				selectedTitle = tabs.get(0).title;
			}
			setSelectedIndex(indexOfAbilityTab(selectedTitle));
			TabInfo selectedTab = typeMap.get(selectedTitle);
			if (selectedTab != null)
			{
				abilityTab.restoreState(selectedTab.tabData);
			}
		}

		private void refreshTabs()
		{
			TabInfo selectedTab = typeMap.get(selectedTitle);
			synchronizeTabs();
			List<String> titles = new ArrayList<>();
			for (TabInfo tabInfo : tabs)
			{
				titles.add(tabInfo.title);
			}
			if (!isInstalled || titles.equals(displayedTitles()))
			{
				return;
			}
			if (selectedTab != null)
			{
				abilityTab.storeState(selectedTab.tabData);
			}
			removeChangeListener(this);
			removeAll();
			displayTabs();
			addChangeListener(this);
		}

		@Override
		public void elementAdded(ListEvent<AbilityCategory> e)
		{
			refreshTabs();
		}

		@Override
		public void elementRemoved(ListEvent<AbilityCategory> e)
		{
			refreshTabs();
		}

		@Override
		public void elementsChanged(ListEvent<AbilityCategory> e)
		{
			refreshTabs();
		}

		@Override
		public void elementModified(ListEvent<AbilityCategory> e)
		{
			//TODO: do something
		}

		public void install()
		{
			activeCategories.addListListener(this);
			synchronizeTabs();
			displayTabs();
			addChangeListener(this);
			isInstalled = true;
		}

		public void uninstall()
		{
			TabInfo selectedTab = typeMap.get(selectedTitle);
			if (selectedTab != null)
			{
				abilityTab.storeState(selectedTab.tabData);
			}
			removeChangeListener(this);
			removeAll();
			activeCategories.removeListListener(this);
			isInstalled = false;
		}

		@Override
		public void stateChanged(ChangeEvent e)
		{
			TabInfo tabInfo = typeMap.get(selectedTitle);
			if (tabInfo != null)
			{
				abilityTab.storeState(tabInfo.tabData);
			}
			if (getSelectedIndex() != -1)
			{
				selectedTitle = getComponentAt(getSelectedIndex()).getName();
				if (typeMap.get(selectedTitle) == null)
				{
					Logging.errorPrint("Selected tab " + selectedTitle + " at index " + getSelectedIndex()
						+ " but there is no typeMap entry for it.");
					Logging.reportAllThreads();
				}
				abilityTab.restoreState(typeMap.get(selectedTitle).tabData);
			}
		}

		@SuppressWarnings({"UseOfObsoleteCollectionType", "PMD.ReplaceHashtableWithMap", "serial"})
		private final class TabInfo
		{
			public final String title;
			private final Hashtable<Object, Object> tabData;
			private final DefaultListFacade<AbilityCategory> categoryList;
			private final DefaultListFacade<AbilityCategory> fullCategoryList;

			private TabInfo(String title, CharacterFacade character)
			{
				this.title = title;
				this.categoryList = new DefaultListFacade<>();
				this.fullCategoryList = new DefaultListFacade<>();
				this.tabData = abilityTab.createState(character, categoryList, fullCategoryList, title);
				typeMap.put(title, this);
			}

			@SuppressWarnings("nls")
			@Override
			public String toString()
			{
				return "TabInfo [title=" + title + ", categoryList=" + categoryList + "]";
			}

		}

		@SuppressWarnings("nls")
		@Override
		public String toString()
		{
			return "AbilityTabsModel [tabs=" + tabs + ", isInstalled=" + isInstalled + ", selectedTitle="
				+ selectedTitle + "]";
		}

	}

	@Override
	public void adviseTodo(String fieldName)
	{
		abilityTab.adviseTodo(fieldName);
	}

}
