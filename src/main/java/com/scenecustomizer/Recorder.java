package com.scenecustomizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.ActorSpotAnim;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.client.callback.ClientThread;

/**
 * Feeds the current recording from the game. Every method runs on the client thread and every event method returns
 * at once when nothing is recording. The two listeners are called on the client thread; the plugin hands them to the
 * Swing thread. Choice: {@code start} while already recording does nothing. Choice: the time of an event is read
 * from the clock when it arrives, so it is the tick's own time to within a tick.
 */
final class Recorder
{
	/** The scan runs every this many ticks. */
	private static final int SCAN_EVERY = 10;
	/** The pictures and the status run every this many ticks. */
	private static final int PICTURES_EVERY = 2;
	/** A stopped recording's pictures are still tried for this many ticks (2 minutes). */
	private static final int FINISH_TICKS = 200;
	/** At most this many first-sight pictures are started per client tick; the rest wait for the regular pass. */
	private static final int MOST_GRABS = 40;

	private final Client client;
	private final ObjectNames objectNames;
	private final SceneLoadGate gate = new SceneLoadGate();
	private final ProjectileSet projectiles = new ProjectileSet();
	private final SpotAnimTracker spotAnims = new SpotAnimTracker();
	private final SceneryScan scan;
	private final ModelPictures pictures;
	private final SessionList sessions = new SessionList();
	private final Map<ThingKey, Object> live = new HashMap<>();

	private Session current;
	/** The stopped session whose pictures are still being tried, with its live references; null when none. */
	private ThingTable finishingTable;
	private Map<ThingKey, Object> finishingLive;
	private int finishingTicks;
	private int grabs;
	private long startedMs;
	private int ticks;
	private Consumer<String> statusListener;
	private Consumer<SessionView> stopListener;

	@Inject
	Recorder(Client client, ClientThread clientThread, ScheduledExecutorService executor)
	{
		this.client = client;
		this.objectNames = new ObjectNames(client);
		this.scan = new SceneryScan(client, objectNames);
		this.pictures = new ModelPictures(client, clientThread, executor, new PictureMemory());
	}

	/** The picture maker, shared with the look-up so both use one picture memory. */
	ModelPictures pictures()
	{
		return pictures;
	}

	/** Sets who hears the status line. */
	void setStatusListener(Consumer<String> listener)
	{
		this.statusListener = listener;
	}

	/** Sets who gets the finished recording's view. */
	void setStopListener(Consumer<SessionView> listener)
	{
		this.stopListener = listener;
	}

	/** True while a recording runs. */
	boolean isRecording()
	{
		return current != null;
	}

	/** The kept sessions. */
	SessionList sessions()
	{
		return sessions;
	}

	/** Starts a recording with the NPCs already about and one nearby scan. */
	void start(long nowEpochMs, String title)
	{
		if (current != null)
		{
			return;
		}
		current = new Session(Session.SessionKind.RECORDING, title, nowEpochMs);
		startedMs = nowEpochMs;
		ticks = 0;
		grabs = 0;
		endFinishing();
		pictures.reset();
		final WorldView top = client.getTopLevelWorldView();
		if (top != null)
		{
			addNpcs(top);
		}
		final Player player = client.getLocalPlayer();
		if (player != null)
		{
			final WorldView own = player.getWorldView();
			if (own != null && own != top)
			{
				addNpcs(own);
			}
			objectNames.refreshImpostors();
			scanAndGrab(0L);
		}
		publishStatus(nowEpochMs);
	}

	/** Ends the recording and keeps it; does nothing when none runs. */
	void stop(long nowEpochMs)
	{
		if (current == null)
		{
			return;
		}
		final Session done = current;
		done.stop(nowEpochMs - startedMs);
		current = null;
		sessions.add(done);
		// Choice: the stopped session's table and references stay for 200 ticks so its pictures keep being tried.
		finishingTable = done.table();
		finishingLive = new HashMap<>(live);
		finishingTicks = 0;
		live.clear();
		projectiles.clear();
		spotAnims.clear();
		pictures.reset();
		if (stopListener != null)
		{
			stopListener.accept(done.view());
		}
	}

	void onNpcSpawned(NpcSpawned event)
	{
		if (current != null)
		{
			npc(event.getNpc());
		}
	}

	void onNpcChanged(NpcChanged event)
	{
		if (current != null)
		{
			npc(event.getNpc());
		}
	}

	/** Drops the finishing session's table and references. */
	void endFinishing()
	{
		finishingTable = null;
		finishingLive = null;
		finishingTicks = 0;
	}

	void onNpcDespawned(NpcDespawned event)
	{
		if (finishingLive != null)
		{
			forget(finishingLive, event.getNpc());
		}
		if (current == null)
		{
			return;
		}
		final NPC npc = event.getNpc();
		spotAnims.forget(npc);
		final NPCComposition comp = npc == null ? null : npc.getTransformedComposition();
		if (comp != null)
		{
			final String name = Names.clean(comp.getName());
			final ThingKey key = name != null ? ThingKey.named(Kind.NPC, Section.NPCS, name)
				: ThingKey.byId(Kind.NPC, Section.NPCS, null, npc.getId());
			current.table().lastSeen(key, at());
		}
		forget(live, npc);
	}

	private static void forget(Map<ThingKey, Object> refs, NPC npc)
	{
		final Iterator<Object> each = refs.values().iterator();
		while (each.hasNext())
		{
			if (each.next() == npc)
			{
				each.remove();
			}
		}
	}

	void onAnimationChanged(AnimationChanged event)
	{
		if (current == null || !(event.getActor() instanceof NPC))
		{
			return;
		}
		final NPC npc = (NPC) event.getActor();
		if (npc.getAnimation() >= 0)
		{
			current.table().animationSeen(npc.getAnimation(), nameOf(npc), at());
		}
	}

	void onGraphicChanged(GraphicChanged event)
	{
		if (current == null || !(event.getActor() instanceof NPC))
		{
			return;
		}
		final Actor npc = event.getActor();
		final List<Integer> ids = new ArrayList<>();
		for (ActorSpotAnim spot : npc.getSpotAnims())
		{
			if (spot.getId() >= 0)
			{
				ids.add(spot.getId());
			}
		}
		final String name = nameOf((NPC) npc);
		for (int id : spotAnims.newIds(npc, ids))
		{
			current.table().graphicSeen(id, name, at());
		}
	}

	void onGraphicsObjectCreated(GraphicsObjectCreated event)
	{
		if (current != null)
		{
			current.table().graphicSeen(event.getGraphicsObject().getId(), null, at());
		}
	}

	void onProjectileMoved(ProjectileMoved event)
	{
		if (current != null && projectiles.add(event.getProjectile()))
		{
			current.table().projectileSeen(event.getProjectile().getId(), at());
		}
	}

	void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (event.getGameObject() == null || ObjectTags.isSceneObject(event.getGameObject().getHash()))
		{
			object(event.getGameObject());
		}
	}

	void onWallObjectSpawned(WallObjectSpawned event)
	{
		object(event.getWallObject());
	}

	void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
	{
		object(event.getDecorativeObject());
	}

	void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		object(event.getGroundObject());
	}

	void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (current == null)
		{
			return;
		}
		final String spell = SpellNames.fromMenu(event.getMenuOption(), event.getMenuAction(), event.getMenuTarget());
		if (spell != null)
		{
			current.table().spellCast(spell, at());
		}
	}

	void onGameStateChanged(GameStateChanged event)
	{
		final GameState state = event.getGameState();
		gate.onGameState(state);
		if (state == GameState.LOADING)
		{
			objectNames.clear();
		}
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			stop(System.currentTimeMillis());
			// Choice: a logout or world hop ends the finishing too; the references belong to the old scene.
			endFinishing();
		}
	}

	/** A game tick: opens the gate, then while recording scans and pictures on their beats. */
	void onGameTick(long nowEpochMs)
	{
		gate.onTick();
		grabs = 0;
		if (finishingTable != null)
		{
			finishingTicks++;
			if (finishingTicks % PICTURES_EVERY == 0)
			{
				pictures.take(finishingTable, finishingLive);
			}
			if (finishingTicks >= FINISH_TICKS)
			{
				endFinishing();
			}
		}
		if (current == null)
		{
			return;
		}
		ticks++;
		if (ticks % SCAN_EVERY == 0)
		{
			scanAndGrab(nowEpochMs - startedMs);
		}
		if (ticks % PICTURES_EVERY == 0)
		{
			pictures.take(current.table(), live);
			publishStatus(nowEpochMs);
		}
	}

	private long at()
	{
		return System.currentTimeMillis() - startedMs;
	}

	/** The nearby scan; every key it adds a live reference for is grabbed at first sight. */
	private void scanAndGrab(long atMs)
	{
		final Set<ThingKey> before = new HashSet<>(live.keySet());
		scan.scan(current.table(), live, atMs);
		for (Map.Entry<ThingKey, Object> entry : live.entrySet())
		{
			if (!before.contains(entry.getKey()))
			{
				firstSight(entry.getKey(), entry.getValue());
			}
		}
	}

	/** First sight of a live thing: its picture is started at once, within the tick's budget of 40. */
	private void firstSight(ThingKey key, Object ref)
	{
		if (grabs < MOST_GRABS)
		{
			grabs++;
			pictures.grab(current.table(), key, ref);
		}
	}

	private void publishStatus(long nowEpochMs)
	{
		if (statusListener != null)
		{
			statusListener.accept(RecordingText.live(nowEpochMs - startedMs, current.table().size()));
		}
	}

	private void addNpcs(WorldView view)
	{
		if (view.npcs() == null)
		{
			return;
		}
		for (NPC npc : view.npcs())
		{
			npc(npc);
		}
	}

	private static String nameOf(NPC npc)
	{
		final NPCComposition comp = npc.getTransformedComposition();
		return comp == null ? null : comp.getName();
	}

	private void npc(NPC npc)
	{
		final NPCComposition comp = npc == null ? null : npc.getTransformedComposition();
		if (comp == null)
		{
			return;
		}
		final ThingKey key = current.table().npcSeen(comp.getName(), npc.getId(), at());
		if (key != null && live.put(key, npc) == null)
		{
			firstSight(key, npc);
			current.table().setActions(key, comp.getActions() == null ? new ArrayList<String>()
				: Arrays.asList(comp.getActions()));
		}
	}

	private void object(TileObject object)
	{
		if (current == null || object == null || !gate.spawnsCount())
		{
			return;
		}
		final WorldPoint where = object.getWorldLocation();
		final Place place = where == null ? null : new Place(where.getX(), where.getY(), where.getPlane());
		final ThingKey key = current.table().objectSpawned(objectNames.nameOf(object.getId()), object.getId(), place, at());
		if (key != null && live.put(key, object) == null)
		{
			firstSight(key, object);
		}
	}
}
