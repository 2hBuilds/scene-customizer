package com.scenecustomizer;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** A frozen copy of a {@link Session}: later sightings do not change it. */
final class SessionView
{
	private final Session.SessionKind kind;
	private final String title;
	private final Session.SessionState state;
	private final long startedEpochMs;
	private final long lengthMs;
	private final int thingCount;
	private final Map<Section, List<ThingView>> sections;
	private final Map<Section, Integer> dropped = new EnumMap<>(Section.class);

	SessionView(Session.SessionKind kind, String title, Session.SessionState state, long startedEpochMs,
		long lengthMs, ThingTable table)
	{
		this.kind = kind;
		this.title = title;
		this.state = state;
		this.startedEpochMs = startedEpochMs;
		this.lengthMs = lengthMs;
		this.thingCount = table.size();
		this.sections = table.sectionViews();
		for (Section s : Section.values())
		{
			dropped.put(s, table.dropped(s));
		}
	}

	/** Recording or look-up. */
	Session.SessionKind kind()
	{
		return kind;
	}

	/** The title. */
	String title()
	{
		return title;
	}

	/** The state when the view was taken. */
	Session.SessionState state()
	{
		return state;
	}

	/** Wall-clock start. */
	long startedEpochMs()
	{
		return startedEpochMs;
	}

	/** A recording's length, ms. */
	long lengthMs()
	{
		return lengthMs;
	}

	/** How many Things there were. */
	int thingCount()
	{
		return thingCount;
	}

	/** The Things by section, in section order, only sections with Things. */
	Map<Section, List<ThingView>> sections()
	{
		return Collections.unmodifiableMap(sections);
	}

	/** How many Things a cap turned away from the section. */
	int dropped(Section section)
	{
		return dropped.get(section);
	}
}
