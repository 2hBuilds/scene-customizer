package com.scenecustomizer;

import java.util.ArrayDeque;
import java.util.Deque;

/** The wiki call limits: at most 10 calls in flight and 60 started in any minute. Pure; callers guard it. */
final class CallBudget
{
	private static final int MOST_IN_FLIGHT = 10;
	private static final int MOST_PER_MINUTE = 60;
	private static final long MINUTE_NANOS = 60_000_000_000L;

	private final Deque<Long> starts = new ArrayDeque<>();
	private int inFlight;

	/** True, and the call counted, when it may start now; false when the budget is used up. */
	boolean tryStart(long nowNanos)
	{
		while (!starts.isEmpty() && nowNanos - starts.peekFirst() >= MINUTE_NANOS)
		{
			starts.removeFirst();
		}
		if (inFlight >= MOST_IN_FLIGHT || starts.size() >= MOST_PER_MINUTE)
		{
			return false;
		}
		inFlight++;
		starts.addLast(nowNanos);
		return true;
	}

	/**
	 * When only the 60-a-minute cap holds calls back (fewer than 10 in flight), the moment the oldest start leaves the
	 * minute; else -1.
	 */
	long minuteCapEndsNanos(long nowNanos)
	{
		while (!starts.isEmpty() && nowNanos - starts.peekFirst() >= MINUTE_NANOS)
		{
			starts.removeFirst();
		}
		if (inFlight >= MOST_IN_FLIGHT || starts.size() < MOST_PER_MINUTE)
		{
			return -1L;
		}
		return starts.peekFirst() + MINUTE_NANOS;
	}

	/** A started call has ended. */
	void finished()
	{
		if (inFlight > 0)
		{
			inFlight--;
		}
	}
}
