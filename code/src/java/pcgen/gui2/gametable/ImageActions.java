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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import pcgen.core.gametable.TabletopOutcome;
import pcgen.core.gametable.TabletopTransfer;
import pcgen.core.gametable.TabletopUpload;
import pcgen.system.LanguageBundle;

/**
 * What the game master does with an image: save it on this computer, or send it to Tabletop Simulator.
 */
final class ImageActions
{
	private ImageActions()
	{
	}

	/**
	 * Asks where to save the image, then writes it there.
	 *
	 * @param parent   the component the dialogs open over.
	 * @param fileName the name proposed for the file.
	 * @param bytes    the image's content.
	 */
	static void save(Component parent, String fileName, byte[] bytes)
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(new File(fileName));
		if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}
		try
		{
			Files.write(chooser.getSelectedFile().toPath(), bytes);
		}
		catch (IOException failed)
		{
			JOptionPane.showMessageDialog(parent, failed.getMessage(), LanguageBundle.getString("in_gametable_title"),
				JOptionPane.WARNING_MESSAGE);
		}
	}

	/**
	 * Sends the image in the background, then tells what became of it.
	 *
	 * @param name         the image's name in the message.
	 * @param placeOnTable whether to place it on the table of the loaded game, after putting it in the cloud.
	 */
	static void send(GameTablePanel table, Component parent, TabletopUpload upload, String name, boolean placeOnTable)
	{
		TabletopTransfer tabletop = table.tabletop();
		table.inBackground(() -> tabletop.send(upload, placeOnTable), outcome -> {
			String message = switch (outcome)
			{
				case TabletopOutcome.Sent sent -> LanguageBundle.getFormattedString(sent.placed()
					? "in_gametable_imageOnTableDone" : "in_gametable_imageToCloudDone", name, sent.key());
				case TabletopOutcome.Failed failed -> failed.reason();
			};
			JOptionPane.showMessageDialog(parent, message, LanguageBundle.getString("in_gametable_title"),
				outcome instanceof TabletopOutcome.Sent ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
		});
	}
}
