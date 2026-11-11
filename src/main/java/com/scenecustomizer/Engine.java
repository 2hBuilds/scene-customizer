package com.scenecustomizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.GroundObject;
import net.runelite.api.NPC;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.hooks.DrawCallbacks;

/**
 * The hide and outline engine: turns the rules into {@link Decisions}. Every method runs on the client thread; the
 * decisions are built fresh and published whole, so the render thread only ever reads finished sets.
 *
 * <p>Choice: the zones of the old scene are dropped at LOADING together with the hashes (a zone key means nothing in
 * the next scene), and the first tick after a load walks the new scene with a zone refresh. Choice: a despawned
 * object only loses its outline; a hash stays hidden until the next walk, which can only be the same object's.
 * Choice: after {@link #shutDown} a late {@link #rulesChanged} does nothing.
 */
final class Engine
{
	private final Client client;
	private final Rules rules;
	private final Decisions decisions = new Decisions();
	private final ObjectNames objectNames;
	private final SceneLoadGate gate = new SceneLoadGate();
	private RuleLookup lookup = new RuleLookup(Collections.<Rule>emptyList());
	private boolean stopped;

	Engine(Client client, Rules rules)
	{
		this.client = client;
		this.rules = rules;
		this.objectNames = new ObjectNames(client);
	}

	/** The decisions, for the callback and the overlay to read. */
	Decisions decisions()
	{
		return decisions;
	}

	/** The rules changed (or the plugin started): read them again and rebuild everything, refreshing the zones. */
	void rulesChanged()
	{
		if (stopped)
		{
			return;
		}
		lookup = new RuleLookup(rules.all());
		rebuildNpcs();
		rebuildObjects(true);
	}

	/** A game tick: the first one after a load walks the scene; NPC rules are looked at again every tick. */
	void onGameTick()
	{
		final boolean wasClosed = !gate.spawnsCount();
		gate.onTick();
		if (wasClosed && gate.spawnsCount())
		{
			rebuildObjects(true);
		}
		if (lookup.hasNpcRules())
		{
			rebuildNpcs();
		}
	}

	/** A game state: LOADING drops the old scene's hashes, outlines and zones and the cached names. */
	void onGameStateChanged(GameState state)
	{
		gate.onGameState(state);
		if (state == GameState.LOADING)
		{
			objectNames.clear();
			decisions.setObjects(decisions.hiddenObjectIds(), Collections.<Long>emptySet(),
				Collections.<OutlinedObject>emptyList(), Collections.<Long>emptySet());
		}
	}

	/** An NPC appeared: with NPC rules, look again at once so a hidden NPC never shows for a frame. */
	void onNpcSpawned()
	{
		if (lookup.hasNpcRules())
		{
			rebuildNpcs();
		}
	}

	/** An object appeared after the load: add what the rules say about it. */
	void onObjectSpawned(TileObject object)
	{
		if (object == null || !lookup.hasObjectRules() || !gate.spawnsCount())
		{
			return;
		}
		if (object instanceof GameObject && !ObjectTags.isSceneObject(object.getHash()))
		{
			return;
		}
		final ObjectDecision decision = decide(object);
		if (decision.isEmpty())
		{
			return;
		}
		final boolean hiddenBefore = decisions.objectHidden(object.getId(), object.getHash());
		final Set<Integer> ids = new HashSet<>(decisions.hiddenObjectIds());
		final Set<Long> hashes = new HashSet<>(decisions.hiddenObjectHashes());
		final List<OutlinedObject> outlined = new ArrayList<>(decisions.outlinedObjects());
		final Set<Long> zones = new HashSet<>(decisions.hiddenZones());
		if (decision.hideId)
		{
			ids.add(object.getId());
		}
		if (decision.hideHash)
		{
			hashes.add(object.getHash());
		}
		if (decision.outline != null)
		{
			outlined.add(new OutlinedObject(object, decision.outline));
		}
		final boolean hiddenNow = !BlockList.isBlocked(object.getId())
			&& (ids.contains(object.getId()) || hashes.contains(object.getHash()));
		final long zone = zoneOf(object);
		if (hiddenNow)
		{
			zones.add(zone);
		}
		decisions.setObjects(ids, hashes, outlined, zones);
		if (hiddenNow != hiddenBefore)
		{
			refreshZones(Collections.singleton(zone));
		}
	}

	/** An object went: its outline is dropped. */
	void onObjectDespawned(TileObject object)
	{
		if (object == null)
		{
			return;
		}
		final List<OutlinedObject> before = decisions.outlinedObjects();
		final List<OutlinedObject> after = new ArrayList<>();
		for (OutlinedObject found : before)
		{
			if (found.object != object)
			{
				after.add(found);
			}
		}
		if (after.size() != before.size())
		{
			decisions.setObjects(decisions.hiddenObjectIds(), decisions.hiddenObjectHashes(), after,
				decisions.hiddenZones());
		}
	}

	/** The plugin stops: every zone that held a hidden object is refreshed, then the decisions are emptied. */
	void shutDown()
	{
		stopped = true;
		refreshZones(decisions.hiddenZones());
		decisions.clear();
	}

	private void rebuildNpcs()
	{
		final Set<NPC> hidden = Collections.newSetFromMap(new IdentityHashMap<NPC, Boolean>());
		final Map<NPC, Outline> outlined = new IdentityHashMap<>();
		final WorldView top = client.getTopLevelWorldView();
		if (top != null)
		{
			collectNpcs(top, hidden, outlined);
		}
		decisions.setNpcs(hidden, outlined);
	}

	private void collectNpcs(WorldView view, Set<NPC> hidden, Map<NPC, Outline> outlined)
	{
		if (view.npcs() != null)
		{
			NpcDecider.decide(view.npcs(), lookup, hidden, outlined);
		}
		if (view.worldViews() != null)
		{
			for (WorldView sub : view.worldViews())
			{
				collectNpcs(sub, hidden, outlined);
			}
		}
	}

	/**
	 * One walk of the top-level scene's tiles: the answers for every object the rules name. Choice: with a refresh,
	 * the zones that held a hidden object before are refreshed too, since an object the rules no longer name is not
	 * seen by the walk and must still come back. A multi-tile object is visited once.
	 */
	private void rebuildObjects(boolean refresh)
	{
		objectNames.refreshImpostors();
		final Set<Long> oldZones = decisions.hiddenZones();
		final WorldView top = client.getTopLevelWorldView();
		final Scene scene = top == null ? null : top.getScene();
		if (!lookup.hasObjectRules() || scene == null || scene.getTiles() == null)
		{
			decisions.setObjects(Collections.<Integer>emptySet(), Collections.<Long>emptySet(),
				Collections.<OutlinedObject>emptyList(), Collections.<Long>emptySet());
			if (refresh)
			{
				refreshZones(oldZones);
			}
			return;
		}
		final Set<Integer> ids = new HashSet<>();
		final Set<Long> hashes = new HashSet<>();
		final List<OutlinedObject> outlined = new ArrayList<>();
		final List<TileObject> named = new ArrayList<>();
		final Set<Long> visited = new HashSet<>();
		for (Tile[][] plane : scene.getTiles())
		{
			if (plane == null)
			{
				continue;
			}
			for (Tile[] column : plane)
			{
				if (column == null)
				{
					continue;
				}
				for (Tile tile : column)
				{
					if (tile != null)
					{
						walkTile(tile, ids, hashes, outlined, named, visited);
					}
				}
			}
		}
		final Set<Long> zones = new HashSet<>();
		final Set<Long> changed = new HashSet<>();
		for (TileObject object : named)
		{
			final boolean hiddenNow = !BlockList.isBlocked(object.getId())
				&& (ids.contains(object.getId()) || hashes.contains(object.getHash()));
			final long zone = zoneOf(object);
			if (hiddenNow)
			{
				zones.add(zone);
			}
			if (hiddenNow != decisions.objectHidden(object.getId(), object.getHash()))
			{
				changed.add(zone);
			}
		}
		decisions.setObjects(ids, hashes, outlined, zones);
		if (refresh)
		{
			changed.addAll(oldZones);
			refreshZones(changed);
		}
	}

	private void walkTile(Tile tile, Set<Integer> ids, Set<Long> hashes, List<OutlinedObject> outlined,
		List<TileObject> named, Set<Long> visited)
	{
		final GameObject[] games = tile.getGameObjects();
		if (games != null)
		{
			for (GameObject game : games)
			{
				if (game != null && ObjectTags.isSceneObject(game.getHash()) && visited.add(game.getHash()))
				{
					visit(game, ids, hashes, outlined, named);
				}
			}
		}
		final WallObject wall = tile.getWallObject();
		if (wall != null)
		{
			visit(wall, ids, hashes, outlined, named);
		}
		final DecorativeObject decor = tile.getDecorativeObject();
		if (decor != null)
		{
			visit(decor, ids, hashes, outlined, named);
		}
		final GroundObject ground = tile.getGroundObject();
		if (ground != null)
		{
			visit(ground, ids, hashes, outlined, named);
		}
	}

	private void visit(TileObject object, Set<Integer> ids, Set<Long> hashes, List<OutlinedObject> outlined,
		List<TileObject> named)
	{
		final ObjectDecision decision = decide(object);
		if (decision.isEmpty())
		{
			return;
		}
		if (decision.hideId)
		{
			ids.add(object.getId());
		}
		if (decision.hideHash)
		{
			hashes.add(object.getHash());
		}
		if (decision.outline != null)
		{
			outlined.add(new OutlinedObject(object, decision.outline));
		}
		named.add(object);
	}

	/** What the rules say about one object; nothing for a name no object rule has. */
	private ObjectDecision decide(TileObject object)
	{
		final String name = objectNames.nameOf(object.getId());
		if (name == null || !lookup.objectNames().contains(name))
		{
			return ObjectRules.decide(object.getId(), object.getHash(), null, null, lookup);
		}
		final WorldPoint where = object.getWorldLocation();
		final Place place = where == null ? null : new Place(where.getX(), where.getY(), where.getPlane());
		return ObjectRules.decide(object.getId(), object.getHash(), name, place, lookup);
	}

	private static long zoneOf(TileObject object)
	{
		return Zones.key(Zones.zoneX(object.getHash()), Zones.zoneZ(object.getHash()));
	}

	/** Asks the GPU to build each zone again; with no GPU plugin there is nothing to do. */
	private void refreshZones(Set<Long> keys)
	{
		if (keys.isEmpty())
		{
			return;
		}
		final DrawCallbacks callbacks = client.getDrawCallbacks();
		final WorldView top = client.getTopLevelWorldView();
		final Scene scene = top == null ? null : top.getScene();
		if (callbacks == null || scene == null)
		{
			return;
		}
		for (long key : keys)
		{
			callbacks.invalidateZone(scene, (int) (key >> 32), (int) key);
		}
	}
}
