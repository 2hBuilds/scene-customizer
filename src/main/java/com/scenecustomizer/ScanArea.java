package com.scenecustomizer;

/** The tiles the nearby scan looks at. */
final class ScanArea
{
	private ScanArea()
	{
	}

	/** The first and last index: {@code [max(0, c - r), min(length - 1, c + r)]}. */
	static int[] range(int centre, int radius, int length)
	{
		return new int[] {Math.max(0, centre - radius), Math.min(length - 1, centre + radius)};
	}
}
