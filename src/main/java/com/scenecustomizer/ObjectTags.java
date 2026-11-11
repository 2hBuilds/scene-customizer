package com.scenecustomizer;

/** Tells scene objects from the other things that carry an object hash. */
final class ObjectTags
{
	private ObjectTags()
	{
	}

	/**
	 * True when the hash's type bits (16 to 18) say 2, a scene object: the check Object Hider uses. Choice: the
	 * recorder applies it to game objects only, since a wall, decoration or ground object has another type by design.
	 */
	static boolean isSceneObject(long hash)
	{
		return ((hash >>> 16) & 7) == 2;
	}
}
