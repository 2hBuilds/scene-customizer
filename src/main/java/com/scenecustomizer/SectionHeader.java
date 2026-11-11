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
 * A section's header: 17 px on (37, 37, 37), a fold triangle, the title (bold 12, white) and the count of its shown
 * rows (11, grey 165). A press folds or opens the section. Swing thread only.
 */
final class SectionHeader extends JPanel
{
	/** The header's height, in px. */
	static final int HEIGHT = 17;
	private static final Color GROUND = new Color(37, 37, 37);

	private final Section section;
	private final int count;
	private final boolean open;

	/**
	 * @param section the section
	 * @param count how many rows it shows
	 * @param open whether its rows show under the header
	 * @param onPress what a press does: folds or opens the section
	 */
	SectionHeader(Section section, int count, boolean open, Runnable onPress)
	{
		this.section = section;
		this.count = count;
		this.open = open;
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
					onPress.run();
				}
			}
		});
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		final Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setColor(GROUND);
		g2.fillRect(0, 0, getWidth(), HEIGHT);
		g2.setColor(ColorScheme.LIGHT_GRAY_COLOR);
		if (open)
		{
			g2.fillPolygon(new int[] {4, 10, 7}, new int[] {7, 7, 10}, 3);
		}
		else
		{
			g2.fillPolygon(new int[] {5, 5, 8}, new int[] {5, 11, 8}, 3);
		}
		g2.setFont(FontManager.getDefaultBoldFont().deriveFont(12f));
		FontMetrics fm = g2.getFontMetrics();
		final int baseline = (HEIGHT - fm.getHeight()) / 2 + fm.getAscent();
		g2.setColor(Color.WHITE);
		final String title = section.title();
		g2.drawString(title, 14, baseline);
		final int after = 14 + fm.stringWidth(title) + 6;
		g2.setFont(FontManager.getDefaultFont().deriveFont(11f));
		fm = g2.getFontMetrics();
		g2.setColor(ColorScheme.LIGHT_GRAY_COLOR);
		g2.drawString(String.valueOf(count), after, (HEIGHT - fm.getHeight()) / 2 + fm.getAscent());
		g2.dispose();
	}
}
