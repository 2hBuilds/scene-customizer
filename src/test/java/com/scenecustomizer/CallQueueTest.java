package com.scenecustomizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** {@link CallQueue}: calls over the budget wait first-in first-out instead of failing. */
public class CallQueueTest
{
	private static final long SECOND = 1_000_000_000L;

	private final long[] now = {0L};
	private final CallQueue queue = new CallQueue(() -> now[0]);
	private final List<Integer> started = new ArrayList<>();
	private final List<Integer> refused = new ArrayList<>();

	private void ask(Object tag, int n)
	{
		queue.ask(tag, () -> started.add(n), () -> refused.add(n));
	}

	@Test
	public void overTenWaitsAndStartsInOrder()
	{
		final Object tag = new Object();
		for (int i = 1; i <= 12; i++)
		{
			ask(tag, i);
		}
		assertEquals(10, started.size());
		assertTrue(refused.isEmpty());
		queue.finished();
		assertEquals(11, started.size());
		assertEquals(Integer.valueOf(11), started.get(10));
		queue.finished();
		assertEquals(12, started.size());
		assertEquals(Integer.valueOf(12), started.get(11));
	}

	@Test
	public void cancelDropsWaitingCallsAndAHundredWaitAtMost()
	{
		final Object other = new Object();
		for (int i = 0; i < 10; i++)
		{
			ask(other, i);
		}
		assertEquals(10, started.size());
		final Object tag = new Object();
		for (int i = 100; i <= 200; i++)
		{
			ask(tag, i);
		}
		assertEquals(Integer.valueOf(200), refused.get(0));
		assertEquals(1, refused.size());
		assertEquals(10, started.size());
		queue.cancel(tag);
		queue.finished();
		assertEquals(10, started.size());
		assertEquals(1, refused.size());
	}

	@Test
	public void theMinuteCapWaitsForTheOldestStart()
	{
		final Object tag = new Object();
		for (int i = 0; i < 60; i++)
		{
			ask(tag, i);
			queue.finished();
		}
		assertEquals(60, started.size());
		now[0] = 30 * SECOND;
		ask(tag, 61);
		assertEquals(60, started.size());
		assertEquals(60 * SECOND, queue.nextDrainNanos(now[0]));
		now[0] = 60 * SECOND;
		queue.drain();
		assertEquals(61, started.size());
		assertEquals(-1L, queue.nextDrainNanos(now[0]));
	}
}
