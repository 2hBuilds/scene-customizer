package com.scenecustomizer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.function.Consumer;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * The filter box: 18 px high, a normal text field that shows a hint in grey while it is empty and tells its listener
 * the text as it is typed. Swing thread only.
 */
final class HintField extends JTextField
{
	/** The box's height, in px. */
	static final int HEIGHT = 18;
	private static final Color GROUND = new Color(21, 21, 21);
	private static final Color EDGE = new Color(60, 60, 60);

	private final String hint;

	/**
	 * @param hint the grey word shown while the box is empty
	 * @param onText told the whole text after every change
	 */
	HintField(String hint, Consumer<String> onText)
	{
		this.hint = hint;
		setFont(FontManager.getDefaultFont().deriveFont(12f));
		setForeground(Color.WHITE);
		setCaretColor(Color.WHITE);
		setBackground(GROUND);
		setBorder(new CompoundBorder(new LineBorder(EDGE, 1), new EmptyBorder(0, 5, 0, 5)));
		setPreferredSize(new Dimension(ListView.WIDTH, HEIGHT));
		setMinimumSize(new Dimension(0, HEIGHT));
		setMaximumSize(new Dimension(Integer.MAX_VALUE, HEIGHT));
		getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				onText.accept(getText());
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				onText.accept(getText());
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				onText.accept(getText());
			}
		});
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		if (getText().isEmpty())
		{
			final Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setFont(getFont());
			g2.setColor(ColorScheme.LIGHT_GRAY_COLOR);
			final int x = getInsets().left;
			g2.drawString(hint, x, (getHeight() - g2.getFontMetrics().getHeight()) / 2 + g2.getFontMetrics().getAscent());
			g2.dispose();
		}
	}
}
