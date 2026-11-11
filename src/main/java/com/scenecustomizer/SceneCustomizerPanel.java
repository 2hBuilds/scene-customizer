package com.scenecustomizer;

import com.scenecustomizer.SceneCustomizerConfig.SidebarIcon;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

/**
 * The sidebar panel: one card whose top row holds the Record button and, at its end, the support pair; under it a
 * grey status line and the two centred message lines; under the card the session picker, the List | Changes toggle
 * and the list of the shown session's rows. Also the settings menu the settings icon opens (STANDARD A4 and A5).
 * Every method here runs on the Swing thread; the panel never touches a Session or the client, only the frozen views
 * it is handed.
 *
 * <p>Choice: the border is 6 px, so that the content is the 213 px the sidebar's layout is built for. Choice: the two
 * message lines of the card go once there is a session to show.
 */
public class SceneCustomizerPanel extends PluginPanel
{
	/** The plugin's GitHub page, what the menu header's fourth mark opens. */
	static final String GITHUB_URL = "https://github.com/2hBuilds/scene-customizer";

	/** The menu's rows. */
	static final String RESET_TEXT = "Reset to default";
	static final String OK_TEXT = "OK";

	/** The card's padding, in px. */
	private static final int PADDING = 8;
	/** The panel's own border, in px: what leaves the content 213 px wide. */
	private static final int BORDER = 6;
	/** The gap between the parts under the card, in px. */
	private static final int GAP = 6;
	/** The card's message. */
	private static final String HEADLINE = "Nothing recorded yet";
	private static final String HELPER = "Press Record, then play";
	/** The status line before any recording. */
	private static final String NOT_RECORDING = "Not recording";
	/** The Record button's two words. */
	private static final String RECORD_TEXT = "Record";
	private static final String STOP_TEXT = "Stop";
	/** The settings icon's size, in px. */
	private static final int ICON_SIZE = 12;
	/** A press on the settings icon this soon after the menu closed is the press that closed it, not a new open. */
	private static final long REOPEN_GUARD_NANOS = 250_000_000L;

	/** The settings menu's check item for the wiki. */
	static final String WIKI_TEXT = "Use the OSRS wiki";
	/** The settings menu's submenu of sidebar pictures; its title adds the current one's words. */
	static final String ICON_TEXT = "Sidebar icon";

	private final Runnable resetToDefault;
	private final LookupBar lookup;
	private final BooleanSupplier wikiOn;
	private final Consumer<Boolean> wikiSwitch;
	private JCheckBoxMenuItem wikiItem;
	private final Supplier<SidebarIcon> iconNow;
	private final Consumer<SidebarIcon> iconSwitch;
	private JMenu iconMenu;
	private final List<JRadioButtonMenuItem> iconItems = new ArrayList<>();
	/** True while the menu is being made to show the stored settings: the controls then write nothing. */
	private boolean syncing;
	private final Runnable toggleRecording;
	private final JButton recordButton = new JButton(RECORD_TEXT);
	private final JLabel statusLine = new JLabel(NOT_RECORDING);
	private final ListView list;
	private final SessionPicker picker;
	private final ChangesView changes;
	private List<SessionView> sessions = new ArrayList<>();
	private int shownIndex;
	private JPanel messageLines;
	private final SupportLinks support;
	private final JPopupMenu menu;
	private long closedAtNanos = System.nanoTime() - 2 * REOPEN_GUARD_NANOS;

	/**
	 * @param resetToDefault what "Reset to default" does: puts the plugin's settings back to their defaults
	 * @param toggleRecording what the Record button does when pressed (it must hand the work to the client thread)
	 * @param rules where the rows' rules are read and the menu's edits saved
	 * @param colours opens RuneLite's colour picker
	 * @param clipboard puts text on the clipboard
	 * @param remover told the shown session and the key of a wiki row to take out of it (it must hand the work to the
	 *        client thread)
	 * @param lookup the look-up bar, shown between the card and the session picker
	 * @param wikiOn whether the wiki setting is on now
	 * @param wikiSwitch told the new value when the settings menu's wiki item is flipped
	 * @param iconNow the sidebar icon the setting holds now
	 * @param iconSwitch told the new choice when one is picked in the settings menu's "Sidebar icon" submenu
	 */
	SceneCustomizerPanel(Runnable resetToDefault, Runnable toggleRecording, RuleEditor rules, ColourOpener colours,
		Consumer<String> clipboard, BiConsumer<SessionView, ThingKey> remover, LookupBar lookup, BooleanSupplier wikiOn,
		Consumer<Boolean> wikiSwitch, Supplier<SidebarIcon> iconNow, Consumer<SidebarIcon> iconSwitch)
	{
		super();
		this.resetToDefault = resetToDefault;
		this.toggleRecording = toggleRecording;
		this.lookup = lookup;
		this.wikiOn = wikiOn;
		this.wikiSwitch = wikiSwitch;
		this.iconNow = iconNow;
		this.iconSwitch = iconSwitch;
		this.support = new SupportLinks("2h NPC & Object Customizer", Version.CURRENT, GITHUB_URL, LinkBrowser::browse, this::closeMenu);
		this.menu = buildMenu();

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(BORDER, BORDER, BORDER, BORDER));
		list = new ListView(rules, colours, clipboard, remover);
		picker = new SessionPicker(this::pickSession);
		changes = new ChangesView(rules);
		rules.addListener(() -> SwingUtilities.invokeLater(list::refreshBadges));
		final JPanel column = new JPanel();
		column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
		column.setOpaque(false);
		column.add(left(messageCard()));
		column.add(Box.createVerticalStrut(GAP));
		column.add(left(lookup));
		column.add(Box.createVerticalStrut(GAP));
		column.add(left(picker));
		column.add(Box.createVerticalStrut(GAP));
		final ViewToggle toggle = new ViewToggle("List", "Changes", index ->
		{
			list.setVisible(index == 0);
			changes.setVisible(index == 1);
		});
		column.add(left(toggle));
		column.add(Box.createVerticalStrut(GAP));
		changes.setVisible(false);
		column.add(left(changes));
		column.add(left(list));
		picker.setSessions(sessions, 0);
		add(column, BorderLayout.NORTH);
		wikiChanged();
	}

	/** Makes the menu's wiki item and the look-up bar match the setting as it stands now. */
	void wikiChanged()
	{
		final boolean on = wikiOn.getAsBoolean();
		wikiItem.setSelected(on);
		lookup.setWikiOn(on);
	}

	/** Sets the grey status line. */
	void setStatus(String text)
	{
		statusLine.setText(text);
	}

	/**
	 * Takes the kept sessions (recordings newest first, then look-ups newest first). The session just stopped, when
	 * there is one, is shown; else the one shown stays shown (or the newest, when it is gone).
	 *
	 * @param all a frozen view of every kept session
	 * @param justStopped the view of the recording that has just stopped, or null
	 */
	void sessionsChanged(List<SessionView> all, SessionView justStopped)
	{
		final SessionView before = shownIndex < sessions.size() ? sessions.get(shownIndex) : null;
		sessions = new ArrayList<>(all);
		shownIndex = indexOf(justStopped != null ? justStopped : before);
		messageLines.setVisible(sessions.isEmpty());
		showSession();
	}

	/** The index of the session that began when {@code wanted} did, or 0. */
	private int indexOf(SessionView wanted)
	{
		for (int i = 0; wanted != null && i < sessions.size(); i++)
		{
			final SessionView view = sessions.get(i);
			if (view.kind() == wanted.kind() && view.startedEpochMs() == wanted.startedEpochMs()
				&& view.title().equals(wanted.title()))
			{
				return i;
			}
		}
		return 0;
	}

	private void pickSession(int index)
	{
		shownIndex = index;
		showSession();
	}

	private void showSession()
	{
		picker.setSessions(sessions, shownIndex);
		list.show(shownIndex < sessions.size() ? sessions.get(shownIndex) : null);
	}

	private static <T extends JComponent> T left(T component)
	{
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
		return component;
	}

	/** Sets the Record button's word: Stop while recording. */
	void setRecording(boolean recording)
	{
		recordButton.setText(recording ? STOP_TEXT : RECORD_TEXT);
	}

	/** The message card: the top row with the support pair at its right end, then the two centred lines. */
	private JPanel messageCard()
	{
		final JPanel card = new JPanel(new BorderLayout());
		card.setBackground(ColorScheme.DARK_GRAY_COLOR);
		card.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));

		final JPanel topRow = new JPanel(new BorderLayout());
		topRow.setOpaque(false);
		recordButton.setFont(FontManager.getDefaultFont().deriveFont(12f));
		recordButton.setFocusable(false);
		recordButton.addActionListener(e -> toggleRecording.run());
		topRow.add(recordButton, BorderLayout.WEST);
		topRow.add(support.supportPair(settingsIcon()), BorderLayout.EAST);
		statusLine.setFont(FontManager.getDefaultFont().deriveFont(11f));
		statusLine.setForeground(new Color(165, 165, 165));
		statusLine.setBorder(new EmptyBorder(PADDING / 2, 0, 0, 0));
		final JPanel north = new JPanel(new BorderLayout());
		north.setOpaque(false);
		north.add(topRow, BorderLayout.NORTH);
		north.add(statusLine, BorderLayout.SOUTH);
		card.add(north, BorderLayout.NORTH);

		final JPanel lines = new JPanel();
		messageLines = lines;
		lines.setLayout(new BoxLayout(lines, BoxLayout.Y_AXIS));
		lines.setOpaque(false);
		lines.setBorder(new EmptyBorder(PADDING, 0, PADDING, 0));
		lines.add(line(HEADLINE, FontManager.getDefaultBoldFont().deriveFont(14f), Color.WHITE));
		lines.add(Box.createVerticalStrut(4));
		lines.add(line(HELPER, FontManager.getDefaultFont().deriveFont(12f), ColorScheme.LIGHT_GRAY_COLOR));
		card.add(lines, BorderLayout.CENTER);
		return card;
	}

	/** One centred line of the card. */
	private static JLabel line(String text, Font font, Color colour)
	{
		final JLabel label = new JLabel(text, SwingConstants.CENTER);
		label.setFont(font);
		label.setForeground(colour);
		label.setAlignmentX(Component.CENTER_ALIGNMENT);
		return label;
	}

	/** The settings icon: grey at rest, orange under the mouse; a press opens the menu under it. */
	private JLabel settingsIcon()
	{
		final JLabel[] self = new JLabel[1];
		self[0] = SupportLinks.namedIconButton(GearGlyph.icon(ICON_SIZE, ColorScheme.LIGHT_GRAY_COLOR),
			GearGlyph.icon(ICON_SIZE, ColorScheme.BRAND_ORANGE), SupportLinks.SETTINGS_TIP, () -> openMenu(self[0]));
		self[0].setBorder(SupportLinks.iconBorder());
		return self[0];
	}

	/** Opens the menu under {@code anchor}; the press that closed it a moment ago opens nothing, so the icon toggles. */
	private void openMenu(JLabel anchor)
	{
		if (!anchor.isShowing() || System.nanoTime() - closedAtNanos < REOPEN_GUARD_NANOS)
		{
			return;
		}
		menu.show(anchor, 0, anchor.getHeight());
	}

	/** Takes the menu down: OK, a link press, or the reset. */
	void closeMenu()
	{
		menu.setVisible(false);
	}

	/** The settings menu: the header, a rule, then the OK row. */
	private JPopupMenu buildMenu()
	{
		final JPopupMenu popup = new JPopupMenu();
		popup.setBorder(new EmptyBorder(5, 5, 5, 5));
		popup.add(support.header());
		popup.addSeparator();
		popup.add(wikiRow());
		popup.addSeparator();
		popup.add(iconSubmenu());
		popup.addSeparator();
		popup.add(okRow());
		popup.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
			{
				syncControls();
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
		return popup;
	}

	/** The wiki check item: it takes the mouse release itself, so flipping it leaves the menu open. */
	private JCheckBoxMenuItem wikiRow()
	{
		wikiItem = new JCheckBoxMenuItem(WIKI_TEXT, wikiOn.getAsBoolean())
		{
			@Override
			protected void processMouseEvent(MouseEvent e)
			{
				if (e.getID() == MouseEvent.MOUSE_RELEASED && SwingUtilities.isLeftMouseButton(e) && contains(e.getPoint()))
				{
					doClick(0);
					getModel().setArmed(true);
					e.consume();
				}
				else
				{
					super.processMouseEvent(e);
				}
			}
		};
		wikiItem.setFont(FontManager.getDefaultFont().deriveFont(12f));
		wikiItem.setForeground(Color.WHITE);
		wikiItem.addActionListener(e ->
		{
			final boolean on = wikiItem.isSelected();
			wikiSwitch.accept(on);
			lookup.setWikiOn(on);
		});
		return wikiItem;
	}

	/**
	 * The "Sidebar icon" submenu: the ten choices as radio items in one group, each showing its picture (the very image
	 * the sidebar button would show) left of its words. A pick keeps the menu and the submenu open and writes the setting.
	 */
	private JMenu iconSubmenu()
	{
		iconMenu = new JMenu(ICON_TEXT);
		iconMenu.setFont(FontManager.getDefaultFont().deriveFont(12f));
		iconMenu.setForeground(Color.WHITE);
		final ButtonGroup group = new ButtonGroup();
		for (final SidebarIcon icon : SidebarIcon.values())
		{
			final JRadioButtonMenuItem item = new JRadioButtonMenuItem(String.valueOf(icon), new ImageIcon(NavIcon.create(icon)))
			{
				@Override
				protected void processMouseEvent(MouseEvent e)
				{
					if (!takeRelease(this, e))
					{
						super.processMouseEvent(e);
					}
				}
			};
			item.setFont(FontManager.getDefaultFont().deriveFont(12f));
			item.setForeground(Color.WHITE);
			item.addActionListener(e ->
			{
				if (!syncing)
				{
					iconSwitch.accept(icon);
					iconMenu.setText(ICON_TEXT + ": " + icon);
				}
			});
			group.add(item);
			iconItems.add(item);
			iconMenu.add(item);
		}
		return iconMenu;
	}

	/**
	 * Shows the stored sidebar icon without writing it back: when the menu opens, and after "Reset to default". The
	 * submenu's title carries the current choice's words and its radio item is the one selected.
	 */
	private void syncControls()
	{
		syncing = true;
		final SidebarIcon icon = iconNow.get();
		iconMenu.setText(ICON_TEXT + ": " + icon);
		iconItems.get(icon.ordinal()).setSelected(true);
		syncing = false;
	}

	/** Takes a left release inside {@code item} over: clicks it and keeps Swing from closing the menu. */
	private static boolean takeRelease(JMenuItem item, MouseEvent e)
	{
		if (e.getID() != MouseEvent.MOUSE_RELEASED || !SwingUtilities.isLeftMouseButton(e) || !item.contains(e.getPoint()))
		{
			return false;
		}
		item.doClick(0);
		// The click leaves the item un-armed; arming it again keeps its hover highlight while the mouse stays on it.
		item.getModel().setArmed(true);
		e.consume();
		return true;
	}

	/** The menu's last row: "Reset to default" at its left, "OK" at its right. */
	private JPanel okRow()
	{
		final JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.setOpaque(false);
		row.setBorder(new EmptyBorder(2, 6, 2, 6));
		final JButton reset = button(RESET_TEXT);
		reset.addActionListener(e ->
		{
			resetToDefault.run();
			wikiChanged();
			syncControls();
		});
		final JButton ok = button(OK_TEXT);
		ok.addActionListener(e -> closeMenu());
		row.add(reset);
		row.add(Box.createHorizontalGlue());
		row.add(ok);
		return row;
	}

	/** A small button in the menu's 12 px type, no wider than its word. */
	private static JButton button(String text)
	{
		final JButton button = new JButton(text);
		button.setFont(FontManager.getDefaultFont().deriveFont(12f));
		button.setFocusable(false);
		button.setMaximumSize(button.getPreferredSize());
		return button;
	}
}
