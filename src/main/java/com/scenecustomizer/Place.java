package com.scenecustomizer;

/** A world point: x, y and plane. */
final class Place
{
	/** World x. */
	final int x;
	/** World y. */
	final int y;
	/** The plane. */
	final int plane;

	Place(int x, int y, int plane)
	{
		this.x = x;
		this.y = y;
		this.plane = plane;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof Place))
		{
			return false;
		}
		final Place p = (Place) o;
		return x == p.x && y == p.y && plane == p.plane;
	}

	@Override
	public int hashCode()
	{
		return 31 * (31 * x + y) + plane;
	}

	@Override
	public String toString()
	{
		return x + "," + y + "," + plane;
	}
}
