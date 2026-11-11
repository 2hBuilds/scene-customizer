package com.scenecustomizer;

import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;

/** {@link ScanArea}: the scan's range stays inside the scene. */
public class SceneryScanTest
{
	@Test
	public void theRangeIsClippedToTheScene()
	{
		assertArrayEquals(new int[] {35, 65}, ScanArea.range(50, 15, 104));
		assertArrayEquals(new int[] {0, 20}, ScanArea.range(5, 15, 104));
		assertArrayEquals(new int[] {85, 103}, ScanArea.range(100, 15, 104));
	}
}
