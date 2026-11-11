package com.scenecustomizer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/** {@link TextureUv}: a point's place on a texture triangle. */
public class TextureUvTest
{
	private static void check(float u, float v, float[] got)
	{
		assertEquals(u, got[0], 0.0001f);
		assertEquals(v, got[1], 0.0001f);
	}

	@Test
	public void cornersAndMidpoint()
	{
		final float[] p = {0, 0, 0};
		final float[] m = {10, 0, 0};
		final float[] n = {0, 10, 0};
		check(0f, 0f, TextureUv.uv(p, m, n, p));
		check(1f, 0f, TextureUv.uv(p, m, n, m));
		check(0f, 1f, TextureUv.uv(p, m, n, n));
		check(0.5f, 0f, TextureUv.uv(p, m, n, new float[] {5, 0, 0}));
		check(0f, 0f, TextureUv.uv(p, p, p, new float[] {3, 4, 5}));
	}
}
