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
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import pcgen.core.gametable.BestiaryPawn;
import pcgen.system.LanguageBundle;

/**
 * The buttons that save the pawn of the creature shown, or send it to Tabletop Simulator, and the link on the pawn
 * that shows it larger.
 */
final class PawnButtons
{
	private PawnButtons()
	{
	}

	/**
	 * @param parent the component the dialogs open over.
	 * @param shown  the pawn of the creature shown now; empty when no creature is shown or none was drawn for it.
	 * @return the buttons: save, to the cloud, on the table.
	 */
	static List<JButton> of(GameTablePanel table, Component parent, Supplier<Optional<BestiaryPawn>> shown)
	{
		return List.of(
			GameTablePanel.button("in_gametable_pawnSave", () -> withPawn(parent, shown,
				pawn -> ImageActions.save(parent, pawn.fileName(), pawn.bytes()))),
			GameTablePanel.button("in_gametable_pawnToCloud", () -> withPawn(parent, shown,
				pawn -> ImageActions.send(table, parent, pawn.forTabletop(), pawn.fileName(), false))),
			GameTablePanel.button("in_gametable_pawnOnTable", () -> withPawn(parent, shown,
				pawn -> ImageActions.send(table, parent, pawn.forTabletop(), pawn.fileName(), true))));
	}

	/**
	 * @param parent the component the window opens over.
	 * @param shown  the pawn of the creature shown now; empty when no creature is shown or none was drawn for it.
	 * @return what follows the link a sheet puts on the pawn: the pawn at its own size, in a window of its own.
	 */
	static HyperlinkListener enlarging(Component parent, Supplier<Optional<BestiaryPawn>> shown)
	{
		return event -> {
			if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED
				&& SheetHtml.PAWN_LINK.equals(event.getDescription()))
			{
				shown.get().ifPresent(pawn -> ImageActions.view(parent, pawn.fileName(), pawn.bytes()));
			}
		};
	}

	private static void withPawn(Component parent, Supplier<Optional<BestiaryPawn>> shown,
		Consumer<BestiaryPawn> action)
	{
		shown.get().ifPresentOrElse(action, () -> JOptionPane.showMessageDialog(parent,
			LanguageBundle.getString("in_gametable_noPawn"), LanguageBundle.getString("in_gametable_title"),
			JOptionPane.INFORMATION_MESSAGE));
	}
}
