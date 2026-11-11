package com.scenecustomizer;

import net.runelite.api.MenuAction;

/** Reads a spell cast out of a menu click. */
final class SpellNames
{
	private SpellNames()
	{
	}

	/**
	 * The spell's name when the click is a cast, else null. The target is cut at its first {@code ->}, so the name of
	 * whatever it was cast on, a player above all, is never kept.
	 */
	static String fromMenu(String option, MenuAction action, String target)
	{
		if (!"Cast".equals(option) || action == null || target == null)
		{
			return null;
		}
		switch (action)
		{
			case WIDGET_TARGET_ON_NPC:
			case WIDGET_TARGET_ON_PLAYER:
			case WIDGET_TARGET_ON_GAME_OBJECT:
			case WIDGET_TARGET_ON_GROUND_ITEM:
			case WIDGET_TARGET_ON_WIDGET:
			case CC_OP:
			case CC_OP_LOW_PRIORITY:
				break;
			default:
				return null;
		}
		final String plain = Names.clean(target);
		if (plain == null)
		{
			return null;
		}
		final int arrow = plain.indexOf("->");
		return Names.clean(arrow < 0 ? plain : plain.substring(0, arrow));
	}
}
