package com.scenecustomizer;

import net.runelite.api.Constants;

/** The scene's 8 x 8 tile zones, as the GPU keeps them: which zone holds an object, and a key for one. */
final class Zones
{
	/** How many zones the extended scene adds on each side of the scene (Object Hider's offset). */
	static final int OFFSET = (Constants.EXTENDED_SCENE_SIZE - Constants.SCENE_SIZE) / 2 >> 3;

	private Zones()
	{
	}

	/** The zone column of a tile object's hash (its scene x is in bits 0 to 6). */
	static int zoneX(long hash)
	{
		return ((int) (hash & 127) >> 3) + OFFSET;
	}

	/** The zone row of a tile object's hash (its scene y is in bits 7 to 13). */
	static int zoneZ(long hash)
	{
		return ((int) ((hash >> 7) & 127) >> 3) + OFFSET;
	}

	/** One number for a zone. */
	static long key(int zx, int zz)
	{
		return ((long) zx << 32) | (zz & 0xFFFFFFFFL);
	}
}
