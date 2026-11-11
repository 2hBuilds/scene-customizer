package com.scenecustomizer;

import java.awt.Component;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * The List half of the sidebar: the filter box, then the shown session's sections, each a header and its rows, with
 * "and N more not listed" under a section whose cap turned rows away. A press on a header folds or opens its section;
 * the state lasts while the panel lives, and "Scenery nearby" starts folded. While the filter holds text every section
 * with a match is open. Swing thread only.
 */
final class ListView extends JPanel
{
	/** The sidebar's content width at RuneLite's default, in px. */
	static final int WIDTH = 213;
	private static final String FILTER_HINT = "Filter";
	private static final String NOTHING_TEXT = "Nothing matches";
	private static final int GAP = 6;
	/** A look-up's last row. */
	static final String LOOKUP_NOTE = "Effects and animations are not on the wiki. Record while you play to see them.";
	/**
	 * A wrapped note's width in CSS px. A CSS px in Swing's HTML is 1.3 screen pixels, so 150 is 195 on screen, which
	 * fits the panel with or without the list's scroll bar.
	 */
	private static final int NOTE_WIDTH = 150;
	private static final int ACTIONS_X = 46;

	private final RuleEditor rules;
	private final RowMenu menu;
	private final Map<Section, Boolean> open = new EnumMap<>(Section.class);
	private final List<ThingRow> rows = new ArrayList<>();
	private final HintField filter;
	private final JPanel content = new JPanel();
	private SessionView shown;
	private String query = "";

	/**
	 * @param rules where the rules are read
	 * @param colours opens the colour picker
	 * @param clipboard puts text on the clipboard
	 * @param remover told the shown session and the key of a wiki row to take out of it
	 */
	ListView(RuleEditor rules, ColourOpener colours, Consumer<String> clipboard, BiConsumer<SessionView, ThingKey> remover)
	{
		this.rules = rules;
		this.menu = new RowMenu(rules, colours, clipboard, t ->
		{
			final SessionView from = shown;
			if (from != null)
			{
				remover.accept(from, t.key());
			}
		});
		for (Section section : Section.values())
		{
			open.put(section, !section.foldedAtFirst());
		}
		setOpaque(false);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		filter = new HintField(FILTER_HINT, text ->
		{
			query = text;
			rebuild();
		});
		add(left(filter));
		add(Box.createVerticalStrut(GAP));
		content.setOpaque(false);
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		add(left(content));
	}

	/** Shows a session's rows (the filter keeps its text). */
	void show(SessionView view)
	{
		shown = view;
		rebuild();
	}

	/** Sets every row's badge from the rules as they stand now. */
	void refreshBadges()
	{
		for (ThingRow row : rows)
		{
			final ThingView t = row.thing();
			row.setRule(t.changeable() ? rules.get(t.kind(), t.name()) : null);
		}
	}

	private static <T extends JComponent> T left(T component)
	{
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
		return component;
	}

	private void rebuild()
	{
		content.removeAll();
		rows.clear();
		final SessionView view = shown;
		if (view != null)
		{
			final boolean lookup = view.kind() == Session.SessionKind.LOOKUP;
			if (lookup && view.state() == Session.SessionState.LOADING)
			{
				content.add(left(wrapped(LookupBar.LOOKING_TEXT, 12f)));
			}
			if (lookup && view.state() == Session.SessionState.FAILED)
			{
				content.add(left(wrapped(LookupBar.FAILED_TEXT, 12f)));
			}
			final boolean filtering = !query.trim().isEmpty();
			final Map<Section, List<ThingView>> kept = RowFilter.filter(view, query);
			for (Map.Entry<Section, List<ThingView>> entry : kept.entrySet())
			{
				addSection(view, entry.getKey(), entry.getValue(), filtering || open.get(entry.getKey()));
			}
			if (filtering && kept.isEmpty())
			{
				content.add(left(note(NOTHING_TEXT, 12f)));
			}
			if (lookup && view.state() == Session.SessionState.DONE)
			{
				content.add(left(wrapped(LOOKUP_NOTE, 11f)));
			}
		}
		content.revalidate();
		content.repaint();
	}

	private void addSection(SessionView view, Section section, List<ThingView> things, boolean isOpen)
	{
		content.add(left(new SectionHeader(section, things.size(), isOpen, () ->
		{
			open.put(section, !open.get(section));
			rebuild();
		})));
		if (isOpen)
		{
			for (ThingView t : things)
			{
				final ThingRow row = new ThingRow(t, t.changeable() ? rules.get(t.kind(), t.name()) : null, menu::show);
				rows.add(row);
				content.add(left(row));
				if (section == Section.THIS_MONSTER && !t.actions().isEmpty())
				{
					content.add(left(actionsLine(t)));
				}
			}
			if (view.dropped(section) > 0)
			{
				content.add(left(note("and " + view.dropped(section) + " more not listed", 11f)));
			}
		}
		content.add(Box.createVerticalStrut(2));
	}

	/** A This monster row's right-click options, in grey, under the row and in line with its words. */
	private static JLabel actionsLine(ThingView t)
	{
		final JLabel label = new JLabel(String.join(", ", t.actions()));
		label.setFont(FontManager.getDefaultFont().deriveFont(11f));
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setBorder(new EmptyBorder(0, ACTIONS_X, 3, 0));
		return label;
	}

	/** A grey note that wraps inside the sidebar's width. */
	private static JLabel wrapped(String text, float size)
	{
		return note("<html><body style='width:" + NOTE_WIDTH + "px'>" + text + "</body></html>", size);
	}

	private static JLabel note(String text, float size)
	{
		final JLabel label = new JLabel(text);
		label.setFont(FontManager.getDefaultFont().deriveFont(size));
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setBorder(new EmptyBorder(3, 6, 3, 0));
		return label;
	}
}
