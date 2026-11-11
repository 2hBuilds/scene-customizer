package com.scenecustomizer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** {@link JagexColors}: the game's colours as screen colours. */
public class JagexColorsTest
{
	private static int hsl(int hue, int saturation, int lightness)
	{
		return (hue << 10) | (saturation << 7) | lightness;
	}

	private static int r(int rgb)
	{
		return (rgb >> 16) & 0xFF;
	}

	private static int g(int rgb)
	{
		return (rgb >> 8) & 0xFF;
	}

	private static int b(int rgb)
	{
		return rgb & 0xFF;
	}

	@Test
	public void greysHaveEqualChannels()
	{
		int previous = -1;
		for (int lightness : new int[] {10, 64, 120})
		{
			final int rgb = JagexColors.hslToRgb(hsl(20, 0, lightness));
			assertEquals(r(rgb), g(rgb));
			assertEquals(g(rgb), b(rgb));
			assertTrue(r(rgb) > previous);
			previous = r(rgb);
		}
	}

	@Test
	public void huesLandWhereTheyShould()
	{
		final int red = JagexColors.hslToRgb(hsl(0, 7, 64));
		assertTrue(r(red) > g(red) && r(red) > b(red));
		final int green = JagexColors.hslToRgb(hsl(21, 7, 64));
		assertTrue(g(green) > r(green) && g(green) > b(green));
		final int blue = JagexColors.hslToRgb(hsl(43, 7, 64));
		assertTrue(b(blue) > r(blue) && b(blue) > g(blue));
	}
}
