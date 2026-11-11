package com.scenecustomizer;

import net.runelite.api.Renderable;
import net.runelite.api.Scene;
import net.runelite.api.TileObject;
import net.runelite.client.callback.RenderCallback;

/**
 * Tells the game what not to draw. Either method may run on a thread other than the client thread, so both read
 * only the {@link Decisions}' published sets and nothing else.
 */
final class HideCallback implements RenderCallback
{
	private final Decisions decisions;

	HideCallback(Decisions decisions)
	{
		this.decisions = decisions;
	}

	/** False for a hidden NPC, its model and its health bar and text alike; true for everything else. */
	@Override
	public boolean addEntity(Renderable renderable, boolean ui)
	{
		return !decisions.npcHidden(renderable);
	}

	/** False for a hidden scene object; the callback also sees other things, which are always drawn. */
	@Override
	public boolean drawObject(Scene scene, TileObject object)
	{
		return !ObjectTags.isSceneObject(object.getHash()) || !decisions.objectHidden(object.getId(), object.getHash());
	}
}
