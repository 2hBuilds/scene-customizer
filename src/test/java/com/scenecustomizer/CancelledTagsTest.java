package com.scenecustomizer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** {@link CancelledTags}: the last 64 cancelled tags are remembered. */
public class CancelledTagsTest
{
	@Test
	public void remembersTheLast64()
	{
		final CancelledTags tags = new CancelledTags();
		final List<Object> made = new ArrayList<>();
		for (int i = 0; i < 65; i++)
		{
			final Object tag = new Object();
			made.add(tag);
			tags.remember(tag);
		}
		assertFalse(tags.contains(made.get(0)));
		assertTrue(tags.contains(made.get(1)));
		assertTrue(tags.contains(made.get(64)));
		assertFalse(tags.contains(new Object()));
	}
}
