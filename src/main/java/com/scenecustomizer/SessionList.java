package com.scenecustomizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The sessions kept in memory: the newest few recordings and the newest few look-ups. */
final class SessionList
{
	private final List<Session> recordings = new ArrayList<>();
	private final List<Session> lookups = new ArrayList<>();

	/** Adds a session as the newest of its kind; the oldest of that kind beyond the cap is dropped. */
	void add(Session session)
	{
		final boolean recording = session.kind() == Session.SessionKind.RECORDING;
		final List<Session> list = recording ? recordings : lookups;
		list.add(0, session);
		final int kept = recording ? Caps.RECORDINGS_KEPT : Caps.LOOKUPS_KEPT;
		while (list.size() > kept)
		{
			list.remove(list.size() - 1);
		}
	}

	/** Recordings, newest first. */
	List<Session> recordings()
	{
		return Collections.unmodifiableList(new ArrayList<>(recordings));
	}

	/** Look-ups, newest first. */
	List<Session> lookups()
	{
		return Collections.unmodifiableList(new ArrayList<>(lookups));
	}

	/** Recordings newest first, then look-ups newest first. */
	List<Session> all()
	{
		final List<Session> all = new ArrayList<>(recordings);
		all.addAll(lookups);
		return Collections.unmodifiableList(all);
	}
}
