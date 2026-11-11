package com.scenecustomizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/** {@link SessionList}: five of each kind, newest first. */
public class SessionListTest
{
	private static List<String> titles(List<Session> sessions)
	{
		final List<String> titles = new ArrayList<>();
		for (Session s : sessions)
		{
			titles.add(s.view().title());
		}
		return titles;
	}

	@Test
	public void keepsFiveOfEachKind()
	{
		final SessionList list = new SessionList();
		for (int i = 1; i <= 6; i++)
		{
			list.add(new Session(Session.SessionKind.RECORDING, "R" + i, i));
		}
		list.add(new Session(Session.SessionKind.LOOKUP, "L1", 7));
		list.add(new Session(Session.SessionKind.LOOKUP, "L2", 8));
		assertEquals(Arrays.asList("R6", "R5", "R4", "R3", "R2"), titles(list.recordings()));
		assertEquals(Arrays.asList("L2", "L1"), titles(list.lookups()));
		assertEquals(Arrays.asList("R6", "R5", "R4", "R3", "R2", "L2", "L1"), titles(list.all()));
	}
}
