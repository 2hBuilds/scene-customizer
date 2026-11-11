package com.scenecustomizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** {@link AnswerCache}: 200 answers, 30 minutes. */
public class AnswerCacheTest
{
	private static final long MINUTE = 60_000_000_000L;

	@Test
	public void oldestOutAndThirtyMinutes()
	{
		final AnswerCache cache = new AnswerCache();
		cache.put("a", "body", 0L);
		assertEquals("body", cache.get("a", 29 * MINUTE));
		assertNull(cache.get("a", 31 * MINUTE));

		final AnswerCache full = new AnswerCache();
		for (int i = 0; i < 201; i++)
		{
			full.put("u" + i, "b" + i, 0L);
		}
		assertNull(full.get("u0", 0L));
		assertEquals("b1", full.get("u1", 0L));
		assertEquals("b200", full.get("u200", 0L));
	}
}
