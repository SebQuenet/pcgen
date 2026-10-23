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
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import pcgen.core.gametable.LootBlock;
import pcgen.core.gametable.LootLine;
import pcgen.system.LanguageBundle;

/**
 * The loot of the scenario's finished fights, one block per fight, the latest last: each
 * line kept or left out by the game master, totals following.
 */
final class LootPanel extends JPanel
{
	private static final int KEPT_COLUMN = 0;
	private static final String[] COLUMNS = {"in_gametable_lootColumnKept", "in_gametable_lootColumnItem",
		"in_gametable_lootColumnCarrier", "in_gametable_lootColumnOrigin", "in_gametable_lootColumnValue",
		"in_gametable_lootColumnResale"};
	private static final int BLOCK_GAP = 12;
	/** The loot reads black on white, whatever the look and feel; a line left out is grey. */
	private static final Color PAPER = Color.WHITE;
	private static final Color INK = Color.BLACK;
	private static final Color LEFT_OUT_INK = Color.GRAY;
	private static final Color CHOSEN_ROW = Color.LIGHT_GRAY;

	private final GameTablePanel table;
	private final JPanel blocks = new JPanel();
	private final JLabel grandTotal = new JLabel(" ");
	private List<LootBlock> shown = List.of();
	private int shownPlayers = -1;

	LootPanel(GameTablePanel table)
	{
		super(new BorderLayout());
		this.table = table;
		blocks.setLayout(new BoxLayout(blocks, BoxLayout.Y_AXIS));
		JLabel heading = new JLabel(LanguageBundle.getString("in_gametable_loot"));
		heading.setFont(heading.getFont().deriveFont(Font.BOLD));
		add(heading, BorderLayout.NORTH);
		JPanel top = new JPanel(new BorderLayout());
		top.add(blocks, BorderLayout.NORTH);
		add(new JScrollPane(top, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
			BorderLayout.CENTER);
		add(grandTotal, BorderLayout.SOUTH);
	}

	void refresh()
	{
		List<LootBlock> loot = table.session().scenario().loot();
		int players = table.session().scenario().party().size();
		if (loot.equals(shown) && players == shownPlayers)
		{
			return;
		}
		shown = loot;
		shownPlayers = players;
		blocks.removeAll();
		for (LootBlock block : loot)
		{
			blocks.add(blockPanel(block, players));
			blocks.add(Box.createVerticalStrut(BLOCK_GAP));
		}
		grandTotal.setText(LanguageBundle.getFormattedString("in_gametable_lootGrandTotal",
			loot.stream().mapToLong(LootBlock::selectedValue).sum(), loot.stream().mapToLong(LootBlock::selectedResale).sum()));
		blocks.revalidate();
		blocks.repaint();
	}

	private JPanel blockPanel(LootBlock block, int players)
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder(LanguageBundle.getFormattedString("in_gametable_lootTitle",
			block.title(), block.challengeRating(), block.experience(), block.experiencePerPlayer(players))));
		JTable lines = new JTable(new LinesModel(block));
		lines.setBackground(PAPER);
		lines.setForeground(INK);
		lines.setSelectionBackground(CHOSEN_ROW);
		lines.setSelectionForeground(INK);
		lines.setGridColor(CHOSEN_ROW);
		lines.getTableHeader().setBackground(PAPER);
		lines.getTableHeader().setForeground(INK);
		lines.setDefaultRenderer(Object.class, new LeftOutRenderer(block));
		lines.setPreferredScrollableViewportSize(lines.getPreferredSize());
		lines.getColumnModel().getColumn(KEPT_COLUMN).setMaxWidth(lines.getRowHeight() * 3);
		JPanel tableHolder = new JPanel(new BorderLayout());
		tableHolder.add(lines.getTableHeader(), BorderLayout.NORTH);
		tableHolder.add(lines, BorderLayout.CENTER);
		panel.add(tableHolder, BorderLayout.CENTER);

		JPanel foot = new JPanel(new BorderLayout());
		foot.add(new JLabel(LanguageBundle.getFormattedString("in_gametable_lootTotals", block.selectedValue(),
			block.selectedResale(), block.budget(), block.sharePerPlayer(players))), BorderLayout.NORTH);
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
		buttons.add(GameTablePanel.button("in_gametable_lootReroll", () -> table.session().rerollLoot(block.id())));
		buttons.add(GameTablePanel.button("in_gametable_lootCopy", () -> copy(block, players)));
		buttons.add(GameTablePanel.button("in_gametable_lootRemove", () -> remove(block)));
		foot.add(buttons, BorderLayout.SOUTH);
		panel.add(foot, BorderLayout.SOUTH);
		return panel;
	}

	private void copy(LootBlock block, int players)
	{
		StringBuilder text = new StringBuilder();
		for (LootLine line : block.lines())
		{
			if (line.selected())
			{
				text.append(LanguageBundle.getFormattedString("in_gametable_lootCopyLine", line.name(), line.quantity(),
					line.resale())).append('\n');
			}
		}
		text.append(LanguageBundle.getFormattedString("in_gametable_lootCopyTotal", block.selectedResale(),
			block.sharePerPlayer(players))).append('\n');
		StringSelection selection = new StringSelection(text.toString());
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
	}

	private void remove(LootBlock block)
	{
		if (JOptionPane.showConfirmDialog(this, LanguageBundle.getFormattedString("in_gametable_lootRemoveConfirm",
			block.title()), LanguageBundle.getString("in_gametable_title"), JOptionPane.OK_CANCEL_OPTION)
			== JOptionPane.OK_OPTION)
		{
			table.session().removeLoot(block.id());
		}
	}

	private static String origin(LootLine line)
	{
		return line.drawnAs().map(type -> LanguageBundle.getFormattedString("in_gametable_lootDrawn",
			LanguageBundle.getString("in_gametable_lootType_" + type.name())))
			.orElseGet(() -> LanguageBundle.getString("in_gametable_lootCarried"));
	}

	private final class LinesModel extends AbstractTableModel
	{
		private final LootBlock block;

		private LinesModel(LootBlock block)
		{
			this.block = block;
		}

		@Override
		public int getRowCount()
		{
			return block.lines().size();
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
		public Class<?> getColumnClass(int column)
		{
			return column == KEPT_COLUMN ? Boolean.class : Object.class;
		}

		@Override
		public boolean isCellEditable(int row, int column)
		{
			return column == KEPT_COLUMN;
		}

		@Override
		public Object getValueAt(int row, int column)
		{
			LootLine line = block.lines().get(row);
			return switch (column)
			{
				case KEPT_COLUMN -> line.selected();
				case 1 -> line.quantity() > 1 ? line.name() + " ×" + line.quantity() : line.name();
				case 2 -> line.carrier();
				case 3 -> origin(line);
				case 4 -> LanguageBundle.getFormattedString("in_gametable_lootGold", line.value());
				default -> LanguageBundle.getFormattedString("in_gametable_lootGold", line.resale());
			};
		}

		@Override
		public void setValueAt(Object value, int row, int column)
		{
			if (column == KEPT_COLUMN && value instanceof Boolean kept)
			{
				table.session().selectLoot(block.id(), row, kept);
			}
		}
	}

	/** Greys out and strikes through a line the game master left out. */
	private static final class LeftOutRenderer extends DefaultTableCellRenderer
	{
		private final LootBlock block;

		private LeftOutRenderer(LootBlock block)
		{
			this.block = block;
		}

		@Override
		public Component getTableCellRendererComponent(JTable lines, Object value, boolean isSelected, boolean hasFocus,
			int row, int column)
		{
			boolean kept = block.lines().get(row).selected();
			String text = String.valueOf(value);
			String escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
			Component cell = super.getTableCellRendererComponent(lines, kept ? text : "<html><s>" + escaped + "</s></html>",
				isSelected, hasFocus, row, column);
			cell.setBackground(isSelected ? CHOSEN_ROW : PAPER);
			cell.setForeground(kept ? INK : LEFT_OUT_INK);
			setHorizontalAlignment(column >= 4 ? RIGHT : LEFT);
			return cell;
		}
	}
}
