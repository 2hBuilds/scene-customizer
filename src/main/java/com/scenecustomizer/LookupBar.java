package com.scenecustomizer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * The look-up bar between the top card and the session picker: an 18 px field that reads "Look up a monster" in grey
 * while empty, a grey line under it for "Looking...", "No monster by that name" and the failure words, and up to eight
 * suggestions, one per row. A press on a suggestion, or Enter (the first one), starts the look-up and clears the
 * suggestions; Escape clears the field. With the setting off the field is not editable and says so. Swing thread only.
 *
 * <p>Choice: after a pick the field keeps the typed text, so the player sees what they asked for.
 */
final class LookupBar extends JPanel
{
	/** The empty field's grey word. */
	static final String HINT = "Look up a monster";
	/** What the field says with the setting off. */
	static final String OFF_TEXT = "Wiki look-up is off";
	/** The field's hover while the setting is off. */
	static final String OFF_HOVER = "Turn it on in the settings menu.";
	/** The line while asking. */
	static final String LOOKING_TEXT = "Looking...";
	/** The line when nothing matches. */
	static final String NONE_TEXT = "No monster by that name";
	/** The line when the wiki did not answer. */
	static final String FAILED_TEXT = "The wiki didn't answer. Try again soon.";
	private static final Color HOVER = new Color(55, 55, 55);
	private static final int ROW_HEIGHT = 20;

	private final Consumer<String> onTyped;
	private final Consumer<String> onPick;
	private final HintField field;
	private final JLabel line = new JLabel();
	private final JPanel rows = new JPanel();
	private final List<String> shown = new ArrayList<>();
	private boolean quiet;
	private boolean off;

	/**
	 * @param onTyped told the field's text after every change the player makes
	 * @param onPick told the title of the suggestion picked
	 */
	LookupBar(Consumer<String> onTyped, Consumer<String> onPick)
	{
		this.onTyped = onTyped;
		this.onPick = onPick;
		setOpaque(false);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		field = new HintField(HINT, this::typed);
		field.addActionListener(e ->
		{
			if (!shown.isEmpty())
			{
				pick(shown.get(0));
			}
		});
		field.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearLookup");
		field.getActionMap().put("clearLookup", new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (!off)
				{
					field.setText("");
				}
			}
		});
		line.setFont(FontManager.getDefaultFont().deriveFont(11f));
		line.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		line.setBorder(new EmptyBorder(3, 6, 0, 0));
		line.setVisible(false);
		rows.setOpaque(false);
		rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
		add(left(field));
		add(left(line));
		add(left(rows));
	}

	/** Shows what the flow says: the line, or the suggestions. */
	void show(LookupBarState state)
	{
		clearRows();
		switch (state.state())
		{
			case LOOKING:
				say(LOOKING_TEXT);
				break;
			case NONE:
				say(NONE_TEXT);
				break;
			case FAILED:
				say(FAILED_TEXT);
				break;
			case SUGGESTIONS:
				say(null);
				for (String title : state.suggestions())
				{
					shown.add(title);
					rows.add(left(new SuggestionRow(title)));
				}
				break;
			default:
				say(null);
				break;
		}
		revalidate();
		repaint();
	}

	/** Switches the field on or off: off, it is not editable and says why; on, it is empty and ready. */
	void setWikiOn(boolean on)
	{
		if (off == !on)
		{
			return;
		}
		off = !on;
		quiet = true;
		field.setText(on ? "" : OFF_TEXT);
		quiet = false;
		field.setEditable(on);
		field.setFocusable(on);
		field.setToolTipText(on ? null : OFF_HOVER);
		field.setForeground(on ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
		clearRows();
		say(null);
		revalidate();
		repaint();
	}

	private void typed(String text)
	{
		if (!quiet)
		{
			onTyped.accept(text);
		}
	}

	private void pick(String title)
	{
		clearRows();
		say(null);
		revalidate();
		repaint();
		onPick.accept(title);
	}

	private void say(String text)
	{
		line.setText(text == null ? "" : text);
		line.setVisible(text != null);
	}

	private void clearRows()
	{
		rows.removeAll();
		shown.clear();
	}

	private static <T extends JComponent> T left(T component)
	{
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
		return component;
	}

	/** One suggestion: 12 px white, dark grey under the mouse. */
	private final class SuggestionRow extends JLabel
	{
		SuggestionRow(String title)
		{
			super(title);
			setFont(FontManager.getDefaultFont().deriveFont(12f));
			setForeground(Color.WHITE);
			setBorder(new EmptyBorder(0, 6, 0, 6));
			setOpaque(false);
			setBackground(HOVER);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setPreferredSize(new Dimension(ListView.WIDTH, ROW_HEIGHT));
			setMinimumSize(new Dimension(0, ROW_HEIGHT));
			setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseEntered(MouseEvent e)
				{
					setOpaque(true);
					repaint();
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					setOpaque(false);
					repaint();
				}

				@Override
				public void mousePressed(MouseEvent e)
				{
					if (SwingUtilities.isLeftMouseButton(e))
					{
						pick(title);
					}
				}
			});
		}
	}
}
