package com.scenecustomizer;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.hooks.DrawCallbacks;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@link Engine}: the zone refresh after a rule goes, and a big object being walked once. */
public class EngineTest
{
	private static final int SCENE_X = 20;
	private static final int SCENE_Z = 37;
	private static final long HASH = (2L << 16) | SCENE_X | ((long) SCENE_Z << 7);
	private static final WorldPoint AT = new WorldPoint(3200, 3200, 0);
	private static final Place PLACE = new Place(3200, 3200, 0);

	private final Client client = mock(Client.class);
	private final DrawCallbacks callbacks = mock(DrawCallbacks.class);
	private final Scene scene = mock(Scene.class);
	private final Rules rules = new Rules(mock(ConfigManager.class), new Gson());

	private void sceneWith(String name, int id, GameObject object, int tiles)
	{
		final ObjectComposition def = mock(ObjectComposition.class);
		when(def.getName()).thenReturn(name);
		when(client.getObjectDefinition(id)).thenReturn(def);
		when(object.getId()).thenReturn(id);
		when(object.getHash()).thenReturn(HASH);
		when(object.getWorldLocation()).thenReturn(AT);
		final Tile[][][] planes = new Tile[1][4][4];
		for (int i = 0; i < tiles; i++)
		{
			final Tile tile = mock(Tile.class);
			when(tile.getGameObjects()).thenReturn(new GameObject[] {object});
			planes[0][i][0] = tile;
		}
		final WorldView top = mock(WorldView.class);
		when(top.getScene()).thenReturn(scene);
		when(scene.getTiles()).thenReturn(planes);
		when(client.getTopLevelWorldView()).thenReturn(top);
		when(client.getDrawCallbacks()).thenReturn(callbacks);
	}

	@Test
	public void anUndoRefreshesTheZoneThatHeldTheHiddenObject()
	{
		sceneWith("Crate", 500, mock(GameObject.class), 1);
		rules.put(new Rule(Kind.OBJECT, "Crate", null, Collections.<Place>emptyList(), HideScope.EVERYWHERE,
			Collections.<Place>emptyList()));
		// An object rule for a name not in the scene keeps the object walk in use after the Crate rule goes: with no
		// object rule left the engine takes another branch, which refreshed the old zones even before the fix.
		rules.put(new Rule(Kind.OBJECT, "Barrel", null, Collections.<Place>emptyList(), HideScope.EVERYWHERE,
			Collections.<Place>emptyList()));
		final Engine engine = new Engine(client, rules);
		final int zx = Zones.zoneX(HASH);
		final int zz = Zones.zoneZ(HASH);

		engine.rulesChanged();
		rules.remove(Kind.OBJECT, "Crate");
		engine.rulesChanged();

		verify(callbacks, times(2)).invalidateZone(scene, zx, zz);
		verify(callbacks, times(2)).invalidateZone(any(Scene.class), anyInt(), anyInt());
	}

	@Test
	public void aBigObjectIsOutlinedOnce()
	{
		final GameObject tree = mock(GameObject.class);
		sceneWith("Tree", 1276, tree, 4);
		rules.put(new Rule(Kind.OBJECT, "Tree", new Outline(0xFF0000, 2), Arrays.asList(PLACE), HideScope.OFF, null));
		final Engine engine = new Engine(client, rules);

		engine.rulesChanged();

		assertEquals(1, engine.decisions().outlinedObjects().size());
		assertEquals(tree, engine.decisions().outlinedObjects().get(0).object);
	}
}
