package com.scenecustomizer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * Lets wiki calls wait for the budget instead of failing. A call starts at once when {@link CallBudget} allows; else it
 * waits first in, first out, at most 100 at a time (the 101st is refused). A finished call starts waiting ones while
 * the budget allows, and a cancelled tag loses its waiting calls. The start and refuse actions run outside the queue's
 * own lock. Pure: the clock is passed in.
 */
final class CallQueue
{
	private static final int MOST_WAITING = 100;

	/** One call that waits. */
	private static final class Waiting
	{
		final Object tag;
		final Runnable start;
		final Runnable refuse;

		Waiting(Object tag, Runnable start, Runnable refuse)
		{
			this.tag = tag;
			this.start = start;
			this.refuse = refuse;
		}
	}

	private final LongSupplier clock;
	private final Object lock = new Object();
	private final CallBudget budget = new CallBudget();
	private final Deque<Waiting> waiting = new ArrayDeque<>();

	/** @param clock the time in nanoseconds, on any base */
	CallQueue(LongSupplier clock)
	{
		this.clock = clock;
	}

	/**
	 * Starts the call at once when it may, else queues it, else (100 already wait) refuses it. A started call is
	 * counted in the budget until {@link #finished}.
	 */
	void ask(Object tag, Runnable start, Runnable refuse)
	{
		final Runnable run;
		synchronized (lock)
		{
			if (waiting.isEmpty() && budget.tryStart(clock.getAsLong()))
			{
				run = start;
			}
			else if (waiting.size() >= MOST_WAITING)
			{
				run = refuse;
			}
			else
			{
				waiting.addLast(new Waiting(tag, start, refuse));
				run = null;
			}
		}
		if (run != null)
		{
			run.run();
		}
	}

	/** A started call has ended: its place goes back and waiting calls start while the budget allows. */
	void finished()
	{
		synchronized (lock)
		{
			budget.finished();
		}
		drain();
	}

	/** Starts waiting calls, oldest first, while the budget allows. */
	void drain()
	{
		final List<Runnable> toStart = new ArrayList<>();
		synchronized (lock)
		{
			while (!waiting.isEmpty() && budget.tryStart(clock.getAsLong()))
			{
				toStart.add(waiting.removeFirst().start);
			}
		}
		for (Runnable start : toStart)
		{
			start.run();
		}
	}

	/** Drops the tag's waiting calls; their actions never run. */
	void cancel(Object tag)
	{
		synchronized (lock)
		{
			final Iterator<Waiting> all = waiting.iterator();
			while (all.hasNext())
			{
				if (all.next().tag.equals(tag))
				{
					all.remove();
				}
			}
		}
	}

	/**
	 * When calls wait only for the 60-a-minute cap, the moment (on the clock's base) the oldest start leaves the
	 * minute; else -1.
	 */
	long nextDrainNanos(long nowNanos)
	{
		synchronized (lock)
		{
			if (waiting.isEmpty())
			{
				return -1L;
			}
			return budget.minuteCapEndsNanos(nowNanos);
		}
	}
}
