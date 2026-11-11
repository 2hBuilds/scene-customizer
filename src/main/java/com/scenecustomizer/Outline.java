package com.scenecustomizer;

/** An outline's colour and width. */
final class Outline
{
	/** The colour, 0xRRGGBB. */
	final int rgb;
	/** The width, 1 to 6. */
	final int width;

	/** Choice: a width outside 1-6 is clamped, not refused. */
	Outline(int rgb, int width)
	{
		this.rgb = rgb;
		this.width = Math.max(1, Math.min(6, width));
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof Outline))
		{
			return false;
		}
		final Outline other = (Outline) o;
		return rgb == other.rgb && width == other.width;
	}

	@Override
	public int hashCode()
	{
		return 31 * rgb + width;
	}
}
