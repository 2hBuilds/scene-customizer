package com.scenecustomizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Remembers which graphics each NPC carried last time, so a graphic held for many ticks counts once. */
final class SpotAnimTracker
{
	private final Map<Object, Set<Integer>> last = new IdentityHashMap<>();

	/** The ids in {@code current} that the NPC did not carry before, in {@code current}'s order. */
	List<Integer> newIds(Object npc, Collection<Integer> current)
	{
		final Set<Integer> before = last.get(npc);
		final List<Integer> fresh = new ArrayList<>();
		for (Integer id : current)
		{
			if (before == null || !before.contains(id))
			{
				fresh.add(id);
			}
		}
		last.put(npc, new HashSet<>(current));
		return fresh;
	}

	/** Drops one NPC's set. */
	void forget(Object npc)
	{
		last.remove(npc);
	}

	/** Drops every set. */
	void clear()
	{
		last.clear();
	}
}
