package com.scenecustomizer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** {@link Names}: tags stripped, empty and "null" turned into no name. */
public class NamesTest
{
	@Test
	public void cleanStripsTagsAndTrims()
	{
		assertEquals("Vorkath", Names.clean("<col=ffff00>Vorkath</col>"));
		assertEquals("Zombified Spawn", Names.clean("  Zombified Spawn "));
	}

	@Test
	public void cleanTurnsNullAndEmptyIntoNameless()
	{
		assertNull(Names.clean(null));
		assertNull(Names.clean(""));
		assertNull(Names.clean("null"));
		assertNull(Names.clean("<col=ff0000></col>"));
	}
}
