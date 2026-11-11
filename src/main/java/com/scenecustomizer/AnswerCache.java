package com.scenecustomizer;

import java.util.LinkedHashMap;
import java.util.Map;

/** Wiki answers by address: 200 kept, the oldest out; an answer over 30 minutes old is a miss. Pure; callers guard it. */
final class AnswerCache
{
	private static final int MOST = 200;
	private static final long LIFETIME_NANOS = 30L * 60L * 1_000_000_000L;

	/** One kept answer and when it came. */
	private static final class Entry
	{
		private final String body;
		private final long atNanos;

		Entry(String body, long atNanos)
		{
			this.body = body;
			this.atNanos = atNanos;
		}
	}

	private final Map<String, Entry> entries = new LinkedHashMap<>();

	/** The kept answer, or null when there is none or it is over 30 minutes old. */
	String get(String url, long nowNanos)
	{
		final Entry entry = entries.get(url);
		if (entry == null)
		{
			return null;
		}
		if (nowNanos - entry.atNanos > LIFETIME_NANOS)
		{
			entries.remove(url);
			return null;
		}
		return entry.body;
	}

	/** Keeps an answer as the newest; beyond 200 the oldest goes. */
	void put(String url, String body, long nowNanos)
	{
		entries.remove(url);
		entries.put(url, new Entry(body, nowNanos));
		while (entries.size() > MOST)
		{
			entries.remove(entries.keySet().iterator().next());
		}
	}
}
