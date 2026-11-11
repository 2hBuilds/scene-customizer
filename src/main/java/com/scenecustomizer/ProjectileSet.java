package com.scenecustomizer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;

/** Remembers the last 1,000 projectiles by identity, so a projectile moving for many ticks counts once. */
final class ProjectileSet
{
	private static final int LIMIT = 1000;

	private final Map<Object, Boolean> known = new IdentityHashMap<>();
	private final Deque<Object> order = new ArrayDeque<>();

	/** True when the projectile is new; past 1,000 the oldest is forgotten. */
	boolean add(Object projectile)
	{
		if (known.containsKey(projectile))
		{
			return false;
		}
		known.put(projectile, Boolean.TRUE);
		order.addLast(projectile);
		while (order.size() > LIMIT)
		{
			known.remove(order.removeFirst());
		}
		return true;
	}

	/** Forgets everything. */
	void clear()
	{
		known.clear();
		order.clear();
	}
}
