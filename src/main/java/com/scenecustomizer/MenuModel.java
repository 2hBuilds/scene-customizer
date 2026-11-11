package com.scenecustomizer;

import java.util.ArrayList;
import java.util.List;

/**
 * The lines of a row's menu, in order. Pure. Choice: a wiki row's "Remove from this list" follows the last line
 * directly, with no rule of its own, since the plan adds exactly one more item.
 */
final class MenuModel
{
	/** The sorts of line. */
	enum Type
	{
		/** The row's label. */
		HEADER,
		/** A rule between groups. */
		SEPARATOR,
		/** A switch. */
		CHECK,
		/** A colour row with a swatch. */
		COLOUR,
		/** The width submenu. */
		WIDTH,
		/** One choice of a group. */
		RADIO,
		/** A grey line saying why. */
		NOTE,
		/** A plain item. */
		ITEM
	}

	/** One line of the menu. */
	static final class MenuLine
	{
		/** The sort of line. */
		final Type type;
		/** The words. */
		final String text;
		/** Whether a check or radio is on. */
		final boolean selected;
		/** The swatch colour, 0xRRGGBB (COLOUR only). */
		final int rgb;
		/** What a press does, or null. */
		final RowAction action;

		MenuLine(Type type, String text, boolean selected, int rgb, RowAction action)
		{
			this.type = type;
			this.text = text;
			this.selected = selected;
			this.rgb = rgb;
			this.action = action;
		}
	}

	/** Text of the copy item. */
	static final String COPY_TEXT = "Copy id";
	/** Text of the remove item. */
	static final String REMOVE_TEXT = "Remove from this list";

	private MenuModel()
	{
	}

	/** The menu of a row; the rule may be null. */
	static List<MenuLine> of(ThingView t, Rule rule)
	{
		final List<MenuLine> lines = new ArrayList<>();
		lines.add(new MenuLine(Type.HEADER, t.label(), false, 0, null));
		lines.add(separator());
		if (t.changeable())
		{
			changeable(lines, t, rule);
		}
		else
		{
			lines.add(new MenuLine(Type.NOTE, why(t), false, 0, null));
			if (!t.ids().isEmpty())
			{
				lines.add(separator());
				lines.add(copy());
			}
		}
		if (t.source() == Source.WIKI)
		{
			lines.add(new MenuLine(Type.ITEM, REMOVE_TEXT, false, 0, RowAction.REMOVE));
		}
		return lines;
	}

	private static void changeable(List<MenuLine> lines, ThingView t, Rule rule)
	{
		final boolean object = t.kind() == Kind.OBJECT;
		final Outline outline = rule == null ? null : rule.outline;
		final HideScope hide = rule == null ? HideScope.OFF : rule.hide;
		lines.add(new MenuLine(Type.CHECK, object ? "Outline here" : "Outline", outline != null, 0, RowAction.OUTLINE));
		lines.add(new MenuLine(Type.COLOUR, "Colour", false, outline == null ? RuleEdits.DEFAULT_RGB : outline.rgb,
			RowAction.COLOUR));
		lines.add(new MenuLine(Type.WIDTH, "Width: " + (outline == null ? RuleEdits.DEFAULT_WIDTH : outline.width),
			false, 0, RowAction.WIDTH));
		lines.add(separator());
		if (object)
		{
			lines.add(new MenuLine(Type.RADIO, "Show", hide == HideScope.OFF, 0, RowAction.SHOW));
			lines.add(new MenuLine(Type.RADIO, "Hide here", hide == HideScope.HERE, 0, RowAction.HIDE_HERE));
			lines.add(new MenuLine(Type.RADIO, "Hide everywhere", hide == HideScope.EVERYWHERE, 0,
				RowAction.HIDE_EVERYWHERE));
		}
		else
		{
			lines.add(new MenuLine(Type.CHECK, "Hide", hide == HideScope.EVERYWHERE, 0, RowAction.HIDE));
		}
		lines.add(separator());
		lines.add(copy());
	}

	private static String why(ThingView t)
	{
		if (t.kind() == Kind.NPC || t.kind() == Kind.OBJECT)
		{
			return t.nameless() ? "No name, so it can't be changed" : "The Plugin Hub doesn't allow changing this";
		}
		return "Shown only";
	}

	private static MenuLine separator()
	{
		return new MenuLine(Type.SEPARATOR, "", false, 0, null);
	}

	private static MenuLine copy()
	{
		return new MenuLine(Type.ITEM, COPY_TEXT, false, 0, RowAction.COPY);
	}
}
