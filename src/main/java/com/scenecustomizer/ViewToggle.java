package com.scenecustomizer;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.IntConsumer;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * The two-half toggle under the session picker (STANDARD A4): two equal halves, the lit one on an orange ground with
 * dark words, the other on (55, 55, 55) with grey 165 words, both words bold in one size. Swing thread only.
 *
 * <p>Choice: the size is the largest up to 12 at which both words keep 6 px a side, measured with the font's own
 * metrics each time the toggle is drawn, since the width is only known then.
 */
final class ViewToggle extends JPanel
{
	/** The toggle's height, in px. */
	static final int HEIGHT = 18;
	private static final int LARGEST = 12;
	private static final int SMALLEST = 8;
	private static final int SIDE = 6;
	private static final Color UNLIT = new Color(55, 55, 55);
	private static final Color DARK = new Color(30, 20, 0);

	private final String[] words;
	private final IntConsumer onPick;
	private int selected;

	/**
	 * @param left the left half's word
	 * @param right the right half's word
	 * @param onPick told 0 or 1 when a half is pressed
	 */
	ViewToggle(String left, String right, IntConsumer onPick)
	{
		this.words = new String[] {left, right};
		this.onPick = onPick;
		setOpaque(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setPreferredSize(new Dimension(ListView.WIDTH, HEIGHT));
		setMinimumSize(new Dimension(0, HEIGHT));
		setMaximumSize(new Dimension(Integer.MAX_VALUE, HEIGHT));
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e))
				{
					pick(e.getX() < getWidth() / 2 ? 0 : 1);
				}
			}
		});
	}

	/** Lights a half without telling anyone. */
	void setSelected(int index)
	{
		selected = index;
		repaint();
	}

	private void pick(int index)
	{
		setSelected(index);
		onPick.accept(index);
	}

	/** The largest size from {@code largest} down that leaves {@code side} px each side of every word in {@code slot}. */
	static int fitSize(java.awt.Component on, boolean bold, int largest, int side, int slot, String... texts)
	{
		for (int size = largest; size > SMALLEST; size--)
		{
			final FontMetrics fm = on.getFontMetrics(face(bold, size));
			boolean fits = true;
			for (String text : texts)
			{
				fits &= fm.stringWidth(text) + 2 * side <= slot;
			}
			if (fits)
			{
				return size;
			}
		}
		return SMALLEST;
	}

	private static Font face(boolean bold, int size)
	{
		return (bold ? FontManager.getDefaultBoldFont() : FontManager.getDefaultFont()).deriveFont((float) size);
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		final Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		final int width = getWidth();
		final int half = (width - 1) / 2;
		final int size = fitSize(this, true, LARGEST, SIDE, half, words);
		final Font font = face(true, size);
		g2.setFont(font);
		final FontMetrics fm = g2.getFontMetrics();
		for (int i = 0; i < 2; i++)
		{
			final int x = i == 0 ? 0 : half + 1;
			final int w = i == 0 ? half : width - half - 1;
			final boolean lit = i == selected;
			g2.setColor(lit ? ColorScheme.BRAND_ORANGE : UNLIT);
			g2.fillRect(x, 0, w, HEIGHT);
			g2.setColor(lit ? DARK : ColorScheme.LIGHT_GRAY_COLOR);
			g2.drawString(words[i], x + (w - fm.stringWidth(words[i])) / 2, (HEIGHT - fm.getHeight()) / 2 + fm.getAscent());
		}
		g2.dispose();
	}
}
