package com.scenecustomizer;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** {@link ModelFaces}: whether a model is made only of see-through faces. */
public class ModelFacesTest
{
	@Test
	public void allSeeThrough()
	{
		assertTrue(ModelFaces.allSeeThrough(new byte[] {(byte) 128, 64}, new int[] {-1, -1}));
		assertFalse(ModelFaces.allSeeThrough(new byte[] {(byte) 128, 0}, new int[] {-1, -1}));
		assertTrue(ModelFaces.allSeeThrough(new byte[] {(byte) 128, 0}, new int[] {-1, -2}));
		assertFalse(ModelFaces.allSeeThrough(null, new int[] {-1, -1}));
		assertFalse(ModelFaces.allSeeThrough(new byte[] {(byte) 128, 64}, new int[] {-2, -2}));
	}
}
