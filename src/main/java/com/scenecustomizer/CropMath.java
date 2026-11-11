package com.scenecustomizer;

import java.awt.Dimension;

/** The arithmetic of picture sizes. */
final class CropMath
{
	private static final int MAX_WIDTH = 36;
	private static final int MAX_HEIGHT = 32;

	private CropMath()
	{
	}

	/** The size scaled down, never up, to fit 36 x 32 keeping the ratio; each side rounded and at least 1. */
	static Dimension fit(int width, int height)
	{
		final double scale = Math.min(1.0, Math.min((double) MAX_WIDTH / width, (double) MAX_HEIGHT / height));
		return new Dimension(Math.max(1, (int) Math.round(width * scale)), Math.max(1, (int) Math.round(height * scale)));
	}
}
