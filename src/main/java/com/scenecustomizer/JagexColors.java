package com.scenecustomizer;

/** The game's 16-bit colours turned into screen colours. Pure. */
final class JagexColors
{
	/** The client's palette raises each channel to this power. */
	static final double BRIGHTNESS = 0.7;

	private JagexColors()
	{
	}

	/**
	 * The 0xRRGGBB of a game colour: hue bits 10-15, saturation bits 7-9, lightness bits 0-6.
	 *
	 * <p>Choice: saturation 0 gives an exact grey (the client's own +1/16 would tint it), so greys stay grey.
	 */
	static int hslToRgb(int hsl)
	{
		final double lightness = (hsl & 0x7F) / 128.0;
		final int saturationBits = (hsl >> 7) & 7;
		double r = lightness;
		double g = lightness;
		double b = lightness;
		if (saturationBits != 0)
		{
			final double hue = ((hsl >> 10) & 0x3F) / 64.0 + 1.0 / 128.0;
			final double saturation = saturationBits / 8.0 + 1.0 / 16.0;
			final double q = lightness < 0.5 ? lightness * (1.0 + saturation) : lightness + saturation - lightness * saturation;
			final double p = 2.0 * lightness - q;
			r = channel(p, q, hue + 1.0 / 3.0);
			g = channel(p, q, hue);
			b = channel(p, q, hue - 1.0 / 3.0);
		}
		return (toByte(r) << 16) | (toByte(g) << 8) | toByte(b);
	}

	private static double channel(double p, double q, double t)
	{
		double h = t;
		if (h < 0)
		{
			h += 1.0;
		}
		if (h > 1)
		{
			h -= 1.0;
		}
		if (6.0 * h < 1.0)
		{
			return p + (q - p) * 6.0 * h;
		}
		if (2.0 * h < 1.0)
		{
			return q;
		}
		if (3.0 * h < 2.0)
		{
			return p + (q - p) * (2.0 / 3.0 - h) * 6.0;
		}
		return p;
	}

	private static int toByte(double value)
	{
		final double lifted = Math.pow(Math.max(0.0, Math.min(1.0, value)), BRIGHTNESS);
		return Math.max(0, Math.min(255, (int) (lifted * 256.0)));
	}
}
