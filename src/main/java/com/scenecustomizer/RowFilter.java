package com.scenecustomizer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Which rows of a session the filter box lets through. Pure. */
final class RowFilter
{
	private RowFilter()
	{
	}

	/**
	 * The rows whose label holds the query, ignoring case, by section in the view's order; a blank query keeps every
	 * row. Sections left empty are dropped.
	 */
	static Map<Section, List<ThingView>> filter(SessionView view, String query)
	{
		final String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		final Map<Section, List<ThingView>> out = new EnumMap<>(Section.class);
		for (Map.Entry<Section, List<ThingView>> entry : view.sections().entrySet())
		{
			final List<ThingView> kept = new ArrayList<>();
			for (ThingView t : entry.getValue())
			{
				if (needle.isEmpty() || t.label().toLowerCase(Locale.ROOT).contains(needle))
				{
					kept.add(t);
				}
			}
			if (!kept.isEmpty())
			{
				out.put(entry.getKey(), kept);
			}
		}
		return out;
	}
}
