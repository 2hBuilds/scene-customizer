package com.scenecustomizer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.gameval.ObjectID;

/** The object ids that can never be changed: the ones the two accepted hiders also refuse. */
final class BlockList
{
	private static final Set<Integer> BLOCKED = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
		ObjectID.TOB_BLOAT_PILLAR,
		ObjectID.TOB_BLOAT_CHAMBER,
		ObjectID.TOB_SOTETSEG_PLAINTILE,
		ObjectID.TOB_SOTETSEG_DARKTILE,
		ObjectID.TOB_SOTETSEG_LIGHTTILE,
		ObjectID.TOB_SOTETSEG_TRIGGEREDTILE,
		ObjectID.TOB_SOTETSEG_HARD_DARKTILE,
		ObjectID.TOB_SOTETSEG_HARD_LIGHTTILE,
		ObjectID.TOB_SOTETSEG_HARD_LIGHTTILE_PARENT_1,
		ObjectID.TOB_SOTETSEG_HARD_LIGHTTILE_PARENT_2,
		ObjectID.TOB_SOTETSEG_HARD_LIGHTTILE_PARENT_3,
		ObjectID.TOB_SOTETSEG_HARD_LIGHTTILE_PARENT_4)));

	private BlockList()
	{
	}

	/** True when the id is on the list. */
	static boolean isBlocked(int id)
	{
		return BLOCKED.contains(id);
	}

	/** True when any of the ids is on the list. */
	static boolean anyBlocked(Collection<Integer> ids)
	{
		for (int id : ids)
		{
			if (BLOCKED.contains(id))
			{
				return true;
			}
		}
		return false;
	}

	/** How many ids are on the list. */
	static int size()
	{
		return BLOCKED.size();
	}
}
