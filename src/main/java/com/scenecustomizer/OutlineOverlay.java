package com.scenecustomizer;

import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Model;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

/**
 * Draws the outlines of the outlined NPCs and objects, RuneLite's own outline with a feather of 0. At most 40 are
 * drawn a frame, the nearest the local player first (all of them when there is no local player); a blocked object
 * id is never drawn. It runs on the client thread and reads the {@link Decisions} only.
 */
final class OutlineOverlay extends Overlay
{
	/** The most outlines drawn in one frame. */
	static final int MOST = 40;

	private final Client client;
	private final ModelOutlineRenderer renderer;
	private final Decisions decisions;

	OutlineOverlay(Client client, ModelOutlineRenderer renderer, Decisions decisions)
	{
		this.client = client;
		this.renderer = renderer;
		this.decisions = decisions;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	/** One outline to draw: an NPC or an object, how far it is and how to draw it. */
	private static final class Pending
	{
		final NPC npc;
		final TileObject object;
		final OutlinedObject found;
		final Outline outline;
		final int distance;

		Pending(NPC npc, OutlinedObject found, Outline outline, int distance)
		{
			this.npc = npc;
			this.found = found;
			this.object = found == null ? null : found.object;
			this.outline = outline;
			this.distance = distance;
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final Map<NPC, Outline> npcs = decisions.outlinedNpcs();
		final List<OutlinedObject> objects = decisions.outlinedObjects();
		if (npcs.isEmpty() && objects.isEmpty())
		{
			return null;
		}
		final Player player = client.getLocalPlayer();
		final WorldPoint from = player == null ? null : player.getWorldLocation();
		final List<Pending> all = new ArrayList<>();
		for (Map.Entry<NPC, Outline> entry : npcs.entrySet())
		{
			all.add(new Pending(entry.getKey(), null, entry.getValue(), distance(from, entry.getKey().getWorldLocation())));
		}
		for (OutlinedObject found : objects)
		{
			if (!BlockList.isBlocked(found.object.getId()))
			{
				all.add(new Pending(null, found, found.outline, distance(from, found.object.getWorldLocation())));
			}
		}
		final List<Pending> shown = from == null ? all : OutlinePick.nearest(all, p -> p.distance, MOST);
		for (Pending pending : shown)
		{
			final Color colour = new Color(pending.outline.rgb);
			if (pending.npc != null)
			{
				renderer.drawOutline(pending.npc, pending.outline.width, colour, 0);
			}
			else if (seeThrough(pending.found))
			{
				final Shape shape = pending.object instanceof GameObject
					? ((GameObject) pending.object).getConvexHull() : null;
				final Shape drawn = shape != null ? shape : pending.object.getClickbox();
				if (drawn != null)
				{
					graphics.setColor(colour);
					graphics.setStroke(new BasicStroke(pending.outline.width));
					graphics.draw(drawn);
				}
			}
			else
			{
				renderer.drawOutline(pending.object, pending.outline.width, colour, 0);
			}
		}
		return null;
	}

	/** Whether the object's model is all see-through; worked out once, the first time it is drawn. */
	private static boolean seeThrough(OutlinedObject found)
	{
		if (found.seeThrough == null)
		{
			final Model model = ModelPictures.objectModel(found.object);
			boolean result = false;
			if (model != null)
			{
				final byte[] transparencies = model.getFaceTransparencies();
				final int[] colours3 = model.getFaceColors3();
				if (transparencies != null && colours3 != null)
				{
					final int count = Math.min(model.getFaceCount(), Math.min(transparencies.length, colours3.length));
					result = ModelFaces.allSeeThrough(Arrays.copyOf(transparencies, count), Arrays.copyOf(colours3, count));
				}
			}
			found.seeThrough = result;
		}
		return found.seeThrough;
	}

	/** How far apart two points are; the largest number when either is unknown or they are on different planes. */
	private static int distance(WorldPoint from, WorldPoint to)
	{
		return from == null || to == null ? Integer.MAX_VALUE : from.distanceTo(to);
	}
}
