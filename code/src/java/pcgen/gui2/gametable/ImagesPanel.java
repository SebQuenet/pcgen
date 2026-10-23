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
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import pcgen.core.gametable.ImageCategory;
import pcgen.core.gametable.ScenarioImage;
import pcgen.core.gametable.TabletopOutcome;
import pcgen.core.gametable.TabletopTransfer;
import pcgen.system.LanguageBundle;

/**
 * The images kept in the scenario at one place, the scenario itself or an encounter: listed in
 * the order they are shown, viewed, saved to disk, sent to Tabletop Simulator.
 */
final class ImagesPanel extends JPanel
{
	private static final double SCREEN_SHARE = 0.8;
	private static final String[] IMAGE_EXTENSIONS = {"png", "jpg", "jpeg", "gif", "bmp"};

	private final GameTablePanel table;
	private final Supplier<Optional<String>> encounter;
	private final List<ImageCategory> categories;
	private final TabletopTransfer tabletop;
	private final DefaultListModel<ScenarioImage> images = new DefaultListModel<>();
	private final JList<ScenarioImage> list = new JList<>(images);
	private final JButton add;

	/**
	 * @param table      the game table.
	 * @param titleKey   the key of the list's title.
	 * @param encounter  the encounter the images belong to; empty for the scenario's own.
	 * @param categories the kinds of image kept here.
	 * @param tabletop   sends images to Tabletop Simulator.
	 */
	ImagesPanel(GameTablePanel table, String titleKey, Supplier<Optional<String>> encounter, List<ImageCategory> categories,
		TabletopTransfer tabletop)
	{
		super(new BorderLayout());
		this.table = table;
		this.encounter = encounter;
		this.categories = List.copyOf(categories);
		this.tabletop = tabletop;
		setBorder(BorderFactory.createTitledBorder(LanguageBundle.getString(titleKey)));
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setVisibleRowCount(4);
		list.setCellRenderer((view, image, index, selected, focused) -> new DefaultListCellRenderer()
			.getListCellRendererComponent(view, image.name() + " — " + categoryLabel(image.category()), index, selected,
				focused));
		add(new JScrollPane(list), BorderLayout.CENTER);
		JPanel buttons = new JPanel(new GridLayout(0, 2));
		add = GameTablePanel.button("in_gametable_imageAdd", this::addImage);
		buttons.add(add);
		buttons.add(GameTablePanel.button("in_gametable_imageView", () -> selected().ifPresent(this::view)));
		buttons.add(GameTablePanel.button("in_gametable_imageSave", () -> selected().ifPresent(this::save)));
		buttons.add(GameTablePanel.button("in_gametable_imageToCloud", () -> selected().ifPresent(image -> send(image, false))));
		buttons.add(GameTablePanel.button("in_gametable_imageOnTable", () -> selected().ifPresent(image -> send(image, true))));
		buttons.add(GameTablePanel.button("in_gametable_imageRemove", () -> selected().ifPresent(this::remove)));
		add(buttons, BorderLayout.SOUTH);
	}

	/**
	 * @param shown   the images kept here now.
	 * @param canAdd  whether an image may be added: an encounter must be chosen for its own.
	 */
	void refresh(List<ScenarioImage> shown, boolean canAdd)
	{
		add.setEnabled(canAdd);
		if (shown.equals(current()))
		{
			return;
		}
		int selected = list.getSelectedIndex();
		images.clear();
		shown.forEach(images::addElement);
		if (selected >= 0 && selected < images.size())
		{
			list.setSelectedIndex(selected);
		}
	}

	private List<ScenarioImage> current()
	{
		return IntStream.range(0, images.size()).mapToObj(images::get).toList();
	}

	private Optional<ScenarioImage> selected()
	{
		return Optional.ofNullable(list.getSelectedValue());
	}

	private static String categoryLabel(ImageCategory category)
	{
		return LanguageBundle.getString("in_gametable_imageCategory_" + category.name());
	}

	private void addImage()
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new FileNameExtensionFilter(LanguageBundle.getString("in_gametable_imageFiles"),
			IMAGE_EXTENSIONS));
		if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}
		Optional<ImageCategory> category = chooseCategory();
		if (category.isEmpty())
		{
			return;
		}
		File file = chooser.getSelectedFile();
		try
		{
			table.session().addImage(encounter.get(), file.getName(), category.get(), Files.readAllBytes(file.toPath()));
		}
		catch (IOException | IllegalArgumentException refused)
		{
			JOptionPane.showMessageDialog(this, refused.getMessage(), LanguageBundle.getString("in_gametable_title"),
				JOptionPane.WARNING_MESSAGE);
		}
	}

	private Optional<ImageCategory> chooseCategory()
	{
		if (categories.size() == 1)
		{
			return Optional.of(categories.getFirst());
		}
		String[] labels = categories.stream().map(ImagesPanel::categoryLabel).toArray(String[]::new);
		Object chosen = JOptionPane.showInputDialog(this, LanguageBundle.getString("in_gametable_imageCategory"),
			LanguageBundle.getString("in_gametable_imageAdd"), JOptionPane.QUESTION_MESSAGE, null, labels, labels[0]);
		for (int index = 0; index < labels.length; index++)
		{
			if (labels[index].equals(chosen))
			{
				return Optional.of(categories.get(index));
			}
		}
		return Optional.empty();
	}

	private void view(ScenarioImage image)
	{
		BufferedImage picture;
		try
		{
			picture = ImageIO.read(new ByteArrayInputStream(image.bytes()));
		}
		catch (IOException unreadable)
		{
			picture = null;
		}
		if (picture == null)
		{
			JOptionPane.showMessageDialog(this, LanguageBundle.getString("in_gametable_imageUnreadable"),
				image.name(), JOptionPane.WARNING_MESSAGE);
			return;
		}
		JDialog viewer = new JDialog(SwingUtilities.getWindowAncestor(this), image.name());
		viewer.add(new JScrollPane(new JLabel(new ImageIcon(picture))));
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		viewer.setSize((int) Math.min(picture.getWidth() + 40, screen.width * SCREEN_SHARE),
			(int) Math.min(picture.getHeight() + 60, screen.height * SCREEN_SHARE));
		viewer.setLocationRelativeTo(this);
		viewer.setVisible(true);
	}

	private void save(ScenarioImage image)
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(new File(image.fileName()));
		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}
		try
		{
			Files.write(chooser.getSelectedFile().toPath(), image.bytes());
		}
		catch (IOException failed)
		{
			JOptionPane.showMessageDialog(this, failed.getMessage(), LanguageBundle.getString("in_gametable_title"),
				JOptionPane.WARNING_MESSAGE);
		}
	}

	private void send(ScenarioImage image, boolean placeOnTable)
	{
		table.inBackground(() -> tabletop.send(image, placeOnTable), outcome -> {
			String message = switch (outcome)
			{
				case TabletopOutcome.Sent sent -> LanguageBundle.getFormattedString(sent.placed()
					? "in_gametable_imageOnTableDone" : "in_gametable_imageToCloudDone", image.name(), sent.key());
				case TabletopOutcome.Failed failed -> failed.reason();
			};
			JOptionPane.showMessageDialog(this, message, LanguageBundle.getString("in_gametable_title"),
				outcome instanceof TabletopOutcome.Sent ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
		});
	}

	private void remove(ScenarioImage image)
	{
		if (JOptionPane.showConfirmDialog(this, LanguageBundle.getFormattedString("in_gametable_imageRemoveConfirm",
			image.name()), LanguageBundle.getString("in_gametable_title"), JOptionPane.OK_CANCEL_OPTION)
			== JOptionPane.OK_OPTION)
		{
			table.session().removeImage(image.id());
		}
	}
}
