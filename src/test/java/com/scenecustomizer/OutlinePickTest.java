package com.scenecustomizer;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/** {@link OutlinePick}: the nearest few, nearest first. */
public class OutlinePickTest
{
	@Test
	public void nearestFirstUpToTheMost()
	{
		final List<Integer> far = new ArrayList<>();
		for (int d = 45; d >= 1; d--)
		{
			far.add(d);
		}
		final List<Integer> picked = OutlinePick.nearest(far, d -> d, 40);
		assertEquals(40, picked.size());
		assertEquals(Integer.valueOf(1), picked.get(0));
		assertEquals(Integer.valueOf(40), picked.get(39));
		final List<Integer> few = new ArrayList<>();
		few.add(9);
		few.add(2);
		few.add(5);
		assertEquals(3, OutlinePick.nearest(few, d -> d, 40).size());
	}
}
