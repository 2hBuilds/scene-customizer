package com.scenecustomizer;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Renderable;
import net.runelite.api.TextureProvider;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.client.callback.ClientThread;

/**
 * Draws the small pictures from the game's own models. The models are found and copied on the client thread; the
 * painting runs on the injected executor; the picture goes back to the table through the client thread. Nothing
 * waits.
 *
 * <p>Choice: a thing that is live but has no model yet (the game builds it only while drawing it) is "not ready":
 * it waits and is asked for again every 10 passes, and is never given up. A miss (no live thing, a model that
 * cannot be copied, a painting that fails) costs the picture one try, up to the table's limit. Choice: an NPC is drawn from the id of the live NPC first seen
 * under that name, and a row is not drawn twice at once because it leaves the wanted list as soon as it has a picture or has
 * used its tries; a picture still being painted may be asked for again, and the table keeps the first to arrive.
 */
public class ModelPictures
{
	/** NPC lighting, as the client does it. */
	private static final int AMBIENT = 64;
	private static final int CONTRAST = 850;
	private static final int LIGHT_X = -30;
	private static final int LIGHT_Y = -50;
	private static final int LIGHT_Z = -30;

	private static final int MOST_PER_PASS = 8;
	/** A key that missed is not tried again for this many passes (about 12 seconds). */
	private static final int RETRY_AFTER = 10;

	private final Client client;
	private final ClientThread clientThread;
	private final ScheduledExecutorService executor;
	/**
	 * Choice: the pass count and the next-try map are safe for any thread, because the recorder may restart them from
	 * the Swing thread while the client thread takes passes.
	 */
	private final AtomicInteger pass = new AtomicInteger();
	private final Map<ThingKey, Integer> nextPass = new ConcurrentHashMap<>();
	private final PictureMemory memory;
	/** Called on the client thread whenever a picture lands in any table; null when nobody listens. */
	private volatile Runnable landed;

	public ModelPictures(Client client, ClientThread clientThread, ScheduledExecutorService executor, PictureMemory memory)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.executor = executor;
		this.memory = memory;
	}

	/** Sets who is told (on the client thread) each time a picture lands in a table. */
	void setLandedListener(Runnable listener)
	{
		this.landed = listener;
	}

	/** Forgets every remembered picture; the plugin calls it in its shutDown. */
	void clearMemory()
	{
		memory.clear();
	}

	private void landed()
	{
		final Runnable listener = landed;
		if (listener != null)
		{
			listener.run();
		}
	}

	/** The id the picture memory keeps this live thing under; null when it has none. */
	private static String memoryId(Object ref)
	{
		if (ref instanceof NPC)
		{
			return "npc:" + ((NPC) ref).getId();
		}
		if (ref instanceof TileObject)
		{
			return "object:" + ((TileObject) ref).getId();
		}
		return null;
	}

	/** The model of a live thing as {@code take} finds it: an NPC's definition, else its own; an object's own. */
	private Model modelOf(Object ref)
	{
		if (ref instanceof NPC)
		{
			final NPC npc = (NPC) ref;
			final Model model = npcModel(npc.getId());
			// Choice: when the definition is not ready, the live NPC's own model, in whatever pose it holds.
			return model != null ? model : npc.getModel();
		}
		return ref instanceof TileObject ? objectModel((TileObject) ref) : null;
	}

	/** Gives the table the remembered picture of the id, if there is one. True when it did. Client thread. */
	private boolean remembered(ThingTable table, ThingKey key, String id)
	{
		final BufferedImage known = id == null ? null : memory.get(id);
		if (known == null)
		{
			return false;
		}
		table.pictureTaken(key, known);
		landed();
		return true;
	}

	/**
	 * First sight of a thing: its remembered picture at once; else its model is copied now and the painting queued.
	 * False when the game has no model for it yet ("not ready"); {@code take} asks again later. Client thread.
	 * Choice: a grab costs no try and sets no wait.
	 */
	boolean grab(ThingTable table, ThingKey key, Object liveRef)
	{
		if (liveRef == null)
		{
			return false;
		}
		final String id = memoryId(liveRef);
		if (remembered(table, key, id))
		{
			return true;
		}
		final Model model = modelOf(liveRef);
		return model != null && paintLater(table, key, model, id);
	}

	/** The lit model of an NPC from its definition; null when the definition or any of its parts is missing. */
	public Model npcModel(int npcId)
	{
		final NPCComposition definition = client.getNpcDefinition(npcId);
		if (definition == null)
		{
			return null;
		}
		final int[] ids = definition.getModels();
		if (ids == null || ids.length == 0)
		{
			return null;
		}
		final ModelData[] parts = new ModelData[ids.length];
		for (int i = 0; i < ids.length; i++)
		{
			parts[i] = client.loadModelData(ids[i]);
			if (parts[i] == null)
			{
				return null;
			}
		}
		final ModelData merged = client.mergeModels(parts);
		if (merged == null)
		{
			return null;
		}
		final ModelData coloured = merged.cloneColors();
		if (coloured == null)
		{
			return null;
		}
		final short[] find = definition.getColorToReplace();
		final short[] replace = definition.getColorToReplaceWith();
		if (find != null && replace != null)
		{
			for (int i = 0; i < Math.min(find.length, replace.length); i++)
			{
				coloured.recolor(find[i], replace[i]);
			}
		}
		return coloured.light(AMBIENT, CONTRAST, LIGHT_X, LIGHT_Y, LIGHT_Z);
	}

	/** The model an object is drawn with, as it stands; null when it has none. */
	public static Model objectModel(TileObject object)
	{
		Renderable renderable = null;
		if (object instanceof GameObject)
		{
			renderable = ((GameObject) object).getRenderable();
		}
		else if (object instanceof WallObject)
		{
			renderable = ((WallObject) object).getRenderable1();
		}
		else if (object instanceof DecorativeObject)
		{
			renderable = ((DecorativeObject) object).getRenderable();
		}
		else if (object instanceof GroundObject)
		{
			renderable = ((GroundObject) object).getRenderable();
		}
		if (renderable == null)
		{
			return null;
		}
		return renderable instanceof Model ? (Model) renderable : renderable.getModel();
	}

	/** Forgets the pass count and every wait; the recorder calls it at each start and stop. */
	public void reset()
	{
		pass.set(0);
		nextPass.clear();
	}

	/**
	 * Starts pictures for up to 8 Things that still need one; the caller keeps this to one call every 2 ticks. A key
	 * that misses waits 10 passes before it is tried again; a key whose live thing gives no model is "not ready" and
	 * waits the same 10 passes without costing a try, so it is never given up.
	 */
	public void take(ThingTable table, Map<ThingKey, Object> live)
	{
		final int now = pass.incrementAndGet();
		int started = 0;
		final List<ThingKey> needing = table.needingPictures();
		nextPass.keySet().retainAll(new HashSet<>(needing));
		for (ThingKey key : needing)
		{
			if (started >= MOST_PER_PASS)
			{
				break;
			}
			final Integer wait = nextPass.get(key);
			if (wait != null && now < wait)
			{
				continue;
			}
			final Object ref = live.get(key);
			final String id = memoryId(ref);
			if (remembered(table, key, id))
			{
				nextPass.remove(key);
				continue;
			}
			final Model model = modelOf(ref);
			if (model == null && ref != null)
			{
				// Not ready: the game builds the live shape only while it draws it. Wait, and never give up.
				nextPass.put(key, now + RETRY_AFTER);
			}
			else if (paintLater(table, key, model, id))
			{
				started++;
			}
			else
			{
				table.pictureMissed(key);
				nextPass.put(key, now + RETRY_AFTER);
			}
		}
	}

	/**
	 * Draws a look-up row from the first of the NPC ids that gives a model. True when a picture was started; false
	 * when no id gave a model, so the caller may use another picture. Client thread.
	 */
	public boolean draw(ThingTable table, ThingKey key, Collection<Integer> npcIds)
	{
		for (int id : npcIds)
		{
			if (remembered(table, key, "npc:" + id) || paintLater(table, key, npcModel(id), "npc:" + id))
			{
				return true;
			}
		}
		return false;
	}

	/** Copies the model now and paints it on the executor; false when there is nothing to paint. Client thread. */
	private boolean paintLater(ThingTable table, ThingKey key, Model model, String memoryId)
	{
		final Mesh mesh = Mesh.copyOf(model);
		if (mesh == null)
		{
			return false;
		}
		final Map<Integer, Integer> colours = new HashMap<>();
		final Map<Integer, int[]> pixels = new HashMap<>();
		final Set<Integer> used = ModelPainter.texturesOf(mesh);
		final TextureProvider textures = used.isEmpty() ? null : client.getTextureProvider();
		if (textures != null)
		{
			for (int texture : used)
			{
				colours.put(texture, textures.getDefaultColor(texture));
				// Choice: the pixels are copied here, on the client thread; a texture that is not ready is skipped
				// and its faces keep the flat colour.
				final int[] loaded = textures.load(texture);
				if (loaded != null)
				{
					pixels.put(texture, loaded.clone());
				}
			}
		}
		executor.execute(() ->
		{
			BufferedImage picture = null;
			try
			{
				picture = ModelPainter.paint(mesh, texture -> colours.getOrDefault(texture, 0), pixels::get);
			}
			catch (RuntimeException e)
			{
				picture = null;
			}
			final BufferedImage done = picture;
			clientThread.invokeLater(() ->
			{
				if (done == null)
				{
					// A failed painting waits like any other miss (L7 review): no three quick misses.
					nextPass.put(key, pass.get() + RETRY_AFTER);
					table.pictureMissed(key);
				}
				else
				{
					memory.put(memoryId, done);
					table.pictureTaken(key, done);
					landed();
				}
			});
		});
		return true;
	}
}
