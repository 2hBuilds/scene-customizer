package com.scenecustomizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

/** Picks the few things nearest the player to outline. */
final class OutlinePick
{
	private OutlinePick()
	{
	}

	/** The {@code most} items with the smallest distance, nearest first; all of them when there are fewer. */
	static <T> List<T> nearest(List<T> items, ToIntFunction<T> distance, int most)
	{
		final List<T> sorted = new ArrayList<>(items);
		sorted.sort(Comparator.comparingInt(distance));
		return sorted.size() <= most ? sorted : new ArrayList<>(sorted.subList(0, most));
	}
}
