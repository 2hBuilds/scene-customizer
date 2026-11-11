package com.scenecustomizer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * The Changes half of the sidebar: one row per rule, newest change first, an Undo on each, and under them
 * {@code Undo all}, which asks {@code Undo all N changes?} with Yes and No before it removes every rule. With no rule
 * it says how to make one. It listens to the {@link RuleEditor} and builds itself again after every change.
 * Swing thread only.
 */
final class ChangesView extends JPanel
{
	private static final String EMPTY_TEXT = "No changes yet. Use a row's menu to make one.";
	private static final String UNDO_ALL_TEXT = "Undo all";
	private static final int GAP = 6;

	private final RuleEditor rules;

	/** @param rules where the rules are read, undone and listened to */
	ChangesView(RuleEditor rules)
	{
		this.rules = rules;
		setOpaque(false);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		rules.addListener(() -> SwingUtilities.invokeLater(this::rebuild));
		rebuild();
	}

	/** Draws the list again from the rules as they are now. */
	void rebuild()
	{
		removeAll();
		final List<Rule> all = rules.all();
		if (all.isEmpty())
		{
			add(left(emptyNote()));
		}
		for (Rule rule : all)
		{
			add(left(new ChangesRow(rule, () -> rules.remove(rule.kind, rule.name))));
		}
		if (!all.isEmpty())
		{
			add(Box.createVerticalStrut(GAP));
			add(left(undoAllLine(all.size())));
		}
		revalidate();
		repaint();
	}

	private static JTextArea emptyNote()
	{
		final JTextArea note = new JTextArea(EMPTY_TEXT);
		note.setLineWrap(true);
		note.setWrapStyleWord(true);
		note.setEditable(false);
		note.setFocusable(false);
		note.setOpaque(false);
		note.setFont(FontManager.getDefaultFont().deriveFont(12f));
		note.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		note.setBorder(new EmptyBorder(GAP, 0, 0, 0));
		return note;
	}

	/** The line under the rows: the Undo all link. */
	private JPanel undoAllLine(int count)
	{
		final JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		line.setOpaque(false);
		line.setBorder(new EmptyBorder(0, 6, 0, 0));
		line.add(link(UNDO_ALL_TEXT, () -> askUndoAll(line, count)));
		return line;
	}

	/** Swaps the Undo all link for the question and its Yes and No. */
	private void askUndoAll(JPanel line, int count)
	{
		line.removeAll();
		final JLabel question = new JLabel("Undo all " + count + (count == 1 ? " change?" : " changes?") + "  ");
		question.setFont(FontManager.getDefaultFont().deriveFont(12f));
		question.setForeground(Color.WHITE);
		line.add(question);
		line.add(link("Yes", rules::clear));
		line.add(Box.createHorizontalStrut(GAP));
		line.add(link("No", this::rebuild));
		line.revalidate();
		line.repaint();
	}

	/** A link word: 11 px, grey 165, white under the mouse. */
	private static JLabel link(String text, Runnable onPress)
	{
		final JLabel label = new JLabel(text);
		label.setFont(FontManager.getDefaultFont().deriveFont(11f));
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		label.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				label.setForeground(Color.WHITE);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e))
				{
					onPress.run();
				}
			}
		});
		return label;
	}

	private static <T extends JComponent> T left(T component)
	{
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
		return component;
	}
}
