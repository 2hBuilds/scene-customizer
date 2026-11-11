package com.scenecustomizer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * One row of the list. An NPC or object row is 38 px: a 36 x 32 picture box 4 px from the left (empty until the
 * picture comes), the name in bold 12 at x 46 cut with "..." to fit, the rule's badge right after the name, the
 * second line under it, and a 16 x 16 "..." button at the right edge. An effect, animation or spell row is 32 px with
 * no picture box and its text at x 6. The whole row is drawn in one go. Swing thread only.
 */
final class ThingRow extends JPanel
{
	/** The height of a row with a picture box, in px. */
	static final int TALL = 38;
	/** The height of a row without one, in px. */
	static final int SHORT = 32;
	private static final int PICTURE_W = 36;
	private static final int PICTURE_H = 32;
	private static final int PICTURE_X = 4;
	private static final int TEXT_X_WITH_PICTURE = 46;
	private static final int TEXT_X_PLAIN = 6;
	private static final int BUTTON = 16;
	private static final int EDGE = 4;
	private static final int BADGE_GAP = 6;
	private static final int SWATCH = 9;
	private static final int EYE_W = 11;
	private static final String ELLIPSIS = "...";
	private static final String EDIT_TIP = "Edit";
	private static final Color PICTURE_GROUND = new Color(24, 24, 24);

	private final ThingView thing;
	private final boolean pictureBox;
	private final String second;
	private final Consumer<ThingRow> onMenu;
	private Rule rule;
	private boolean overButton;

	/**
	 * @param thing the row's frozen Thing
	 * @param rule the rule that names it, or null
	 * @param onMenu told this row when its "..." button is pressed
	 */
	ThingRow(ThingView thing, Rule rule, Consumer<ThingRow> onMenu)
	{
		this.thing = thing;
		this.rule = rule;
		this.onMenu = onMenu;
		this.pictureBox = thing.kind() == Kind.NPC || thing.kind() == Kind.OBJECT;
		this.second = RowText.secondLine(thing);
		final int height = pictureBox ? TALL : SHORT;
		setOpaque(false);
		setPreferredSize(new Dimension(ListView.WIDTH, height));
		setMinimumSize(new Dimension(0, height));
		setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
		setToolTipText(" ");
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e) && buttonBounds().contains(e.getPoint()))
				{
					onMenu.accept(ThingRow.this);
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
				hover(buttonBounds().contains(e.getPoint()));
			}
		});
	}

	/** The Thing this row shows. */
	ThingView thing()
	{
		return thing;
	}

	/** Shows the rule's badge: its outline colour, its hide, both or none. */
	void setRule(Rule rule)
	{
		this.rule = rule;
		repaint();
	}

	/** Where the "..." button sits in the row. */
	Rectangle buttonBounds()
	{
		return new Rectangle(getWidth() - EDGE - BUTTON, (getHeight() - BUTTON) / 2, BUTTON, BUTTON);
	}

	@Override
	public String getToolTipText(MouseEvent event)
	{
		return buttonBounds().contains(event.getPoint()) ? EDIT_TIP : null;
	}

	private void hover(boolean over)
	{
		if (over != overButton)
		{
			overButton = over;
			repaint();
		}
	}

	/** The text cut with "..." so that it is no wider than {@code width} in the metrics' face. */
	static String cut(FontMetrics fm, String text, int width)
	{
		if (fm.stringWidth(text) <= width)
		{
			return text;
		}
		String kept = text;
		while (!kept.isEmpty() && fm.stringWidth(kept + ELLIPSIS) > width)
		{
			kept = kept.substring(0, kept.length() - 1);
		}
		return fm.stringWidth(kept + ELLIPSIS) > width ? "" : kept + ELLIPSIS;
	}

	private boolean outlined()
	{
		return rule != null && rule.outline != null;
	}

	private boolean hidden()
	{
		return rule != null && rule.hide != HideScope.OFF;
	}

	private int badgeWidth()
	{
		return (outlined() ? SWATCH : 0) + (outlined() && hidden() ? 4 : 0) + (hidden() ? EYE_W : 0);
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		final Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		final int width = getWidth();
		final int height = getHeight();
		int textX = TEXT_X_PLAIN;
		if (pictureBox)
		{
			textX = TEXT_X_WITH_PICTURE;
			paintPicture(g2);
		}
		final int limit = width - EDGE - BUTTON - EDGE;

		g2.setFont(FontManager.getDefaultBoldFont().deriveFont(12f));
		FontMetrics fm = g2.getFontMetrics();
		final int nameBase = pictureBox ? 16 : 13;
		final int badge = badgeWidth();
		final String name = cut(fm, thing.label(), limit - textX - (badge > 0 ? badge + BADGE_GAP : 0));
		g2.setColor(Color.WHITE);
		g2.drawString(name, textX, nameBase);
		if (badge > 0)
		{
			paintBadge(g2, textX + fm.stringWidth(name) + BADGE_GAP, nameBase - SWATCH);
		}

		g2.setFont(FontManager.getDefaultFont().deriveFont(11f));
		fm = g2.getFontMetrics();
		g2.setColor(ColorScheme.LIGHT_GRAY_COLOR);
		g2.drawString(cut(fm, second, limit - textX), textX, pictureBox ? 31 : 27);

		final Rectangle b = new Rectangle(width - EDGE - BUTTON, (height - BUTTON) / 2, BUTTON, BUTTON);
		g2.setColor(overButton ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
		for (int i = 0; i < 3; i++)
		{
			g2.fillRect(b.x + 3 + i * 4, b.y + BUTTON / 2 - 1, 2, 2);
		}
		g2.dispose();
	}

	private void paintPicture(Graphics2D g2)
	{
		final int top = (getHeight() - PICTURE_H) / 2;
		g2.setColor(PICTURE_GROUND);
		g2.fillRect(PICTURE_X, top, PICTURE_W, PICTURE_H);
		final BufferedImage picture = thing.picture();
		if (picture != null)
		{
			g2.drawImage(picture, PICTURE_X + (PICTURE_W - picture.getWidth()) / 2,
				top + (PICTURE_H - picture.getHeight()) / 2, null);
		}
	}

	private void paintBadge(Graphics2D g2, int x, int y)
	{
		int at = x;
		if (outlined())
		{
			final Color colour = new Color(rule.outline.rgb);
			g2.setColor(colour);
			g2.fillRect(at, y, SWATCH, SWATCH);
			g2.setColor(colour.darker());
			g2.drawRect(at, y, SWATCH - 1, SWATCH - 1);
			at += SWATCH + 4;
		}
		if (hidden())
		{
			g2.setColor(ColorScheme.LIGHT_GRAY_COLOR);
			g2.drawOval(at, y + 1, EYE_W - 1, 6);
			g2.drawLine(at, y + SWATCH, at + EYE_W - 2, y);
		}
	}

	/** Where the popup menu should hang from: the button's bottom right corner. */
	Point menuCorner()
	{
		final Rectangle b = buttonBounds();
		return new Point(b.x + b.width, b.y + b.height);
	}
}
