package com.scenecustomizer;

import java.awt.image.BufferedImage;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/** {@link PictureMemory}: the newest 1,000 pictures. */
public class PictureMemoryTest
{
	private static BufferedImage image()
	{
		return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
	}

	/** T22 */
	@Test
	public void keepsTheNewestThousand()
	{
		final PictureMemory memory = new PictureMemory();
		final BufferedImage last = image();
		for (int i = 0; i < 1000; i++)
		{
			memory.put("npc:" + i, image());
		}
		memory.put("npc:1000", last);
		assertNull(memory.get("npc:0"));
		assertSame(last, memory.get("npc:1000"));

		final BufferedImage replacement = image();
		memory.put("npc:1000", replacement);
		assertSame(replacement, memory.get("npc:1000"));
		assertNotNull(memory.get("npc:1"));
	}
}
