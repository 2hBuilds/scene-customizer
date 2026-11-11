package com.scenecustomizer;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** {@link RowFilter}: the filter box. */
public class RowFilterTest
{
	@Test
	public void blankKeepsAllAndAQueryIgnoresCase()
	{
		final Session session = new Session(Session.SessionKind.RECORDING, "Recording 21:14", 0L);
		session.table().npcSeen("Vorkath", 8059, 0L);
		session.table().npcSeen("Zombified Spawn", 8062, 1000L);
		session.table().projectileSeen(1477, 2000L);
		final SessionView view = session.view();

		final Map<Section, List<ThingView>> all = RowFilter.filter(view, "");
		assertEquals(2, all.get(Section.NPCS).size());
		assertEquals(1, all.get(Section.EFFECTS).size());

		final Map<Section, List<ThingView>> spawn = RowFilter.filter(view, "SPAWN");
		assertEquals(1, spawn.size());
		assertEquals("Zombified Spawn", spawn.get(Section.NPCS).get(0).label());
		assertFalse(spawn.containsKey(Section.EFFECTS));

		assertTrue(RowFilter.filter(view, "zz").isEmpty());
	}
}
