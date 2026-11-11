package com.scenecustomizer;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** {@link ProjectileSet}: each projectile counts once, the oldest are forgotten. */
public class ProjectileSetTest
{
	@Test
	public void countsEachProjectileOnce()
	{
		final ProjectileSet set = new ProjectileSet();
		final Object a = new Object();
		final Object b = new Object();
		assertTrue(set.add(a));
		assertFalse(set.add(a));
		assertTrue(set.add(b));
	}

	@Test
	public void forgetsTheOldestPast1000()
	{
		final ProjectileSet set = new ProjectileSet();
		final Object[] all = new Object[1001];
		for (int i = 0; i < all.length; i++)
		{
			all[i] = new Object();
			set.add(all[i]);
		}
		assertTrue(set.add(all[0]));
		assertFalse(set.add(all[1000]));
	}
}
