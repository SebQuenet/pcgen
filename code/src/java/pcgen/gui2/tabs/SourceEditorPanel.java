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
package pcgen.gui2.tabs;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Edits a character's source text, such as a tactical plan or a catalog of
 * conjurations, and says what is wrong with it while it is typed.
 *
 * <p>
 * Every keystroke restarts a short timer rather than storing at once, so a word
 * being typed does not each time rewrite the character and re-render its page.
 */
@SuppressWarnings("serial")
public final class SourceEditorPanel extends JPanel
{
	private static final int TYPING_PAUSE_MS = 300;
	private static final int BANNER_PADDING = 3;
	private static final int BANNER_INDENT = 6;

	private final JTextArea editor = new JTextArea();
	private final JLabel banner = new JLabel();
	private final Timer typingPause = new Timer(TYPING_PAUSE_MS, event -> storeNow());
	private final DocumentListener typing = new DocumentListener()
	{
		@Override
		public void insertUpdate(DocumentEvent event)
		{
			typingPause.restart();
		}

		@Override
		public void removeUpdate(DocumentEvent event)
		{
			typingPause.restart();
		}

		@Override
		public void changedUpdate(DocumentEvent event)
		{
			typingPause.restart();
		}
	};
	private EditedText edited;

	/**
	 * One character's text, as the editor needs it.
	 */
	public interface EditedText
	{
		/**
		 * @return the text the character holds, empty when it holds none
		 */
		String stored();

		/**
		 * @param text a candidate text
		 * @return what is wrong with it, one message per problem, empty when it
		 *         reads
		 */
		List<String> problemsIn(String text);

		/**
		 * Stores the text whether or not it reads: losing someone's typing
		 * because of a half finished line would be worse than storing it.
		 *
		 * @param text  the text as typed
		 * @param reads true when it has no problem, so its page can be rendered
		 *              again
		 */
		void store(String text, boolean reads);
	}

	public SourceEditorPanel()
	{
		super(new BorderLayout());
		editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, editor.getFont().getSize()));
		editor.setTabSize(2);
		editor.setLineWrap(false);
		banner.setBorder(BorderFactory.createEmptyBorder(BANNER_PADDING, BANNER_INDENT, BANNER_PADDING, BANNER_INDENT));
		typingPause.setRepeats(false);
		add(new JScrollPane(editor), BorderLayout.CENTER);
		add(banner, BorderLayout.SOUTH);
	}

	/**
	 * @param text the character's text to edit from now on
	 */
	public void install(EditedText text)
	{
		edited = text;
		editor.setText(text.stored());
		editor.setCaretPosition(0);
		showProblems(text.problemsIn(editor.getText()));
		editor.getDocument().addDocumentListener(typing);
	}

	/**
	 * Stores what is typed, and stops editing it.
	 */
	public void uninstall()
	{
		typingPause.stop();
		editor.getDocument().removeDocumentListener(typing);
		storeNow();
		edited = null;
	}

	private void storeNow()
	{
		if (edited == null)
		{
			return;
		}
		String text = editor.getText();
		List<String> problems = edited.problemsIn(text);
		showProblems(problems);
		edited.store(text, problems.isEmpty());
	}

	private void showProblems(List<String> problems)
	{
		if (problems.isEmpty())
		{
			banner.setText(" ");
			return;
		}
		String more = (problems.size() > 1) ? " (+" + (problems.size() - 1) + ')' : "";
		banner.setText(problems.getFirst() + more);
	}
}
