package com.scenecustomizer;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/** {@link Decisions}: what the render thread reads. */
public class DecisionsTest
{
	@Test
	public void anNpcIsHiddenOnlyWhenInTheSet()
	{
		final NPC a = mock(NPC.class);
		final NPC b = mock(NPC.class);
		final Decisions decisions = new Decisions();
		decisions.setNpcs(Collections.singleton(a), new IdentityHashMap<NPC, Outline>());
		assertTrue(decisions.npcHidden(a));
		assertFalse(decisions.npcHidden(b));
		assertFalse(decisions.npcHidden(mock(Player.class)));
		assertFalse(decisions.npcHidden(null));
	}

	@Test
	public void objectsHideByIdOrHashButNeverBlocked()
	{
		final Decisions decisions = new Decisions();
		final Set<Integer> ids = new HashSet<>(Arrays.asList(500, ObjectID.TOB_BLOAT_PILLAR));
		final Map<NPC, Outline> none = Collections.emptyMap();
		decisions.setNpcs(Collections.<NPC>emptySet(), none);
		decisions.setObjects(ids, Collections.singleton(77L), Collections.<OutlinedObject>emptyList(),
			Collections.<Long>emptySet());
		assertTrue(decisions.objectHidden(500, 1L));
		assertTrue(decisions.objectHidden(1, 77L));
		assertFalse(decisions.objectHidden(1, 1L));
		assertFalse(decisions.objectHidden(ObjectID.TOB_BLOAT_PILLAR, 1L));
	}
}
