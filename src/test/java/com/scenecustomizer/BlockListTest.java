package com.scenecustomizer;

import java.util.Arrays;
import java.util.Collections;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** {@link BlockList}: the twelve ids and nothing else. */
public class BlockListTest
{
	@Test
	public void theTwelveIdsAreBlocked()
	{
		final int[] ids = {ObjectID.TOB_BLOAT_PILLAR, ObjectID.TOB_BLOAT_CHAMBER, ObjectID.TOB_SOTETSEG_PLAINTILE,
			ObjectID.TOB_SOTETSEG_DARKTILE, ObjectID.TOB_SOTETSEG_LIGHTTILE, ObjectID.TOB_SOTETSEG_TRIGGEREDTILE,
			ObjectID.TOB_SOTETSEG_HARD_DARKTILE, ObjectID.TOB_SOTETSEG_HARD_LIGHTTILE,
			ObjectID.TOB_SOTETSEG_HARD_LIGHTTILE_PARENT_1, ObjectID.TOB_SOTETSEG_HARD_LIGHTTILE_PARENT_2,
			ObjectID.TOB_SOTETSEG_HARD_LIGHTTILE_PARENT_3, ObjectID.TOB_SOTETSEG_HARD_LIGHTTILE_PARENT_4};
		for (int id : ids)
		{
			assertTrue(BlockList.isBlocked(id));
		}
		assertEquals(12, BlockList.size());
	}

	@Test
	public void otherIdsAreNot()
	{
		assertFalse(BlockList.isBlocked(-1));
		assertFalse(BlockList.isBlocked(0));
		assertFalse(BlockList.isBlocked(1));
	}

	@Test
	public void anyBlocked()
	{
		assertTrue(BlockList.anyBlocked(Arrays.asList(1, 2, ObjectID.TOB_BLOAT_PILLAR)));
		assertFalse(BlockList.anyBlocked(Arrays.asList(1, 2)));
		assertFalse(BlockList.anyBlocked(Collections.<Integer>emptyList()));
	}
}
