package com.scenecustomizer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/** {@link Session} and {@link SessionView}. */
public class SessionTest
{
	@Test
	public void aViewIsACopy()
	{
		final Session s = new Session(Session.SessionKind.RECORDING, "Recording 21:14", 1000L);
		s.table().npcSeen("Vorkath", 8059, 0);
		final SessionView view = s.view();
		s.table().npcSeen("Vorkath", 8059, 5);
		assertEquals(1, view.sections().get(Section.NPCS).get(0).seen());
		assertEquals(2, s.view().sections().get(Section.NPCS).get(0).seen());
		try
		{
			view.sections().get(Section.NPCS).add(null);
			org.junit.Assert.fail("the list should not change");
		}
		catch (UnsupportedOperationException expected)
		{
			assertEquals(1, view.thingCount());
		}
		try
		{
			view.sections().get(Section.NPCS).get(0).ids().add(1);
			org.junit.Assert.fail("the ids should not change");
		}
		catch (UnsupportedOperationException expected)
		{
			assertEquals(1, view.sections().get(Section.NPCS).get(0).ids().size());
		}
	}

	@Test
	public void statesAndLength()
	{
		final Session r = new Session(Session.SessionKind.RECORDING, "Recording", 0L);
		assertEquals(Session.SessionState.RECORDING, r.view().state());
		r.stop(240000L);
		assertEquals(Session.SessionState.DONE, r.view().state());
		assertEquals(240000L, r.view().lengthMs());
		final Session l = new Session(Session.SessionKind.LOOKUP, "Vorkath", 0L);
		assertEquals(Session.SessionState.LOADING, l.view().state());
		l.fail();
		assertEquals(Session.SessionState.FAILED, l.view().state());
	}
}
