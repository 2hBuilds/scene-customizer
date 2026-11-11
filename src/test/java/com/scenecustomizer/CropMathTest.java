package com.scenecustomizer;

import java.awt.Dimension;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/** {@link CropMath}: the picture size. */
public class CropMathTest
{
	@Test
	public void fitsIn36By32KeepingTheShape()
	{
		assertEquals(new Dimension(24, 32), CropMath.fit(28, 38));
		assertEquals(new Dimension(36, 16), CropMath.fit(72, 32));
		assertEquals(new Dimension(10, 10), CropMath.fit(10, 10));
		assertEquals(new Dimension(32, 32), CropMath.fit(100, 100));
	}
}
