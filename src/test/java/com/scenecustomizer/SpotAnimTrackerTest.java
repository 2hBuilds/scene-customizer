package com.scenecustomizer;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/** {@link SpotAnimTracker}: only the graphics an NPC did not carry before are new. */
public class SpotAnimTrackerTest
{
	@Test
	public void onlyNewIdsCount()
	{
		final SpotAnimTracker tracker = new SpotAnimTracker();
		final Object a = new Object();
		final Object b = new Object();
		assertEquals(Arrays.asList(10, 11), tracker.newIds(a, Arrays.asList(10, 11)));
		assertEquals(Arrays.asList(12), tracker.newIds(a, Arrays.asList(11, 12)));
		assertEquals(Arrays.asList(11), tracker.newIds(b, Arrays.asList(11)));
		tracker.forget(a);
		assertEquals(Arrays.asList(11), tracker.newIds(a, Arrays.asList(11)));
	}
}
