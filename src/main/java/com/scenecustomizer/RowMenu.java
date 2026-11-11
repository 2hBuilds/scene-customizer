package com.scenecustomizer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * The menu a row's "..." button opens, in STANDARD A5's look: the row's label as a bold 13 header, a rule, then the
 * lines {@link MenuModel} gives. Check and radio items keep the menu open, so several edits can be made in one visit.
 * The colour row's swatch takes the menu down, opens the colour picker, and brings the menu back under the row's
 * button when the picker closes. Every edit is saved at once through the {@link RuleEditor}. Swing thread only.
 *
 * <p>Choice: after an edit every line is set again from the new rule, which is possible because a row's menu keeps the
 * same shape whatever its rule says. Choice: the menu hangs from the button's bottom right corner, so that it opens
 * towards the game and not off the screen's edge.
 */
final class RowMenu
{
	private static final int FONT = 12;
	private static final int HEADER_FONT = 13;
	private static final int SWATCH = 16;
	private static final int HEADER_WIDTH = 200;
	/** A press on the button this soon after the menu closed is the press that closed it, not a new open. */
	private static final long REOPEN_GUARD_NANOS = 250_000_000L;
	private static final String COLOUR_TITLE = "Outline colour";
	private static final int[] WIDTHS = {1, 2, 3, 4, 5, 6};

	private final RuleEditor rules;
	private final ColourOpener colours;
	private final Consumer<String> clipboard;
	private final Consumer<ThingView> remover;
	private JPopupMenu popup;
	private ThingRow closedFor;
	private long closedAtNanos;
	private final List<Consumer<MenuModel.MenuLine>> syncs = new ArrayList<>();

	/**
	 * @param rules where the edits are read and saved
	 * @param colours opens the colour picker
	 * @param clipboard puts text on the clipboard
	 * @param remover takes a wiki row out of the list
	 */
	RowMenu(RuleEditor rules, ColourOpener colours, Consumer<String> clipboard, Consumer<ThingView> remover)
	{
		this.rules = rules;
		this.colours = colours;
		this.clipboard = clipboard;
		this.remover = remover;
	}

	/** Opens the menu of {@code row} under its button; a press that closed the same row's menu a moment ago opens none. */
	void show(ThingRow row)
	{
		if (row == closedFor && System.nanoTime() - closedAtNanos < REOPEN_GUARD_NANOS)
		{
			return;
		}
		showNow(row);
	}

	private void showNow(ThingRow row)
	{
		if (!row.isShowing())
		{
			return;
		}
		if (popup != null)
		{
			popup.setVisible(false);
		}
		build(row);
		final Point corner = row.menuCorner();
		popup.show(row, corner.x - popup.getPreferredSize().width, corner.y);
	}

	private Rule ruleOf(ThingView t)
	{
		return t.changeable() ? rules.get(t.kind(), t.name()) : null;
	}

	private void build(ThingRow row)
	{
		final ThingView t = row.thing();
		final List<MenuModel.MenuLine> lines = MenuModel.of(t, ruleOf(t));
		final JPopupMenu menu = new JPopupMenu();
		menu.setBorder(new EmptyBorder(5, 5, 5, 5));
		syncs.clear();
		ButtonGroup group = null;
		for (MenuModel.MenuLine line : lines)
		{
			if (line.type != MenuModel.Type.RADIO)
			{
				group = null;
			}
			switch (line.type)
			{
				case HEADER:
					menu.add(header(line.text));
					syncs.add(null);
					break;
				case SEPARATOR:
					menu.addSeparator();
					syncs.add(null);
					break;
				case CHECK:
					menu.add(check(line, t));
					break;
				case COLOUR:
					menu.add(colourRow(line, row, menu));
					break;
				case WIDTH:
					menu.add(widthMenu(line, t));
					break;
				case RADIO:
					if (group == null)
					{
						group = new ButtonGroup();
					}
					menu.add(radio(line, t, group));
					break;
				case NOTE:
					menu.add(caption(line.text, ColorScheme.LIGHT_GRAY_COLOR));
					syncs.add(null);
					break;
				default:
					menu.add(item(line, t, menu));
					break;
			}
		}
		menu.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
			{
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
			{
				closedFor = row;
				closedAtNanos = System.nanoTime();
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e)
			{
				closedFor = row;
				closedAtNanos = System.nanoTime();
			}
		});
		popup = menu;
	}

	/** Saves one edit and sets every line of the open menu from the new rule. */
	private void edit(ThingView t, RowAction action, int value)
	{
		rules.put(RuleEdits.apply(t, rules.get(t.kind(), t.name()), action, value));
		final List<MenuModel.MenuLine> lines = MenuModel.of(t, rules.get(t.kind(), t.name()));
		for (int i = 0; i < lines.size() && i < syncs.size(); i++)
		{
			final Consumer<MenuModel.MenuLine> sync = syncs.get(i);
			if (sync != null)
			{
				sync.accept(lines.get(i));
			}
		}
	}

	private static Font menuFont(int size)
	{
		return FontManager.getDefaultFont().deriveFont((float) size);
	}

	private static JLabel header(String text)
	{
		final JLabel label = new JLabel();
		label.setFont(FontManager.getDefaultBoldFont().deriveFont((float) HEADER_FONT));
		label.setForeground(Color.WHITE);
		label.setText(ThingRow.cut(label.getFontMetrics(label.getFont()), text, HEADER_WIDTH));
		return centred(label);
	}

	private static JLabel caption(String text, Color colour)
	{
		final JLabel label = new JLabel(text);
		label.setFont(menuFont(FONT));
		label.setForeground(colour);
		return centred(label);
	}

	/** A label takes the items' centre alignment and the whole width, its words still at its left. */
	private static JLabel centred(JLabel label)
	{
		label.setBorder(new EmptyBorder(3, 10, 3, 6));
		label.setAlignmentX(Component.CENTER_ALIGNMENT);
		label.setMaximumSize(new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height));
		return label;
	}

	private JCheckBoxMenuItem check(MenuModel.MenuLine line, ThingView t)
	{
		final JCheckBoxMenuItem item = new JCheckBoxMenuItem(line.text, line.selected)
		{
			@Override
			protected void processMouseEvent(MouseEvent e)
			{
				if (!takeRelease(this, e, true))
				{
					super.processMouseEvent(e);
				}
			}
		};
		item.setFont(menuFont(FONT));
		item.setForeground(Color.WHITE);
		item.addActionListener(e -> edit(t, line.action, 0));
		syncs.add(next -> item.setSelected(next.selected));
		return item;
	}

	private JRadioButtonMenuItem radio(MenuModel.MenuLine line, ThingView t, ButtonGroup group)
	{
		final JRadioButtonMenuItem item = new JRadioButtonMenuItem(line.text, line.selected)
		{
			@Override
			protected void processMouseEvent(MouseEvent e)
			{
				if (!takeRelease(this, e, true))
				{
					super.processMouseEvent(e);
				}
			}
		};
		item.setFont(menuFont(FONT));
		item.setForeground(Color.WHITE);
		group.add(item);
		item.addActionListener(e -> edit(t, line.action, 0));
		syncs.add(next -> item.setSelected(next.selected));
		return item;
	}

	private JMenu widthMenu(MenuModel.MenuLine line, ThingView t)
	{
		final JMenu menu = new JMenu(line.text);
		menu.setFont(menuFont(FONT));
		menu.setForeground(Color.WHITE);
		final ButtonGroup group = new ButtonGroup();
		final List<JRadioButtonMenuItem> items = new ArrayList<>();
		for (int width : WIDTHS)
		{
			final JRadioButtonMenuItem item = new JRadioButtonMenuItem(String.valueOf(width), line.text.endsWith(": " + width))
			{
				@Override
				protected void processMouseEvent(MouseEvent e)
				{
					if (!takeRelease(this, e, false))
					{
						super.processMouseEvent(e);
					}
				}
			};
			item.setFont(menuFont(FONT));
			item.setForeground(Color.WHITE);
			item.addActionListener(e -> edit(t, RowAction.WIDTH, width));
			group.add(item);
			menu.add(item);
			items.add(item);
		}
		syncs.add(next ->
		{
			menu.setText(next.text);
			for (int i = 0; i < WIDTHS.length; i++)
			{
				items.get(i).setSelected(next.text.endsWith(": " + WIDTHS[i]));
			}
		});
		return menu;
	}

	/** The colour row: the caption at the left, a 16 px swatch at the right that opens the colour picker. */
	private JPanel colourRow(MenuModel.MenuLine line, ThingRow row, JPopupMenu menu)
	{
		final ThingView t = row.thing();
		final JButton swatch = new JButton()
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				g.setColor(getBackground());
				g.fillRect(0, 0, getWidth(), getHeight());
			}
		};
		swatch.setFocusable(false);
		swatch.setContentAreaFilled(false);
		swatch.setOpaque(true);
		swatch.setBorder(new LineBorder(ColorScheme.LIGHT_GRAY_COLOR, 1));
		final Dimension size = new Dimension(SWATCH, SWATCH);
		swatch.setPreferredSize(size);
		swatch.setMinimumSize(size);
		swatch.setMaximumSize(size);
		swatch.setBackground(new Color(line.rgb));
		swatch.addActionListener(e ->
		{
			final Color current = swatch.getBackground();
			menu.setVisible(false);
			colours.open(row, COLOUR_TITLE, current, colour -> edit(t, RowAction.COLOUR, colour.getRGB() & 0xFFFFFF),
				() -> showNow(row));
		});
		syncs.add(next -> swatch.setBackground(new Color(next.rgb)));

		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.setOpaque(false);
		panel.setBorder(new EmptyBorder(2, 10, 2, 6));
		final JLabel caption = new JLabel(line.text);
		caption.setFont(menuFont(FONT));
		caption.setForeground(Color.WHITE);
		panel.add(caption);
		panel.add(Box.createHorizontalStrut(12));
		panel.add(Box.createHorizontalGlue());
		panel.add(swatch);
		panel.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
		return panel;
	}

	private JMenuItem item(MenuModel.MenuLine line, ThingView t, JPopupMenu menu)
	{
		final JMenuItem item = new JMenuItem(line.text);
		item.setFont(menuFont(FONT));
		item.setForeground(Color.WHITE);
		item.addActionListener(e ->
		{
			menu.setVisible(false);
			if (line.action == RowAction.COPY)
			{
				clipboard.accept(t.ids().stream().map(String::valueOf).collect(Collectors.joining(", ")));
			}
			else if (line.action == RowAction.REMOVE)
			{
				remover.accept(t);
			}
		});
		syncs.add(null);
		return item;
	}

	/**
	 * Takes over a left release inside {@code item}: clicks it, consumes the event and tells the caller not to pass it
	 * to Swing's menu item UI, which would take the whole menu down.
	 *
	 * @param rearm whether the item is armed again after the click, to keep its hover highlight; a submenu's item is
	 *        not, since its submenu closes with the click
	 * @return whether the event was a left release inside the item and has been taken
	 */
	private static boolean takeRelease(JMenuItem item, MouseEvent e, boolean rearm)
	{
		if (e.getID() != MouseEvent.MOUSE_RELEASED || !SwingUtilities.isLeftMouseButton(e) || !item.contains(e.getPoint()))
		{
			return false;
		}
		item.doClick(0);
		if (rearm)
		{
			item.getModel().setArmed(true);
		}
		e.consume();
		return true;
	}
}
