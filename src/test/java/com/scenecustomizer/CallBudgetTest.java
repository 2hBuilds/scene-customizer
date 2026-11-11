package com.scenecustomizer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** {@link CallBudget}: 10 in flight and 60 started in a minute. */
public class CallBudgetTest
{
	private static final long SECOND = 1_000_000_000L;

	@Test
	public void tenInFlightAndSixtyAMinute()
	{
		final CallBudget flight = new CallBudget();
		for (int i = 0; i < 10; i++)
		{
			assertTrue(flight.tryStart(0L));
		}
		assertFalse(flight.tryStart(0L));
		flight.finished();
		assertTrue(flight.tryStart(0L));

		final CallBudget minute = new CallBudget();
		for (int i = 0; i < 60; i++)
		{
			assertTrue("start " + i, minute.tryStart(i * SECOND / 2));
			minute.finished();
		}
		assertFalse(minute.tryStart(30 * SECOND));
		assertTrue(minute.tryStart(61 * SECOND));
	}
}
