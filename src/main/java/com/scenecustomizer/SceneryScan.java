package com.scenecustomizer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

/**
 * The nearby scan: every named scene object within 15 tiles of the player, on the player's plane, in the player's own
 * world view. Client thread only. Choice: a found object is kept as the key's live reference too, so it can be
 * pictured like a spawned one; the references are cleared at the stop.
 */
final class SceneryScan
{
	private static final int RADIUS = 15;

	private final Client client;
	private final ObjectNames names;

	SceneryScan(Client client, ObjectNames names)
	{
		this.client = client;
		this.names = names;
	}

	/** Offers what is nearby to the table; with no player or scene it does nothing. */
	void scan(ThingTable table, Map<ThingKey, Object> live, long atMs)
	{
		final Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}
		final WorldView view = player.getWorldView();
		final LocalPoint where = player.getLocalLocation();
		if (view == null || where == null)
		{
			return;
		}
		final Scene scene = view.getScene();
		final Tile[][][] planes = scene == null ? null : scene.getTiles();
		final int plane = view.getPlane();
		if (planes == null || plane < 0 || plane >= planes.length || planes[plane] == null)
		{
			return;
		}
		final Tile[][] tiles = planes[plane];
		final int[] xs = ScanArea.range(where.getSceneX(), RADIUS, tiles.length);
		final Set<Long> multiTile = new HashSet<>();
		for (int x = xs[0]; x <= xs[1]; x++)
		{
			if (tiles[x] == null)
			{
				continue;
			}
			final int[] ys = ScanArea.range(where.getSceneY(), RADIUS, tiles[x].length);
			for (int y = ys[0]; y <= ys[1]; y++)
			{
				final Tile tile = tiles[x][y];
				if (tile != null)
				{
					scanTile(tile, table, live, atMs, multiTile);
				}
			}
		}
	}

	private void scanTile(Tile tile, ThingTable table, Map<ThingKey, Object> live, long atMs, Set<Long> multiTile)
	{
		final GameObject[] games = tile.getGameObjects();
		if (games != null)
		{
			for (GameObject g : games)
			{
				if (g != null && ObjectTags.isSceneObject(g.getHash()) && multiTile.add(g.getHash()))
				{
					offer(g, table, live, atMs);
				}
			}
		}
		final WallObject wall = tile.getWallObject();
		if (wall != null)
		{
			offer(wall, table, live, atMs);
		}
		final DecorativeObject decor = tile.getDecorativeObject();
		if (decor != null)
		{
			offer(decor, table, live, atMs);
		}
		final GroundObject ground = tile.getGroundObject();
		if (ground != null)
		{
			offer(ground, table, live, atMs);
		}
	}

	private void offer(TileObject object, ThingTable table, Map<ThingKey, Object> live, long atMs)
	{
		final String name = names.nameOf(object.getId());
		if (name == null)
		{
			return;
		}
		final WorldPoint at = object.getWorldLocation();
		final Place place = at == null ? null : new Place(at.getX(), at.getY(), at.getPlane());
		final ThingKey key = table.sceneryFound(name, object.getId(), place, atMs);
		if (key != null)
		{
			live.put(key, object);
		}
	}
}
