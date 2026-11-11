package com.scenecustomizer;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * One row of the Changes list, 36 px high: the kind word (11 px, grey 165) and the name (bold 12, white) on the first
 * line, what the rule does (11 px, grey 165) under it with a 9 x 9 swatch of the outline's colour before it when
 * there is an outline, and an Undo link (11 px, grey 165, white under the mouse) at the right. Swing thread only.
 */
final class ChangesRow extends JPanel
{
	/** The row's height, in px. */
	static final int HEIGHT = 36;
	private static final int EDGE = 6;
	private static final int GAP = 6;
	private static final int SWATCH = 9;
	private static final String UNDO_TEXT = "Undo";

	private final String kindWord;
	private final String name;
	private final String what;
	private final Color swatch;
	private final Runnable onUndo;
	private boolean overUndo;

	/**
	 * @param rule the rule this row shows
	 * @param onUndo what the Undo link does
	 */
	ChangesRow(Rule rule, Runnable onUndo)
	{
		this.kindWord = rule.kind == Kind.NPC ? "NPC" : "Object";
		this.name = rule.name;
		this.what = ChangesText.describe(rule);
		this.swatch = rule.outline == null ? null : new Color(rule.outline.rgb);
		this.onUndo = onUndo;
		setOpaque(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		setPreferredSize(new Dimension(ListView.WIDTH, HEIGHT));
		setMinimumSize(new Dimension(0, HEIGHT));
		setMaximumSize(new Dimension(Integer.MAX_VALUE, HEIGHT));
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e) && undoBounds().contains(e.getPoint()))
				{
					ChangesRow.this.onUndo.run();
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				hover(false);
			}
		});
		addMouseMotionListener(new MouseAdapter()
		{
			@Override
			public void mouseMoved(MouseEvent e)
			{
				hover(undoBounds().contains(e.getPoint()));
			}
		});
	}

	/** Where the Undo link sits, a little larger than its word so that it is easy to hit. */
	private java.awt.Rectangle undoBounds()
	{
		final FontMetrics fm = getFontMetrics(FontManager.getDefaultFont().deriveFont(11f));
		final int width = fm.stringWidth(UNDO_TEXT) + 2 * GAP;
		return new java.awt.Rectangle(getWidth() - EDGE - width + GAP, 0, width, getHeight());
	}

	private void hover(boolean over)
	{
		if (over != overUndo)
		{
			overUndo = over;
			setCursor(Cursor.getPredefinedCursor(over ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
			repaint();
		}
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		final Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setFont(FontManager.getDefaultFont().deriveFont(11f));
		FontMetrics small = g2.getFontMetrics();
		final int undoWidth = small.stringWidth(UNDO_TEXT);
		final int undoX = getWidth() - EDGE - undoWidth;
		final int limit = undoX - GAP;

		g2.setColor(ColorScheme.LIGHT_GRAY_COLOR);
		g2.drawString(kindWord, EDGE, 15);
		final int nameX = EDGE + small.stringWidth(kindWord) + 5;
		g2.setFont(FontManager.getDefaultBoldFont().deriveFont(12f));
		FontMetrics bold = g2.getFontMetrics();
		g2.setColor(Color.WHITE);
		g2.drawString(ThingRow.cut(bold, name, limit - nameX), nameX, 15);

		g2.setFont(FontManager.getDefaultFont().deriveFont(11f));
		small = g2.getFontMetrics();
		int textX = EDGE;
		if (swatch != null)
		{
			g2.setColor(swatch);
			g2.fillRect(textX, 29 - SWATCH, SWATCH, SWATCH);
			g2.setColor(swatch.darker());
			g2.drawRect(textX, 29 - SWATCH, SWATCH - 1, SWATCH - 1);
			textX += SWATCH + 4;
		}
		g2.setColor(ColorScheme.LIGHT_GRAY_COLOR);
		g2.drawString(ThingRow.cut(small, what, getWidth() - EDGE - textX), textX, 29);

		g2.setColor(overUndo ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
		g2.drawString(UNDO_TEXT, undoX, 15);
		g2.dispose();
	}
}
