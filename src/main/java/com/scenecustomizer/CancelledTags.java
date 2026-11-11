package com.scenecustomizer;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The last 64 cancelled call tags, the oldest forgotten first. A call asked under a remembered tag is never started,
 * so a late callback of a cancelled look-up cannot start calls that nobody will cancel. Pure; callers guard it.
 */
final class CancelledTags
{
	private static final int MOST = 64;

	private final Set<Object> tags = new LinkedHashSet<>();

	/** Writes down that the tag was cancelled. */
	void remember(Object tag)
	{
		tags.remove(tag);
		tags.add(tag);
		if (tags.size() > MOST)
		{
			final Iterator<Object> oldest = tags.iterator();
			oldest.next();
			oldest.remove();
		}
	}

	/** True when the tag is one of the last 64 cancelled. */
	boolean contains(Object tag)
	{
		return tags.contains(tag);
	}
}
