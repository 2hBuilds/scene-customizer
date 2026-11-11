package com.scenecustomizer;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Model;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.IndexedObjectSet;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.callback.ClientThread;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@link Recorder}: what a recording catches, with a mocked client. */
public class RecorderTest
{
	private Client client;
	private Recorder recorder;

	@Before
	public void setUp()
	{
		client = mock(Client.class);
		recorder = new Recorder(client, mock(ClientThread.class), mock(ScheduledExecutorService.class));
	}

	private static NPC vorkath()
	{
		final NPC npc = mock(NPC.class);
		final NPCComposition comp = mock(NPCComposition.class);
		when(comp.getName()).thenReturn("Vorkath");
		when(comp.getActions()).thenReturn(new String[] {"Attack", null, "Examine"});
		when(npc.getTransformedComposition()).thenReturn(comp);
		when(npc.getId()).thenReturn(8059);
		return npc;
	}

	private static GameStateChanged state(GameState s)
	{
		final GameStateChanged e = new GameStateChanged();
		e.setGameState(s);
		return e;
	}

	private static AnimationChanged animation(Actor actor)
	{
		final AnimationChanged e = new AnimationChanged();
		e.setActor(actor);
		return e;
	}

	private static List<ThingView> list(Session s, Section section)
	{
		return s.view().sections().get(section);
	}

	@Test
	public void npcsCountOnlyWhileRecording()
	{
		final NPC npc = vorkath();
		recorder.onNpcSpawned(new NpcSpawned(npc));
		recorder.start(1000L, "R");
		recorder.onNpcSpawned(new NpcSpawned(npc));
		recorder.stop(2000L);
		final List<ThingView> npcs = list(recorder.sessions().recordings().get(0), Section.NPCS);
		assertEquals(1, npcs.size());
		assertEquals("Vorkath", npcs.get(0).label());
		assertEquals(1, npcs.get(0).seen());
		assertEquals(Arrays.asList("Attack", "Examine"), npcs.get(0).actions());
	}

	@Test
	public void playersAreNeverRecorded()
	{
		recorder.start(1000L, "R");
		final Player player = mock(Player.class);
		when(player.getAnimation()).thenReturn(808);
		recorder.onAnimationChanged(animation(player));
		final NPC npc = vorkath();
		when(npc.getAnimation()).thenReturn(7952);
		recorder.onAnimationChanged(animation(npc));
		recorder.stop(2000L);
		final List<ThingView> anims = list(recorder.sessions().recordings().get(0), Section.ANIMATIONS);
		assertEquals(1, anims.size());
		assertEquals("Animation 7952", anims.get(0).label());
		assertEquals(Arrays.asList("Vorkath"), anims.get(0).hints());
	}

	@Test
	public void objectSpawnsDuringALoadAreLeftOut()
	{
		final ObjectComposition def = mock(ObjectComposition.class);
		when(def.getName()).thenReturn("Altar");
		when(client.getObjectDefinition(10)).thenReturn(def);
		final GameObject object = mock(GameObject.class);
		when(object.getId()).thenReturn(10);
		when(object.getHash()).thenReturn(2L << 16);
		when(object.getWorldLocation()).thenReturn(new WorldPoint(3200, 3200, 0));
		final GameObjectSpawned spawn = new GameObjectSpawned();
		spawn.setGameObject(object);

		recorder.start(1000L, "R");
		recorder.onGameStateChanged(state(GameState.LOADING));
		recorder.onGameObjectSpawned(spawn);
		recorder.onGameStateChanged(state(GameState.LOGGED_IN));
		recorder.onGameObjectSpawned(spawn);
		recorder.stop(1500L);
		assertNull(list(recorder.sessions().recordings().get(0), Section.OBJECTS));

		recorder.start(2000L, "R2");
		recorder.onGameTick(2600L);
		recorder.onGameObjectSpawned(spawn);
		recorder.stop(3000L);
		final List<ThingView> objects = list(recorder.sessions().recordings().get(0), Section.OBJECTS);
		assertEquals(1, objects.size());
		assertEquals("Altar", objects.get(0).label());
		assertEquals(1, objects.get(0).seen());
	}

	@Test
	public void logoutAndHopStopTheRecording()
	{
		recorder.start(1000L, "R");
		recorder.onGameStateChanged(state(GameState.LOGIN_SCREEN));
		assertFalse(recorder.isRecording());
		assertEquals(1, recorder.sessions().recordings().size());
		assertEquals(Session.SessionState.DONE, recorder.sessions().recordings().get(0).view().state());
		recorder.start(2000L, "R");
		recorder.onGameStateChanged(state(GameState.HOPPING));
		assertFalse(recorder.isRecording());
		assertEquals(2, recorder.sessions().recordings().size());
	}

	@Test
	public void keepsTheLastFiveRecordings()
	{
		for (int i = 0; i < 6; i++)
		{
			recorder.start(1000L * i, "R" + i);
			assertTrue(recorder.isRecording());
			recorder.stop(1000L * i + 500L);
		}
		final List<Session> kept = recorder.sessions().recordings();
		assertEquals(5, kept.size());
		assertEquals("R5", kept.get(0).view().title());
		assertEquals("R1", kept.get(4).view().title());
	}

	private static Model triangleModel()
	{
		final Model model = mock(Model.class);
		when(model.getFaceCount()).thenReturn(1);
		when(model.getVerticesX()).thenReturn(new float[] {0, 10, 0});
		when(model.getVerticesY()).thenReturn(new float[] {0, 0, 10});
		when(model.getVerticesZ()).thenReturn(new float[] {0, 0, 0});
		when(model.getFaceIndices1()).thenReturn(new int[] {0});
		when(model.getFaceIndices2()).thenReturn(new int[] {1});
		when(model.getFaceIndices3()).thenReturn(new int[] {2});
		when(model.getFaceColors1()).thenReturn(new int[] {64});
		when(model.getFaceColors2()).thenReturn(new int[] {64});
		when(model.getFaceColors3()).thenReturn(new int[] {-1});
		return model;
	}

	private static NPC named(String name, int id, Model model)
	{
		final NPC npc = mock(NPC.class);
		final NPCComposition comp = mock(NPCComposition.class);
		when(comp.getName()).thenReturn(name);
		when(npc.getTransformedComposition()).thenReturn(comp);
		when(npc.getId()).thenReturn(id);
		when(npc.getModel()).thenReturn(model);
		return npc;
	}

	@SuppressWarnings("unchecked")
	private static void npcsAbout(Client client, NPC... npcs)
	{
		final WorldView top = mock(WorldView.class);
		final IndexedObjectSet<NPC> set = mock(IndexedObjectSet.class);
		when(set.iterator()).thenAnswer(call -> Arrays.asList(npcs).iterator());
		doReturn(set).when(top).npcs();
		when(client.getTopLevelWorldView()).thenReturn(top);
	}

	/** T20: three NPCs about at the start each get a painting started before any game tick. */
	@Test
	public void startGrabsEveryNpcAtOnce()
	{
		final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
		final Recorder grabbing = new Recorder(client, mock(ClientThread.class), executor);
		npcsAbout(client, named("A", 1, triangleModel()), named("B", 2, triangleModel()), named("C", 3, triangleModel()));

		grabbing.start(1000L, "R");
		verify(executor, times(3)).execute(any(Runnable.class));
	}

	/** T23: after the stop the session keeps being tried for 200 ticks, then no more. */
	@Test
	public void afterStopTheRecordingKeepsTrying()
	{
		final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
		final Recorder trying = new Recorder(client, mock(ClientThread.class), executor);
		final NPC late = named("Late", 5, null);
		npcsAbout(client, late);
		trying.start(1000L, "R");
		trying.stop(1100L);
		verify(executor, never()).execute(any(Runnable.class));

		final Model appeared = triangleModel();
		when(late.getModel()).thenReturn(appeared);
		for (int tick = 0; tick < 20; tick++)
		{
			trying.onGameTick(1200L + tick);
		}
		verify(executor, atLeastOnce()).execute(any(Runnable.class));

		final ScheduledExecutorService quiet = mock(ScheduledExecutorService.class);
		final Recorder giving = new Recorder(client, mock(ClientThread.class), quiet);
		npcsAbout(client, named("Never", 6, null));
		giving.start(2000L, "R2");
		giving.stop(2100L);
		for (int tick = 0; tick < 200; tick++)
		{
			giving.onGameTick(2200L + tick);
		}
		final int asked = mockingDetails(client).getInvocations().size();
		for (int tick = 0; tick < 40; tick++)
		{
			giving.onGameTick(3000L + tick);
		}
		assertEquals(asked, mockingDetails(client).getInvocations().size());
		verify(quiet, never()).execute(any(Runnable.class));
	}
}
