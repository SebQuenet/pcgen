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
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import pcgen.core.Campaign;
import pcgen.core.gametable.Combat;
import pcgen.core.gametable.Combatant;
import pcgen.core.gametable.CombatantKind;
import pcgen.core.gametable.CombatReward;
import pcgen.core.gametable.Expiry;
import pcgen.core.gametable.ForgeOutcome;
import pcgen.core.gametable.Initiative;
import pcgen.core.gametable.MonsterRecipe;
import pcgen.core.gametable.MonsterSource;
import pcgen.core.gametable.Turn;
import pcgen.facade.core.DataSetFacade;
import pcgen.facade.util.ListFacades;
import pcgen.system.LanguageBundle;
import pcgen.system.PCGenSettings;

/**
 * The fight: the order of initiative on the left, the sheet of the combatant picked on
 * the right, and every common action one key away.
 */
final class CombatPanel extends JPanel
{
	private static final Color CURRENT_TURN = new Color(255, 243, 196);
	private static final Color DOWN = new Color(170, 40, 40);

	private final GameTablePanel table;
	private final RowsModel rows = new RowsModel();
	private final JTable order = new JTable(rows);
	private final JEditorPane sheet = new JEditorPane("text/html", "");
	private final JLabel round = new JLabel();
	private final JLabel notice = new JLabel(" ");
	private final JButton startOrNext;
	private final JButton retouchButton;
	private final JButton openButton;
	private int shownId = -1;

	CombatPanel(GameTablePanel table)
	{
		super(new BorderLayout());
		this.table = table;
		startOrNext = GameTablePanel.button("in_gametable_next", this::next);

		JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
		round.setFont(round.getFont().deriveFont(Font.BOLD));
		bar.add(round);
		bar.add(startOrNext);
		bar.add(GameTablePanel.button("in_gametable_damage", this::damage));
		bar.add(GameTablePanel.button("in_gametable_heal", this::heal));
		bar.add(GameTablePanel.button("in_gametable_condition", this::condition));
		bar.add(GameTablePanel.button("in_gametable_castSpell", this::castSpell));
		bar.add(GameTablePanel.button("in_gametable_initiative", this::initiative));
		bar.add(GameTablePanel.button("in_gametable_delay", this::delayOrResume));
		bar.add(GameTablePanel.button("in_gametable_conjure", this::conjure));
		// A second row keeps every action in sight on a laptop screen
		JPanel manage = new JPanel(new FlowLayout(FlowLayout.LEFT));
		manage.add(GameTablePanel.button("in_gametable_addPlayer", this::addPlayer));
		manage.add(GameTablePanel.button("in_gametable_addMonster", this::addMonster));
		manage.add(GameTablePanel.button("in_gametable_remove", this::removeCombatant));
		manage.add(GameTablePanel.button("in_gametable_endCombat", this::endCombat));
		JPanel bars = new JPanel(new GridLayout(2, 1));
		bars.add(bar);
		bars.add(manage);
		add(bars, BorderLayout.NORTH);

		// Several rows picked take a condition at once
		order.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		order.setRowHeight(order.getRowHeight() + 6);
		order.setDefaultRenderer(Object.class, new RowRenderer());
		order.getColumnModel().getColumn(0).setMaxWidth(70);
		order.getSelectionModel().addListSelectionListener(event -> {
			if (!event.getValueIsAdjusting())
			{
				showSheet();
			}
		});
		DragToReorder drag = new DragToReorder();
		order.addMouseListener(drag);

		sheet.setEditable(false);
		sheet.addHyperlinkListener(event -> {
			if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED
				&& event.getDescription().startsWith(SheetHtml.REMOVE_LINK))
			{
				String condition = URLDecoder.decode(event.getDescription().substring(SheetHtml.REMOVE_LINK.length()),
					StandardCharsets.UTF_8);
				target().ifPresent(combatant -> table.session().removeCondition(combatant.id(), condition));
			}
			else if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED
				&& event.getDescription().startsWith(SheetHtml.DROP_SPELL_LINK))
			{
				String spell = URLDecoder.decode(event.getDescription().substring(SheetHtml.DROP_SPELL_LINK.length()),
					StandardCharsets.UTF_8);
				target().ifPresent(combatant -> table.session().dropSpell(combatant.id(), spell));
			}
			else if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED
				&& event.getDescription().startsWith(SheetHtml.REMOVE_FROM_GROUP_LINK))
			{
				String condition = URLDecoder.decode(
					event.getDescription().substring(SheetHtml.REMOVE_FROM_GROUP_LINK.length()), StandardCharsets.UTF_8);
				target().ifPresent(combatant -> table.session().removeCondition(
					SheetHtml.groupBearing(combatant, table.session().combat(), condition), condition));
			}
		});
		sheet.addHyperlinkListener(GameTablePanel::browse);
		JPanel right = new JPanel(new BorderLayout());
		right.add(new JScrollPane(sheet), BorderLayout.CENTER);
		JPanel sheetButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
		retouchButton = GameTablePanel.button("in_gametable_retouch", this::retouch);
		openButton = GameTablePanel.button("in_gametable_openInCharacters", this::openInCharacters);
		sheetButtons.add(retouchButton);
		sheetButtons.add(openButton);
		right.add(sheetButtons, BorderLayout.SOUTH);

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(order), right);
		split.setResizeWeight(0.55);
		add(split, BorderLayout.CENTER);
		add(notice, BorderLayout.SOUTH);
		bindKeys();
	}

	private void bindKeys()
	{
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "next", this::next);
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "damage", this::damage);
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), "heal", this::heal);
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0), "condition", this::condition);
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0), "castSpell", this::castSpell);
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_I, 0), "initiative", this::initiative);
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "delay", this::delayOrResume);
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0), "conjure", this::conjure);
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "remove", this::removeCombatant);
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK), "moveUp", () -> moveSelected(-1));
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK), "moveDown", () -> moveSelected(1));
	}

	private void bind(KeyStroke key, String name, Runnable action)
	{
		AbstractAction bound = new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				action.run();
			}
		};
		for (JComponent component : List.of(order, this))
		{
			InputMap keys = component == order ? order.getInputMap(JComponent.WHEN_FOCUSED)
				: getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
			keys.put(key, name);
			component.getActionMap().put(name, bound);
		}
	}

	void refresh()
	{
		Combat combat = table.session().combat();
		Optional<Integer> selected = selectedCombatant().map(Combatant::id);
		List<Integer> alsoSelected = selectedCombatants().stream().map(Combatant::id).toList();
		rows.show(combat);
		round.setText(combat.isStarted() ? LanguageBundle.getFormattedString("in_gametable_round", combat.round())
			: LanguageBundle.getString("in_gametable_notStarted"));
		startOrNext.setText(LanguageBundle.getString(combat.isStarted() ? "in_gametable_next" : "in_gametable_start"));
		int row = selected.map(rows::rowOf).orElse(-1);
		if (row < 0)
		{
			row = rows.currentRow();
		}
		order.clearSelection();
		for (int id : alsoSelected)
		{
			int other = rows.rowOf(id);
			if (other >= 0 && other != row)
			{
				order.getSelectionModel().addSelectionInterval(other, other);
			}
		}
		if (row >= 0)
		{
			// Added last, the row the sheet shows stays the lead of the selection
			order.getSelectionModel().addSelectionInterval(row, row);
		}
		showSheet();
	}

	private Optional<Combatant> selectedCombatant()
	{
		int lead = order.getSelectionModel().getLeadSelectionIndex();
		int row = order.isRowSelected(lead) ? lead : order.getSelectedRow();
		return (row < 0 || row >= rows.rows.size()) ? Optional.empty() : Optional.of(rows.rows.get(row).combatant());
	}

	/** Every combatant picked, once each though acting twice a round. */
	private List<Combatant> selectedCombatants()
	{
		return Arrays.stream(order.getSelectedRows()).filter(row -> row < rows.rows.size())
			.mapToObj(row -> rows.rows.get(row).combatant()).distinct().toList();
	}

	/** The combatant an action applies to: the one picked, else the one acting. */
	private Optional<Combatant> target()
	{
		Optional<Combatant> picked = selectedCombatant();
		if (picked.isPresent())
		{
			return picked;
		}
		Combat combat = table.session().combat();
		return combat.current().flatMap(turn -> combat.combatant(turn.combatantId()));
	}

	private void showSheet()
	{
		Optional<Combatant> combatant = target();
		boolean monster = combatant.map(found -> found.kind().isGameMasters()).orElse(false);
		retouchButton.setEnabled(monster);
		openButton.setEnabled(monster);
		if (combatant.isEmpty())
		{
			sheet.setText("");
			shownId = -1;
			return;
		}
		Combatant found = combatant.get();
		Combat combat = table.session().combat();
		String html = monster ? SheetHtml.monster(found, table.session().monsterSheet(found.id()), combat,
				table.session().monster(found.id()).map(TacticsHtml::of).orElse(""))
			: SheetHtml.player(found, table.session().glance(found.id()), combat);
		boolean sameCombatant = shownId == found.id();
		Rectangle visible = sheet.getVisibleRect();
		sheet.setText(html);
		shownId = found.id();
		if (sameCombatant)
		{
			sheet.scrollRectToVisible(visible);
		}
		else
		{
			sheet.setCaretPosition(0);
		}
	}

	private void next()
	{
		if (!table.session().combat().isStarted())
		{
			table.session().start();
			order.clearSelection();
			notice.setText(" ");
			return;
		}
		List<Expiry> expired = table.session().next();
		order.clearSelection();
		refresh();
		notice.setText(expired.isEmpty() ? " " : expired.stream().map(CombatPanel::describe)
			.collect(Collectors.joining(" · ")));
	}

	private static String describe(Expiry expiry)
	{
		return switch (expiry)
		{
			case Expiry.CreatureLeft left -> LanguageBundle.getFormattedString("in_gametable_creatureLeft",
				left.combatant());
			case Expiry.ConditionEnded ended -> LanguageBundle.getFormattedString("in_gametable_conditionEnded",
				ended.combatant(), ended.condition());
			case Expiry.SpellEnded ended -> LanguageBundle.getFormattedString("in_gametable_spellEnded",
				ended.combatant(), ended.spell());
		};
	}

	private void damage()
	{
		target().filter(combatant -> combatant.health().isPresent()).ifPresent(combatant -> askNumber(this,
			LanguageBundle.getFormattedString("in_gametable_damageTo", combatant.name()), 0)
			.filter(amount -> amount > 0).ifPresent(amount -> table.session().damage(combatant.id(), amount)));
	}

	private void heal()
	{
		target().filter(combatant -> combatant.health().isPresent()).ifPresent(combatant -> askNumber(this,
			LanguageBundle.getFormattedString("in_gametable_healTo", combatant.name()), 0)
			.filter(amount -> amount > 0).ifPresent(amount -> table.session().heal(combatant.id(), amount)));
	}

	private void condition()
	{
		target().ifPresent(combatant -> ConditionDialog.ask(this, LanguageBundle.getString("in_gametable_condition"),
			scopesAround(combatant),
			table.session()::conditionChoices)
			.ifPresent(laying -> table.session().addCondition(laying.scope().ids(), laying.condition())));
	}

	/**
	 * Keeps track of a spell cast by the combatant acting, else the one picked, and lays its
	 * effect on its targets until it ends.
	 */
	private void castSpell()
	{
		Combat combat = table.session().combat();
		Optional<Combatant> acting = combat.current().flatMap(turn -> combat.combatant(turn.combatantId()));
		acting.or(this::selectedCombatant).ifPresent(caster -> ConditionDialog.ask(this,
			LanguageBundle.getFormattedString("in_gametable_castSpellBy", caster.name()), spellScopes(caster),
			table.session()::conditionChoices)
			.ifPresent(laying -> table.session().castSpell(caster.id(), laying.condition(), laying.scope().ids())));
	}

	/** Whom a spell can bear on: those a condition can go on, its caster, the players' side, or no one. */
	private List<ConditionDialog.Scope> spellScopes(Combatant caster)
	{
		Combatant picked = selectedCombatant().orElse(caster);
		List<ConditionDialog.Scope> scopes = new ArrayList<>(scopesAround(picked));
		if (picked.id() != caster.id())
		{
			scopes.add(new ConditionDialog.Scope(LanguageBundle.getFormattedString("in_gametable_conditionOn",
				caster.name()), List.of(caster.id())));
		}
		List<Combatant> players = table.session().combat().order().stream()
			.filter(other -> !other.kind().isGameMasters()).toList();
		if (players.size() > 1)
		{
			scopes.add(new ConditionDialog.Scope(LanguageBundle.getFormattedString("in_gametable_conditionOnPlayers",
				players.size()), idsOf(players)));
		}
		scopes.add(new ConditionDialog.Scope(LanguageBundle.getString("in_gametable_spellUntargeted"), List.of()));
		return scopes;
	}

	/** The combatants a condition can go on: the one acted on, the rows picked, its group, every monster. */
	private List<ConditionDialog.Scope> scopesAround(Combatant combatant)
	{
		List<ConditionDialog.Scope> scopes = new ArrayList<>();
		scopes.add(new ConditionDialog.Scope(LanguageBundle.getFormattedString("in_gametable_conditionOn",
			combatant.name()), List.of(combatant.id())));
		List<Combatant> picked = selectedCombatants();
		if (picked.size() > 1)
		{
			// Several rows picked are what the game master meant, so they come first
			scopes.addFirst(new ConditionDialog.Scope(LanguageBundle.getFormattedString("in_gametable_conditionOnPicked",
				picked.size()), idsOf(picked)));
		}
		List<Combatant> order = table.session().combat().order();
		List<Combatant> group = order.stream().filter(other -> other.kind().isGameMasters()
			&& !combatant.group().isBlank() && other.group().equals(combatant.group())).toList();
		if (combatant.kind().isGameMasters() && group.size() > 1)
		{
			scopes.add(new ConditionDialog.Scope(LanguageBundle.getFormattedString("in_gametable_conditionOnGroup",
				combatant.group(), group.size()), idsOf(group)));
		}
		List<Combatant> monsters = order.stream().filter(other -> other.kind().isGameMasters()).toList();
		if (monsters.size() > 1 && monsters.size() != group.size())
		{
			scopes.add(new ConditionDialog.Scope(LanguageBundle.getFormattedString("in_gametable_conditionOnMonsters",
				monsters.size()), idsOf(monsters)));
		}
		return scopes;
	}

	private static List<Integer> idsOf(List<Combatant> combatants)
	{
		return combatants.stream().map(Combatant::id).toList();
	}

	private void initiative()
	{
		target().ifPresent(combatant -> askNumber(this,
			LanguageBundle.getFormattedString("in_gametable_initiativeOf", combatant.name()),
			combatant.initiative().score().orElse(0))
			.ifPresent(score -> table.session().setInitiative(combatant.id(), score)));
	}

	private void delayOrResume()
	{
		target().ifPresent(combatant -> {
			if (combatant.delaying())
			{
				table.session().resume(combatant.id());
			}
			else
			{
				table.session().delay(combatant.id());
			}
		});
	}

	private void conjure()
	{
		target().ifPresent(summoner -> SummonDialog.ask(this, summoner).ifPresent(request -> table.inBackground(
			() -> table.session().summon(summoner.id(), request.name(), request.recipe(), request.count(),
				request.rounds()), table::reportProblems)));
	}

	private void addPlayer()
	{
		String name = JOptionPane.showInputDialog(this, LanguageBundle.getString("in_gametable_memberName"));
		if (name != null && !name.isBlank())
		{
			table.session().addPlayer(name, Optional.empty());
		}
	}

	private void addMonster()
	{
		Object[] choices = {LanguageBundle.getString("in_gametable_addFromBestiary"),
			LanguageBundle.getString("in_gametable_addFromFile"), LanguageBundle.getString("in_gametable_cancel")};
		int choice = JOptionPane.showOptionDialog(this, LanguageBundle.getString("in_gametable_addMonster"),
			LanguageBundle.getString("in_gametable_title"), JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
			null, choices, choices[0]);
		if (choice == 0)
		{
			BestiaryPanel.pickRace(this).ifPresent(monster -> table.inBackground(
				() -> table.session().addMonsters(monster.name(), monster.recipe(), monster.count()),
				table::reportProblems));
		}
		else if (choice == 1)
		{
			PartyPanel.chooseCharacter(this).ifPresent(file -> table.inBackground(
				() -> table.session().addMonsters(file.getName().replaceFirst("\\.pcg$", ""),
					MonsterRecipe.of(new MonsterSource.CharacterFile(file.getAbsolutePath())), 1),
				table::reportProblems));
		}
	}

	private void removeCombatant()
	{
		target().ifPresent(combatant -> {
			if (JOptionPane.showConfirmDialog(this,
				LanguageBundle.getFormattedString("in_gametable_removeConfirm", combatant.name()),
				LanguageBundle.getString("in_gametable_title"), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
			{
				table.session().remove(combatant.id());
			}
		});
	}

	private void endCombat()
	{
		if (table.session().combat().order().isEmpty() || JOptionPane.showConfirmDialog(this,
			LanguageBundle.getString("in_gametable_endCombatConfirm"), LanguageBundle.getString("in_gametable_title"),
			JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
		{
			return;
		}
		int blocksBefore = table.session().scenario().loot().size();
		CombatReward reward = table.session().endCombat();
		notice.setText(" ");
		String message = LanguageBundle.getFormattedString("in_gametable_reward", reward.challengeRating().orElse("—"),
			reward.experience(), reward.perPlayer(), reward.players());
		if (table.session().scenario().loot().size() > blocksBefore)
		{
			message += "\n" + LanguageBundle.getString("in_gametable_lootAdded");
		}
		JOptionPane.showMessageDialog(this, message, LanguageBundle.getString("in_gametable_endCombat"),
			JOptionPane.INFORMATION_MESSAGE);
	}

	private void moveSelected(int step)
	{
		int row = order.getSelectedRow();
		if (row >= 0)
		{
			moveRow(row, row + step);
		}
	}

	private void moveRow(int fromRow, int toRow)
	{
		if (toRow < 0 || toRow >= rows.rows.size() || fromRow == toRow)
		{
			return;
		}
		Combatant moving = rows.rows.get(fromRow).combatant();
		Combatant there = rows.rows.get(toRow).combatant();
		List<Combatant> combatants = table.session().combat().order();
		int index = combatants.indexOf(there);
		table.session().move(moving.id(), index);
		order.getSelectionModel().setSelectionInterval(rows.rowOf(moving.id()), rows.rowOf(moving.id()));
	}

	private void retouch()
	{
		target().filter(combatant -> combatant.recipe().isPresent()).ifPresent(combatant -> {
			MonsterRecipe recipe = combatant.recipe().get();
			table.inBackground(() -> table.session().templateKeysOf(recipe), outcome -> {
				List<String> current = (outcome instanceof ForgeOutcome.Built<List<String>> built) ? built.value()
					: List.of();
				RetouchDialog.edit(this, recipe, current).ifPresent(touched -> table.inBackground(
					() -> table.session().retouch(combatant.id(), touched).map(List::of).orElse(List.of()),
					table::reportProblems));
			});
		});
	}

	private void openInCharacters()
	{
		target().filter(combatant -> combatant.recipe().isPresent()).ifPresent(combatant -> {
			MonsterSource source = combatant.recipe().get().source();
			if (source instanceof MonsterSource.CharacterFile file && combatant.recipe().get().addedTemplates().isEmpty()
				&& combatant.recipe().get().removedTemplates().isEmpty() && combatant.recipe().get().addedHitDice() == 0)
			{
				table.frame().showCharactersWorkspace();
				table.frame().loadCharacterFromFile(new File(file.path()));
				return;
			}
			DataSetFacade dataSet = table.frame().getLoadedDataSetRef().get();
			if (dataSet == null)
			{
				return;
			}
			JFileChooser chooser = new JFileChooser(PCGenSettings.getPcgDir());
			chooser.setFileFilter(new FileNameExtensionFilter(LanguageBundle.getString("in_gametable_characterFiles"),
				"pcg"));
			chooser.setSelectedFile(new File(PCGenSettings.getPcgDir(), combatant.name() + ".pcg"));
			if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
			{
				return;
			}
			File target = chooser.getSelectedFile();
			List<Campaign> campaigns = ListFacades.wrap(dataSet.getCampaigns());
			Optional<String> failure = table.session().saveMonster(combatant.id(), target, dataSet.getGameMode(),
				campaigns);
			failure.ifPresentOrElse(table::showError, () -> {
				table.frame().showCharactersWorkspace();
				table.frame().loadCharacterFromFile(target);
			});
		});
	}

	/**
	 * @param parent  the component the question opens over.
	 * @param message the question.
	 * @param initial the number offered.
	 * @return the whole number typed; empty when cancelled or not a number.
	 */
	static Optional<Integer> askNumber(Component parent, String message, int initial)
	{
		String typed = JOptionPane.showInputDialog(parent, message, initial == 0 ? "" : String.valueOf(initial));
		if (typed == null)
		{
			return Optional.empty();
		}
		try
		{
			return Optional.of(Integer.parseInt(typed.strip().replace("+", "")));
		}
		catch (NumberFormatException notANumber)
		{
			return Optional.empty();
		}
	}

	/** One line of the order: a turn, or a combatant that is delaying. */
	private record Row(Combatant combatant, Optional<Turn> turn)
	{
	}

	/** The order of initiative, turn by turn, those delaying last. */
	private static final class RowsModel extends AbstractTableModel
	{
		private static final String[] COLUMNS = {"in_gametable_initiativeColumn", "in_gametable_name",
			"in_gametable_hitPoints", "in_gametable_conditions", "in_gametable_spells"};

		private List<Row> rows = List.of();
		private Optional<Turn> current = Optional.empty();

		void show(Combat combat)
		{
			List<Row> shown = new ArrayList<>();
			for (Turn turn : combat.turns())
			{
				combat.combatant(turn.combatantId()).ifPresent(found -> shown.add(new Row(found, Optional.of(turn))));
			}
			combat.order().stream().filter(Combatant::delaying)
				.forEach(found -> shown.add(new Row(found, Optional.empty())));
			rows = List.copyOf(shown);
			current = combat.current();
			fireTableDataChanged();
		}

		int rowOf(int combatantId)
		{
			for (int row = 0; row < rows.size(); row++)
			{
				if (rows.get(row).combatant().id() == combatantId)
				{
					return row;
				}
			}
			return -1;
		}

		int currentRow()
		{
			for (int row = 0; row < rows.size(); row++)
			{
				if (rows.get(row).turn().isPresent() && rows.get(row).turn().equals(current))
				{
					return row;
				}
			}
			return -1;
		}

		boolean isCurrent(int row)
		{
			return row == currentRow();
		}

		@Override
		public int getRowCount()
		{
			return rows.size();
		}

		@Override
		public int getColumnCount()
		{
			return COLUMNS.length;
		}

		@Override
		public String getColumnName(int column)
		{
			return LanguageBundle.getString(COLUMNS[column]);
		}

		@Override
		public Object getValueAt(int row, int column)
		{
			Row line = rows.get(row);
			Combatant combatant = line.combatant();
			return switch (column)
			{
				case 0 -> initiativeText(line);
				case 1 -> combatant.name() + kindText(combatant);
				case 2 -> combatant.health().map(health -> health.remaining() + " / " + health.maximum()).orElse("");
				case 3 -> combatant.conditions().stream()
					.map(condition -> condition.name() + roundsText(condition.roundsLeft()))
					.collect(Collectors.joining(", "))
					+ combatant.roundsLeft().stream()
						.mapToObj(rounds -> LanguageBundle.getFormattedString("in_gametable_leavesIn", rounds))
						.findFirst().orElse("");
				default -> combatant.spells().stream().map(spell -> spell.name() + roundsText(spell.roundsLeft()))
					.collect(Collectors.joining(", "));
			};
		}

		private static String roundsText(OptionalInt roundsLeft)
		{
			return roundsLeft.stream().mapToObj(rounds -> " (" + rounds + ")").findFirst().orElse("");
		}

		private String kindText(Combatant combatant)
		{
			return switch (combatant.kind())
			{
				case PLAYER -> " " + LanguageBundle.getString("in_gametable_playerTag");
				case PLAYER_SUMMON, MONSTER_SUMMON -> " " + LanguageBundle.getString("in_gametable_summonTag");
				case MONSTER -> "";
			};
		}

		private static String initiativeText(Row line)
		{
			if (line.turn().isEmpty())
			{
				return LanguageBundle.getString("in_gametable_delaying");
			}
			OptionalInt score = line.combatant().initiative().score();
			if (score.isEmpty())
			{
				return "—";
			}
			return line.turn().get().second()
				? (score.getAsInt() - Initiative.SECOND_TURN_DROP) + " ²" : String.valueOf(score.getAsInt());
		}
	}

	/** Shows the turn being played, those delaying, and monsters at zero hit points or below. */
	private final class RowRenderer extends DefaultTableCellRenderer
	{
		@Override
		public Component getTableCellRendererComponent(JTable view, Object value, boolean selected, boolean focused,
			int row, int column)
		{
			Component cell = super.getTableCellRendererComponent(view, value, selected, focused, row, column);
			Row line = rows.rows.get(row);
			if (!selected)
			{
				cell.setBackground(rows.isCurrent(row) ? CURRENT_TURN : view.getBackground());
			}
			boolean down = line.combatant().health().map(health -> health.remaining() <= 0).orElse(false);
			cell.setForeground(down ? DOWN : (selected ? view.getSelectionForeground() : view.getForeground()));
			int style = Font.PLAIN;
			if (rows.isCurrent(row))
			{
				style |= Font.BOLD;
			}
			if (line.turn().isEmpty() || line.combatant().kind() == CombatantKind.PLAYER_SUMMON)
			{
				style |= Font.ITALIC;
			}
			cell.setFont(view.getFont().deriveFont(style));
			return cell;
		}
	}

	/** Drags a line of the order to another place. */
	private final class DragToReorder extends MouseAdapter
	{
		private int pressedRow = -1;

		@Override
		public void mousePressed(MouseEvent event)
		{
			pressedRow = order.rowAtPoint(event.getPoint());
		}

		@Override
		public void mouseReleased(MouseEvent event)
		{
			int releasedRow = order.rowAtPoint(event.getPoint());
			if (pressedRow >= 0 && releasedRow >= 0 && releasedRow != pressedRow)
			{
				moveRow(pressedRow, releasedRow);
			}
			pressedRow = -1;
		}
	}
}
