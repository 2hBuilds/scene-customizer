package com.scenecustomizer;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.TileObject;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** {@link HideCallback}: it reads the decisions and nothing else. */
public class HideCallbackTest
{
	private static TileObject object(int id, long hash)
	{
		final TileObject object = mock(TileObject.class);
		when(object.getId()).thenReturn(id);
		when(object.getHash()).thenReturn(hash);
		return object;
	}

	@Test
	public void onlyHiddenNpcsAreLeftOut()
	{
		final NPC hidden = mock(NPC.class);
		final Decisions decisions = new Decisions();
		decisions.setNpcs(Collections.singleton(hidden), new IdentityHashMap<NPC, Outline>());
		final HideCallback callback = new HideCallback(decisions);
		assertFalse(callback.addEntity(hidden, false));
		assertFalse(callback.addEntity(hidden, true));
		assertTrue(callback.addEntity(mock(NPC.class), false));
		assertTrue(callback.addEntity(mock(Player.class), false));
	}

	@Test
	public void objectsNeedTheSceneTagAndAreNeverBlocked()
	{
		final Decisions decisions = new Decisions();
		final Set<Integer> ids = new HashSet<>();
		ids.add(500);
		ids.add(ObjectID.TOB_BLOAT_PILLAR);
		decisions.setObjects(ids, Collections.<Long>emptySet(), Collections.<OutlinedObject>emptyList(),
			Collections.<Long>emptySet());
		final HideCallback callback = new HideCallback(decisions);
		assertFalse(callback.drawObject(null, object(500, 2L << 16)));
		assertTrue(callback.drawObject(null, object(500, 1L << 16)));
		assertTrue(callback.drawObject(null, object(ObjectID.TOB_BLOAT_PILLAR, 2L << 16)));
		assertTrue(callback.drawObject(null, object(501, 2L << 16)));
	}
}
