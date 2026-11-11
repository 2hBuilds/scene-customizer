package com.scenecustomizer;

import net.runelite.api.TileObject;

/** An object to outline and the outline to draw it with. Only {@link #seeThrough} is filled in later, on the client thread. */
final class OutlinedObject
{
	/** The object, in the scene. */
	final TileObject object;
	/** How to draw it. */
	final Outline outline;
	/** Whether every visible face of the object's model is see-through; null until the overlay first draws it. */
	Boolean seeThrough;

	OutlinedObject(TileObject object, Outline outline)
	{
		this.object = object;
		this.outline = outline;
	}
}
