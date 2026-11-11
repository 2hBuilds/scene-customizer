package com.scenecustomizer;

import java.util.regex.Pattern;

/** The game's names, cleaned. */
final class Names
{
	private static final Pattern TAG = Pattern.compile("<[^>]*>");

	private Names()
	{
	}

	/**
	 * The name with every {@code <...>} tag removed and trimmed, or null when nothing is left, or the text is
	 * exactly {@code null} (the game's word for no name).
	 */
	static String clean(String raw)
	{
		if (raw == null)
		{
			return null;
		}
		final String name = TAG.matcher(raw).replaceAll("").trim();
		if (name.isEmpty() || name.equals("null"))
		{
			return null;
		}
		return name;
	}
}
