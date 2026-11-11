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
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * The session picker: a 20 px row on {@code DARK_GRAY_COLOR} with the shown session's title (12, white) at the left
 * and, at the right, its info (11, grey 165) and a small down triangle. A press opens a popup with one radio item per
 * kept session, the shown one selected. With no session the row reads "No recordings yet" and a press does nothing.
 * Swing thread only.
 *
 * <p>Choice: a recording's info reads "under 1 min" below one minute, as the status line does.
 */
final class SessionPicker extends JPanel
{
	/** The row's height, in px. */
	static final int HEIGHT = 20;
	private static final String EMPTY_TEXT = "No recordings yet";
	private static final String LOOKUP_INFO = "look-up";
	private static final int SIDE = 6;
	private static final int TRIANGLE_WIDTH = 14;
	private static final int GAP = 8;
	/** A press this soon after the popup closed is the press that closed it, not a new open. */
	private static final long REOPEN_GUARD_NANOS = 250_000_000L;

	private final IntConsumer onPick;
	private List<SessionView> sessions = new ArrayList<>();
	private int shown;
	private long closedAtNanos = System.nanoTime() - 2 * REOPEN_GUARD_NANOS;

	/** @param onPick told the index of the session picked from the popup */
	SessionPicker(IntConsumer onPick)
	{
		this.onPick = onPick;
		setOpaque(false);
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
					open();
				}
			}
		});
	}

	/** Shows the kept sessions and which one is shown. */
	void setSessions(List<SessionView> all, int shownIndex)
	{
		sessions = new ArrayList<>(all);
		shown = shownIndex;
		setCursor(Cursor.getPredefinedCursor(sessions.isEmpty() ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR));
		repaint();
	}

	/** The info words of a session: its length and count for a recording, "look-up" for a look-up. */
	static String info(SessionView view)
	{
		if (view.kind() == Session.SessionKind.LOOKUP)
		{
			return LOOKUP_INFO;
		}
		final long minutes = view.lengthMs() / 60000L;
		return (minutes < 1 ? "under 1 min" : minutes + " min") + ", " + view.thingCount() + " things";
	}

	private void open()
	{
		if (sessions.isEmpty() || System.nanoTime() - closedAtNanos < REOPEN_GUARD_NANOS || !isShowing())
		{
			return;
		}
		final JPopupMenu popup = new JPopupMenu();
		popup.setBorder(new EmptyBorder(5, 5, 5, 5));
		final ButtonGroup group = new ButtonGroup();
		for (int i = 0; i < sessions.size(); i++)
		{
			final int index = i;
			final JRadioButtonMenuItem item = new JRadioButtonMenuItem(sessions.get(i).title(), i == shown);
			item.setFont(FontManager.getDefaultFont().deriveFont(12f));
			item.setForeground(Color.WHITE);
			item.addActionListener(e -> onPick.accept(index));
			group.add(item);
			popup.add(item);
		}
		popup.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
			{
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
			{
				closedAtNanos = System.nanoTime();
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e)
			{
				closedAtNanos = System.nanoTime();
			}
		});
		popup.show(this, 0, HEIGHT);
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		final Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		final int width = getWidth();
		g2.setColor(ColorScheme.DARK_GRAY_COLOR);
		g2.fillRect(0, 0, width, HEIGHT);
		g2.setFont(FontManager.getDefaultFont().deriveFont(12f));
		FontMetrics fm = g2.getFontMetrics();
		final int titleBase = (HEIGHT - fm.getHeight()) / 2 + fm.getAscent();
		g2.setColor(Color.WHITE);
		if (sessions.isEmpty() || shown < 0 || shown >= sessions.size())
		{
			g2.drawString(EMPTY_TEXT, SIDE, titleBase);
			g2.dispose();
			return;
		}
		final SessionView view = sessions.get(shown);
		final String info = info(view);
		g2.setFont(FontManager.getDefaultFont().deriveFont(11f));
		final FontMetrics small = g2.getFontMetrics();
		final int infoWidth = small.stringWidth(info);
		final int infoX = width - SIDE - TRIANGLE_WIDTH - infoWidth;
		g2.setColor(ColorScheme.LIGHT_GRAY_COLOR);
		g2.drawString(info, infoX, (HEIGHT - small.getHeight()) / 2 + small.getAscent());
		g2.fillPolygon(new int[] {width - SIDE - 8, width - SIDE - 2, width - SIDE - 5},
			new int[] {8, 8, 11}, 3);
		g2.setFont(FontManager.getDefaultFont().deriveFont(12f));
		fm = g2.getFontMetrics();
		g2.setColor(Color.WHITE);
		g2.drawString(ThingRow.cut(fm, view.title(), infoX - GAP - SIDE), SIDE, titleBase);
		g2.dispose();
	}
}
