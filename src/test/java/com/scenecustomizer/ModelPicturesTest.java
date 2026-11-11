package com.scenecustomizer;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Renderable;
import net.runelite.client.callback.ClientThread;
import org.junit.Test;
import org.mockito.InOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** {@link ModelPictures}: finding the models and handing out the painting. */
public class ModelPicturesTest
{
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

	@Test
	public void anNpcModelFromItsDefinition()
	{
		final Client client = mock(Client.class);
		final NPCComposition definition = mock(NPCComposition.class);
		when(definition.getModels()).thenReturn(new int[] {1, 2});
		when(definition.getColorToReplace()).thenReturn(new short[] {10});
		when(definition.getColorToReplaceWith()).thenReturn(new short[] {20});
		when(client.getNpcDefinition(7)).thenReturn(definition);
		final ModelData one = mock(ModelData.class);
		final ModelData two = mock(ModelData.class);
		final ModelData merged = mock(ModelData.class);
		final ModelData coloured = mock(ModelData.class);
		final Model lit = mock(Model.class);
		when(client.loadModelData(1)).thenReturn(one);
		when(client.loadModelData(2)).thenReturn(two);
		when(client.mergeModels(one, two)).thenReturn(merged);
		when(merged.cloneColors()).thenReturn(coloured);
		when(coloured.light(64, 850, -30, -50, -30)).thenReturn(lit);
		final ModelPictures pictures = new ModelPictures(client, mock(ClientThread.class),
			mock(ScheduledExecutorService.class), new PictureMemory());

		assertSame(lit, pictures.npcModel(7));
		final InOrder order = inOrder(client, merged, coloured);
		order.verify(client).loadModelData(1);
		order.verify(client).loadModelData(2);
		order.verify(client).mergeModels(one, two);
		order.verify(merged).cloneColors();
		order.verify(coloured).recolor((short) 10, (short) 20);
		order.verify(coloured).light(64, 850, -30, -50, -30);

		when(definition.getModels()).thenReturn(new int[] {1, 3});
		assertNull(pictures.npcModel(7));
	}

	@Test
	public void anNpcFallsBackToItsLiveModel()
	{
		final Client client = mock(Client.class);
		final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
		final NPCComposition definition = mock(NPCComposition.class);
		when(definition.getModels()).thenReturn(new int[] {1});
		when(client.getNpcDefinition(7)).thenReturn(definition);
		final NPC npc = mock(NPC.class);
		when(npc.getId()).thenReturn(7);
		final Model live7 = triangleModel();
		when(npc.getModel()).thenReturn(live7);
		final ThingTable table = new ThingTable();
		final ThingKey key = table.npcSeen("Brugsen", 7, 0L);
		final Map<ThingKey, Object> live = new HashMap<>();
		live.put(key, npc);
		new ModelPictures(client, mock(ClientThread.class), executor, new PictureMemory()).take(table, live);
		verify(executor, times(1)).execute(any(Runnable.class));

		final ScheduledExecutorService quiet = mock(ScheduledExecutorService.class);
		final NPC bare = mock(NPC.class);
		when(bare.getId()).thenReturn(7);
		live.put(key, bare);
		final ModelPictures none = new ModelPictures(client, mock(ClientThread.class), quiet, new PictureMemory());
		for (int pass = 0; pass < 5; pass++)
		{
			assertEquals(1, table.needingPictures().size());
			none.reset();
			none.take(table, live);
		}
		verify(quiet, never()).execute(any(Runnable.class));
		assertEquals(1, table.needingPictures().size());
	}

	/** T19: a thing whose model is not built yet is never given up; it paints once the model appears. */
	@Test
	public void anObjectWithNoModelYetIsNeverGivenUp()
	{
		final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
		final ThingTable table = new ThingTable();
		final ThingKey key = table.objectSpawned("Bank booth", 55, new Place(1, 2, 0), 0L);
		final GameObject object = mock(GameObject.class);
		final Map<ThingKey, Object> live = new HashMap<>();
		live.put(key, object);
		final ModelPictures pictures = new ModelPictures(mock(Client.class), mock(ClientThread.class), executor, new PictureMemory());
		for (int pass = 0; pass < 5; pass++)
		{
			assertEquals(1, table.needingPictures().size());
			pictures.reset();
			pictures.take(table, live);
		}
		verify(executor, never()).execute(any(Runnable.class));
		assertEquals(1, table.needingPictures().size());

		final Model built = triangleModel();
		when(object.getRenderable()).thenReturn(built);
		pictures.reset();
		pictures.take(table, live);
		verify(executor, times(1)).execute(any(Runnable.class));
	}

	@Test
	public void aMissWaitsTenPassesBeforeTheNextTry()
	{
		final Client client = mock(Client.class);
		final NPC npc = mock(NPC.class);
		when(npc.getId()).thenReturn(7);
		final ThingTable table = new ThingTable();
		final Map<ThingKey, Object> live = new HashMap<>();
		live.put(table.npcSeen("Nobody", 7, 0L), npc);
		final ModelPictures pictures = new ModelPictures(client, mock(ClientThread.class),
			mock(ScheduledExecutorService.class), new PictureMemory());

		pictures.take(table, live);
		verify(client, times(1)).getNpcDefinition(7);
		for (int pass = 2; pass <= 10; pass++)
		{
			pictures.take(table, live);
		}
		verify(client, times(1)).getNpcDefinition(7);
		pictures.take(table, live);
		verify(client, times(2)).getNpcDefinition(7);

		pictures.reset();
		pictures.take(table, live);
		verify(client, times(3)).getNpcDefinition(7);
	}

	/** T18, added by the lead for the L7 review: a painting that comes back empty waits ten passes like any miss. */
	@Test
	public void aFailedPaintingAlsoWaitsTenPasses()
	{
		final Client client = mock(Client.class);
		final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
		doAnswer(call ->
		{
			((Runnable) call.getArgument(0)).run();
			return null;
		}).when(executor).execute(any(Runnable.class));
		final ClientThread clientThread = mock(ClientThread.class);
		doAnswer(call ->
		{
			((Runnable) call.getArgument(0)).run();
			return null;
		}).when(clientThread).invokeLater(any(Runnable.class));
		final Model hidden = triangleModel();
		when(hidden.getFaceColors3()).thenReturn(new int[] {-2});
		final NPC npc = mock(NPC.class);
		when(npc.getId()).thenReturn(7);
		when(npc.getModel()).thenReturn(hidden);
		final ThingTable table = new ThingTable();
		final Map<ThingKey, Object> live = new HashMap<>();
		live.put(table.npcSeen("Ghost", 7, 0L), npc);
		final ModelPictures pictures = new ModelPictures(client, clientThread, executor, new PictureMemory());

		pictures.take(table, live);
		verify(executor, times(1)).execute(any(Runnable.class));
		for (int pass = 2; pass <= 10; pass++)
		{
			pictures.take(table, live);
		}
		verify(executor, times(1)).execute(any(Runnable.class));
		pictures.take(table, live);
		verify(executor, times(2)).execute(any(Runnable.class));
	}

	@Test
	public void anObjectModelFromItsRenderable()
	{
		final ModelPictures pictures = new ModelPictures(mock(Client.class), mock(ClientThread.class),
			mock(ScheduledExecutorService.class), new PictureMemory());
		final GameObject direct = mock(GameObject.class);
		final Model model = mock(Model.class);
		when(direct.getRenderable()).thenReturn(model);
		assertSame(model, pictures.objectModel(direct));

		final GameObject indirect = mock(GameObject.class);
		final Renderable renderable = mock(Renderable.class);
		when(renderable.getModel()).thenReturn(model);
		when(indirect.getRenderable()).thenReturn(renderable);
		assertSame(model, pictures.objectModel(indirect));

		assertNull(pictures.objectModel(mock(GameObject.class)));
	}

	@Test
	public void atMostFourAPassAndMissesWithoutAModel()
	{
		final Client client = mock(Client.class);
		final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
		final ClientThread clientThread = mock(ClientThread.class);
		doAnswer(call ->
		{
			((Runnable) call.getArgument(0)).run();
			return null;
		}).when(executor).execute(any(Runnable.class));
		doAnswer(call ->
		{
			((Runnable) call.getArgument(0)).run();
			return null;
		}).when(clientThread).invokeLater(any(Runnable.class));
		final Model lit = triangleModel();
		final ThingTable table = new ThingTable();
		final Map<ThingKey, Object> live = new HashMap<>();
		for (int i = 0; i < 10; i++)
		{
			final NPCComposition definition = mock(NPCComposition.class);
			when(definition.getModels()).thenReturn(new int[] {100 + i});
			when(client.getNpcDefinition(i)).thenReturn(definition);
			final ModelData data = mock(ModelData.class);
			final ModelData coloured = mock(ModelData.class);
			when(client.loadModelData(100 + i)).thenReturn(data);
			when(client.mergeModels(data)).thenReturn(data);
			when(data.cloneColors()).thenReturn(coloured);
			when(coloured.light(64, 850, -30, -50, -30)).thenReturn(lit);
			final NPC npc = mock(NPC.class);
			when(npc.getId()).thenReturn(i);
			live.put(table.npcSeen("Npc" + i, i, 0L), npc);
		}
		final ModelPictures pictures = new ModelPictures(client, clientThread, executor, new PictureMemory());

		pictures.take(table, live);
		verify(executor, times(8)).execute(any(Runnable.class));
		assertEquals(2, table.needingPictures().size());

		final ThingTable objects = new ThingTable();
		objects.objectSpawned("Rock", 55, new Place(1, 2, 0), 0L);
		final ScheduledExecutorService quiet = mock(ScheduledExecutorService.class);
		final ModelPictures none = new ModelPictures(client, clientThread, quiet, new PictureMemory());
		for (int pass = 0; pass < Caps.PICTURE_TRIES; pass++)
		{
			assertEquals(1, objects.needingPictures().size());
			none.reset();
			none.take(objects, new HashMap<>());
		}
		verify(quiet, never()).execute(any(Runnable.class));
		assertTrue(objects.needingPictures().isEmpty());
	}

	/** A table of {@code count} NPCs, each with a live model, and the live map for it. */
	private static Map<ThingKey, Object> livePack(ThingTable table, int count)
	{
		final Map<ThingKey, Object> live = new HashMap<>();
		for (int i = 0; i < count; i++)
		{
			final NPC npc = mock(NPC.class);
			when(npc.getId()).thenReturn(i);
			final Model built = triangleModel();
			when(npc.getModel()).thenReturn(built);
			live.put(table.npcSeen("Npc" + i, i, 0L), npc);
		}
		return live;
	}

	private static ScheduledExecutorService inlineExecutor()
	{
		final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
		doAnswer(call ->
		{
			((Runnable) call.getArgument(0)).run();
			return null;
		}).when(executor).execute(any(Runnable.class));
		return executor;
	}

	private static ClientThread inlineClientThread()
	{
		final ClientThread clientThread = mock(ClientThread.class);
		doAnswer(call ->
		{
			((Runnable) call.getArgument(0)).run();
			return null;
		}).when(clientThread).invokeLater(any(Runnable.class));
		return clientThread;
	}

	/** T21: the memory holds npc:7; a grab needs no model and asks the client for nothing. */
	@Test
	public void aRememberedPictureNeedsNoModel()
	{
		final Client client = mock(Client.class);
		final PictureMemory memory = new PictureMemory();
		final BufferedImage remembered = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
		memory.put("npc:7", remembered);
		final NPC npc = mock(NPC.class);
		when(npc.getId()).thenReturn(7);
		final ThingTable table = new ThingTable();
		final ThingKey key = table.npcSeen("Brugsen", 7, 0L);
		final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
		final ModelPictures pictures = new ModelPictures(client, mock(ClientThread.class), executor, memory);

		assertTrue(pictures.grab(table, key, npc));
		assertTrue(table.needingPictures().isEmpty());
		assertSame(remembered, table.sectionViews().get(Section.NPCS).get(0).picture());
		verifyNoInteractions(client);
		verify(executor, never()).execute(any(Runnable.class));
	}

	/** T24: each landed picture calls the listener. */
	@Test
	public void eachLandedPictureCallsTheListener()
	{
		final ThingTable table = new ThingTable();
		final Map<ThingKey, Object> live = livePack(table, 2);
		final ModelPictures pictures = new ModelPictures(mock(Client.class), inlineClientThread(), inlineExecutor(),
			new PictureMemory());
		final AtomicInteger calls = new AtomicInteger();
		pictures.setLandedListener(calls::incrementAndGet);

		pictures.take(table, live);
		assertEquals(2, calls.get());
	}

	/** T25: ten keys with models; one take starts eight paintings. */
	@Test
	public void eightAPass()
	{
		final ThingTable table = new ThingTable();
		final Map<ThingKey, Object> live = livePack(table, 10);
		final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
		new ModelPictures(mock(Client.class), mock(ClientThread.class), executor, new PictureMemory()).take(table, live);
		verify(executor, times(8)).execute(any(Runnable.class));
	}

	/** T26: a finished painting is put in the memory under its thing's id. */
	@Test
	public void aFinishedPaintingIsRemembered()
	{
		final PictureMemory memory = new PictureMemory();
		final ThingTable table = new ThingTable();
		final Map<ThingKey, Object> live = new HashMap<>();
		final NPC npc = mock(NPC.class);
		when(npc.getId()).thenReturn(7);
		final Model built = triangleModel();
		when(npc.getModel()).thenReturn(built);
		live.put(table.npcSeen("Brugsen", 7, 0L), npc);
		assertNull(memory.get("npc:7"));

		new ModelPictures(mock(Client.class), inlineClientThread(), inlineExecutor(), memory).take(table, live);
		assertNotNull(memory.get("npc:7"));
	}
}
