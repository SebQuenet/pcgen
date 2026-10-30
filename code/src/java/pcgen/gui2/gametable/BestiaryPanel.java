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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import pcgen.core.Campaign;
import pcgen.core.gametable.BestiaryPawn;
import pcgen.core.gametable.CombatantSheets;
import pcgen.core.gametable.EncounterMonster;
import pcgen.core.gametable.ForgeOutcome;
import pcgen.core.gametable.MonsterForge;
import pcgen.core.gametable.MonsterPreview;
import pcgen.core.gametable.MonsterRecipe;
import pcgen.core.gametable.SheetPreview;
import pcgen.core.gametable.UniqueCreature;
import pcgen.core.gametable.catalog.BestiaryIndex;
import pcgen.core.gametable.catalog.BestiaryPreferences;
import pcgen.core.gametable.catalog.Catalog;
import pcgen.core.gametable.catalog.CatalogAssembler;
import pcgen.core.gametable.catalog.CatalogEntry;
import pcgen.core.gametable.catalog.CatalogField;
import pcgen.core.gametable.catalog.DirectFacts;
import pcgen.core.gametable.catalog.EntryFacts;
import pcgen.core.gametable.catalog.EntryKind;
import pcgen.core.gametable.catalog.FieldValue;
import pcgen.core.gametable.catalog.LocalIndexer;
import pcgen.facade.core.DataSetFacade;
import pcgen.facade.util.ListFacades;
import pcgen.session.HeadlessUIDelegate;
import pcgen.system.ConfigurationSettings;
import pcgen.system.LanguageBundle;
import pcgen.system.PCGenSettings;

/**
 * The bestiary: the races and NPC kits of the loaded sources, the character files of the NPC folders and the
 * scenario's unique creatures, filtered on any value of their sheet. An entry goes to the selected encounter, into
 * the fight, or its pawn to Tabletop Simulator.
 */
final class BestiaryPanel extends JPanel
{
	private static final int MOST_AT_ONCE = 99;
	private static final String INDEX_FILE = "gametable-bestiary-index.json";

	private final GameTablePanel table;
	private final BestiaryPreferences preferences = BestiaryPreferences.ofSettings();
	private final CatalogBrowser browser;
	private final JEditorPane preview = new JEditorPane("text/html", "");
	private final SpinnerNumberModel count = new SpinnerNumberModel(1, 1, MOST_AT_ONCE, 1);
	private List<CatalogEntry> dataEntries = List.of();
	private Catalog catalog = Catalog.of(List.of());
	private Optional<LocalIndexer> indexer = Optional.empty();
	private BestiaryIndex delivered;
	private final Map<UniqueCreature, EntryFacts> uniqueFacts = new HashMap<>();

	BestiaryPanel(GameTablePanel table)
	{
		super(new BorderLayout());
		this.table = table;
		browser = new CatalogBrowser(preferences);
		preview.setEditable(false);
		preview.addHyperlinkListener(GameTablePanel::browse);
		browser.onSelection(this::showPreview);

		JPanel right = new JPanel(new BorderLayout());
		right.add(new JScrollPane(preview), BorderLayout.CENTER);
		JPanel actions = new JPanel(new GridLayout(2, 1));
		JPanel send = new JPanel(new FlowLayout(FlowLayout.LEFT));
		send.add(new JLabel(LanguageBundle.getString("in_gametable_count")));
		send.add(new JSpinner(count));
		send.add(GameTablePanel.button("in_gametable_toEncounter", this::toEncounter));
		send.add(GameTablePanel.button("in_gametable_toFight", this::toFight));
		PawnButtons.of(table, this, this::pickedPawn).forEach(send::add);
		send.add(GameTablePanel.button("in_gametable_retouchUnique", this::retouch));
		JPanel settings = new JPanel(new FlowLayout(FlowLayout.LEFT));
		settings.add(GameTablePanel.button("in_gametable_chooseColumns", this::chooseColumns));
		settings.add(GameTablePanel.button("in_gametable_npcFolders", this::chooseNpcFolders));
		settings.add(GameTablePanel.button("in_gametable_refreshBestiary", this::refreshCatalog));
		actions.add(send);
		actions.add(settings);
		right.add(actions, BorderLayout.SOUTH);

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, browser, right);
		split.setResizeWeight(0.6);
		add(split, BorderLayout.CENTER);
	}

	/**
	 * Lists the races and kits of the sources loaded now, and indexes the NPC files on them.
	 *
	 * @param campaigns the sources loaded; none when they were unloaded.
	 */
	void refreshSources(Collection<Campaign> campaigns)
	{
		indexer.ifPresent(LocalIndexer::cancel);
		indexer = Optional.empty();
		if (campaigns.isEmpty())
		{
			dataEntries = List.of();
			showCatalog();
			return;
		}
		HeadlessUIDelegate choices = new HeadlessUIDelegate();
		choices.setAutoChoose(true);
		MonsterForge forge = new MonsterForge(campaigns, choices);
		indexer = Optional.of(new LocalIndexer(forge, new CombatantSheets(forge.builder()),
			Path.of(ConfigurationSettings.getSettingsDir(), INDEX_FILE), table.session()));
		BestiaryIndex known = delivered;
		table.inBackground(() -> new Listed(CatalogAssembler.ofLoadedData(), known == null ? BestiaryIndex.delivered() : known),
			listed -> {
				dataEntries = listed.entries();
				delivered = listed.delivered();
				refreshCatalog();
			});
	}

	private record Listed(List<CatalogEntry> entries, BestiaryIndex delivered)
	{
	}

	/**
	 * Lists the NPC files and the scenario's unique creatures again, and brings their index up to date.
	 */
	void refreshCatalog()
	{
		List<Path> files = CatalogAssembler.characterFiles(preferences.npcFolders());
		showCatalog(files);
		List<UniqueCreature> uncomputed = table.session().scenario().uniques().stream()
			.filter(unique -> !uniqueFacts.containsKey(unique)).toList();
		indexer.filter(running -> !uncomputed.isEmpty()).ifPresent(running -> table.inBackground(() -> {
			Map<UniqueCreature, EntryFacts> computed = new HashMap<>();
			uncomputed.forEach(unique -> computed.put(unique, running.factsOf(unique.recipe(), uniqueNamed(unique))));
			return computed;
		}, computed -> {
			uniqueFacts.putAll(computed);
			showCatalog(files);
		}));
		indexer.ifPresent(running -> running.start(files,
			done -> SwingUtilities.invokeLater(() -> browser.setIndexing(
				LanguageBundle.getFormattedString("in_gametable_indexing", done, files.size()))),
			run -> SwingUtilities.invokeLater(() -> {
				browser.setIndexing("");
				if (run.built() > 0)
				{
					showCatalog(files);
				}
			})));
	}

	private static Map<CatalogField, FieldValue> uniqueNamed(UniqueCreature unique)
	{
		return Map.of(CatalogField.NAME, new FieldValue.Text(unique.name()), CatalogField.ORIGIN, new FieldValue.Text("scenario"),
			CatalogField.CATEGORY, new FieldValue.Text(DirectFacts.NPC));
	}

	private void showCatalog()
	{
		showCatalog(List.of());
	}

	private void showCatalog(List<Path> files)
	{
		List<CatalogEntry> entries = new ArrayList<>(dataEntries);
		entries.addAll(CatalogAssembler.ofFiles(files));
		for (UniqueCreature unique : table.session().scenario().uniques())
		{
			entries.add(new CatalogEntry(unique.name(), unique.recipe(), EntryKind.UNIQUE,
				uniqueFacts.getOrDefault(unique, EntryFacts.none().with(uniqueNamed(unique)))));
		}
		BestiaryIndex index = delivered == null ? BestiaryIndex.empty() : delivered;
		catalog = Catalog.of(entries).withIndex(indexer.map(running -> index.merged(running.current())).orElse(index));
		browser.setCatalog(catalog);
	}

	private Optional<CatalogEntry> picked()
	{
		List<CatalogEntry> selected = browser.selected();
		return selected.isEmpty() ? Optional.empty() : Optional.of(selected.getFirst());
	}

	private Optional<BestiaryPawn> pickedPawn()
	{
		return picked().flatMap(entry -> BestiaryPawn.of(entry.recipe().source()));
	}

	private void showPreview()
	{
		preview.setText("");
		picked().ifPresent(entry -> table.inBackground(() -> table.session().previewSheet(entry.recipe()), outcome -> {
			switch (outcome)
			{
				case ForgeOutcome.Built<SheetPreview> built ->
				{
					preview.setText(SheetHtml.preview(built.value().sheet(), BestiaryPawn.of(entry.recipe().source()),
						TacticsHtml.of(built.value().monster())));
					// Browsing the bestiary builds thousands of creatures: each is let go once shown
					MonsterForge.release(built.value().monster());
				}
				case ForgeOutcome.Failed<SheetPreview> failed -> preview.setText(EncountersPanel.escape(failed.reason()));
			}
			preview.setCaretPosition(0);
		}));
	}

	private List<EncounterMonster> chosenMonsters(List<CatalogEntry> entries, int number)
	{
		return entries.stream().map(entry -> new EncounterMonster(entry.name(), entry.recipe(), number)).toList();
	}

	private void toEncounter()
	{
		for (EncounterMonster monster : chosenMonsters(browser.selected(), count.getNumber().intValue()))
		{
			if (!table.encounters().addToSelected(monster))
			{
				table.showError(LanguageBundle.getString("in_gametable_pickEncounterFirst"));
				return;
			}
		}
	}

	private void toFight()
	{
		List<EncounterMonster> monsters = chosenMonsters(browser.selected(), count.getNumber().intValue());
		if (monsters.isEmpty())
		{
			return;
		}
		table.inBackground(() -> {
			List<String> problems = new ArrayList<>();
			monsters.forEach(monster -> problems.addAll(table.session().addMonsters(monster.name(), monster.recipe(),
				monster.count())));
			return problems;
		}, problems -> {
			table.reportProblems(problems);
			table.showCombat();
		});
	}

	private void chooseColumns()
	{
		List<CatalogField> shown = preferences.columns();
		JPanel boxes = new JPanel(new GridLayout(0, 3));
		List<JCheckBox> fields = new ArrayList<>();
		for (CatalogField field : CatalogField.values())
		{
			JCheckBox box = new JCheckBox(LanguageBundle.getString(field.labelKey()), shown.contains(field));
			fields.add(box);
			boxes.add(box);
		}
		if (JOptionPane.showConfirmDialog(this, new JScrollPane(boxes), LanguageBundle.getString("in_gametable_chooseColumns"),
			JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
		{
			return;
		}
		List<CatalogField> chosen = new ArrayList<>();
		for (int index = 0; index < fields.size(); index++)
		{
			if (fields.get(index).isSelected())
			{
				chosen.add(CatalogField.values()[index]);
			}
		}
		if (!chosen.isEmpty())
		{
			preferences.saveColumns(chosen);
			browser.setColumns(chosen);
		}
	}

	private void chooseNpcFolders()
	{
		DefaultListModel<Path> folders = new DefaultListModel<>();
		preferences.npcFolders().forEach(folders::addElement);
		JList<Path> list = new JList<>(folders);
		JPanel editor = new JPanel(new BorderLayout());
		editor.add(new JScrollPane(list), BorderLayout.CENTER);
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
		buttons.add(GameTablePanel.button("in_gametable_addFolder", () -> {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
			{
				folders.addElement(chooser.getSelectedFile().toPath());
			}
		}));
		buttons.add(GameTablePanel.button("in_gametable_removeFolder", () -> list.getSelectedValuesList()
			.forEach(folders::removeElement)));
		editor.add(buttons, BorderLayout.SOUTH);
		editor.setPreferredSize(new Dimension(520, 260));
		if (JOptionPane.showConfirmDialog(this, editor, LanguageBundle.getString("in_gametable_npcFolders"),
			JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
		{
			List<Path> chosen = new ArrayList<>();
			folders.elements().asIterator().forEachRemaining(chosen::add);
			preferences.saveNpcFolders(chosen);
			refreshCatalog();
		}
	}

	private void retouch()
	{
		picked().ifPresent(entry -> table.inBackground(
			() -> new Retouching(table.session().templateKeysOf(entry.recipe()), table.session().compatibleTemplates(entry.recipe())),
			retouching -> {
				switch (retouching.current())
				{
					case ForgeOutcome.Failed<List<String>> failed -> table.showError(failed.reason());
					case ForgeOutcome.Built<List<String>> current ->
					{
						Map<String, String> offered = retouching.compatible() instanceof ForgeOutcome.Built<Map<String, String>> built
							? built.value() : CombatantSheets.templates();
						RetouchDialog.editUnique(this, entry.recipe(), current.value(), offered, entry.name(), this::summary)
							.ifPresent(this::keep);
					}
				}
			}));
	}

	private record Retouching(ForgeOutcome<List<String>> current, ForgeOutcome<Map<String, String>> compatible)
	{
	}

	private String summary(MonsterRecipe recipe)
	{
		return switch (table.session().preview(recipe))
		{
			case ForgeOutcome.Built<MonsterPreview> built -> LanguageBundle.getFormattedString("in_gametable_uniqueSummary",
				built.value().stats().maximumHitPoints(), built.value().challengeRating());
			case ForgeOutcome.Failed<MonsterPreview> failed -> failed.reason();
		};
	}

	private void keep(RetouchDialog.Unique unique)
	{
		Object[] choices = {LanguageBundle.getString("in_gametable_keepInScenario"),
			LanguageBundle.getString("in_gametable_saveAsCharacter"), LanguageBundle.getString("in_gametable_cancel")};
		int choice = JOptionPane.showOptionDialog(this, LanguageBundle.getFormattedString("in_gametable_keepUnique",
			unique.name()), LanguageBundle.getString("in_gametable_title"), JOptionPane.DEFAULT_OPTION,
			JOptionPane.QUESTION_MESSAGE, null, choices, choices[0]);
		if (choice == 0)
		{
			table.session().addUnique(new UniqueCreature(unique.name(), unique.recipe()));
			refreshCatalog();
		}
		else if (choice == 1)
		{
			saveAsCharacter(unique);
		}
	}

	private void saveAsCharacter(RetouchDialog.Unique unique)
	{
		DataSetFacade dataSet = table.frame().getLoadedDataSetRef().get();
		if (dataSet == null)
		{
			return;
		}
		List<Path> folders = preferences.npcFolders();
		File folder = folders.isEmpty() ? new File(PCGenSettings.getPcgDir()) : folders.getFirst().toFile();
		JFileChooser chooser = new JFileChooser(folder);
		chooser.setFileFilter(new FileNameExtensionFilter(LanguageBundle.getString("in_gametable_characterFiles"), "pcg"));
		chooser.setSelectedFile(new File(folder, unique.name() + ".pcg"));
		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}
		File target = chooser.getSelectedFile();
		List<Campaign> campaigns = ListFacades.wrap(dataSet.getCampaigns());
		table.inBackground(() -> table.session().saveRecipe(unique.recipe(), unique.name(), target, dataSet.getGameMode(),
			campaigns), failure -> failure.ifPresentOrElse(table::showError, this::refreshCatalog));
	}

	/**
	 * @param parent the component the dialog opens over.
	 * @return the entry and number the game master picks in the bestiary; empty when cancelled.
	 */
	Optional<EncounterMonster> pick(Component parent)
	{
		JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
			LanguageBundle.getString("in_gametable_bestiary"), JDialog.ModalityType.APPLICATION_MODAL);
		CatalogBrowser picker = new CatalogBrowser(preferences);
		picker.setCatalog(catalog);
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
		dialog.setPreferredSize(new Dimension(900, 640));
		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
		if (!accepted[0] || picker.selected().isEmpty())
		{
			return Optional.empty();
		}
		CatalogEntry entry = picker.selected().getFirst();
		return Optional.of(new EncounterMonster(entry.name(), entry.recipe(), number.getNumber().intValue()));
	}
}
