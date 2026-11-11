package com.scenecustomizer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/** {@link Zones}: which 8 x 8 zone holds an object. */
public class ZonesTest
{
	@Test
	public void theZoneOfAHash()
	{
		assertEquals(5, Zones.OFFSET);
		final long hash = 17 | 42L << 7;
		assertEquals(7, Zones.zoneX(hash));
		assertEquals(10, Zones.zoneZ(hash));
		assertNotEquals(Zones.key(7, 10), Zones.key(10, 7));
	}
}
