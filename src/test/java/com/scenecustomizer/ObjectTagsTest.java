package com.scenecustomizer;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** {@link ObjectTags}: only type 2 is a scene object. */
public class ObjectTagsTest
{
	@Test
	public void onlyTypeTwoIsASceneObject()
	{
		assertTrue(ObjectTags.isSceneObject(2L << 16));
		assertTrue(ObjectTags.isSceneObject((2L << 16) | 12345L));
		assertTrue(ObjectTags.isSceneObject((5L << 20) | (2L << 16)));
		assertFalse(ObjectTags.isSceneObject(1L << 16));
		assertFalse(ObjectTags.isSceneObject(3L << 16));
		assertFalse(ObjectTags.isSceneObject(0L));
	}
}
