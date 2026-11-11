package com.scenecustomizer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** The words of a row's second line: ids, times seen and the first-seen time. Pure. */
final class RowText
{
	/** The gap between the parts of a second line. */
	private static final String GAP = "   ";
	/** How many ids the line names before it counts the rest. */
	private static final int IDS_SHOWN = 3;

	private RowText()
	{
	}

	/** A time as {@code m:ss} under an hour, else {@code h:mm:ss}. */
	static String time(long ms)
	{
		final long total = Math.max(0L, ms) / 1000L;
		final long hours = total / 3600L;
		final long minutes = (total / 60L) % 60L;
		final long seconds = total % 60L;
		if (hours == 0L)
		{
			return String.format("%d:%02d", total / 60L, seconds);
		}
		return String.format("%d:%02d:%02d", hours, minutes, seconds);
	}

	/** The first three ids joined by a comma and a space, then a plus and the rest of the list and {@code moreIds}. */
	static String ids(List<Integer> ids, int moreIds)
	{
		final String shown = ids.stream().limit(IDS_SHOWN).map(String::valueOf).collect(Collectors.joining(", "));
		final int rest = Math.max(0, ids.size() - IDS_SHOWN) + Math.max(0, moreIds);
		if (rest == 0)
		{
			return shown;
		}
		return shown.isEmpty() ? "+" + rest : shown + " +" + rest;
	}

	/** The second line of a row, its parts joined by three spaces. */
	static String secondLine(ThingView t)
	{
		final List<String> parts = new ArrayList<>();
		final String ids = ids(t.ids(), t.moreIds());
		if (t.source() == Source.WIKI)
		{
			parts.add("From the wiki");
			addIfAny(parts, ids);
		}
		else
		{
			switch (t.kind())
			{
				case NPC:
				case OBJECT:
					addIfAny(parts, ids);
					break;
				case EFFECT:
				case ANIMATION:
					if (!t.hints().isEmpty())
					{
						parts.add("on " + String.join(", ", t.hints()));
					}
					break;
				default:
					break;
			}
			parts.add("seen " + t.seen());
			parts.add(time(t.firstSeenMs()));
		}
		return String.join(GAP, parts);
	}

	private static void addIfAny(List<String> parts, String part)
	{
		if (!part.isEmpty())
		{
			parts.add(part);
		}
	}
}
