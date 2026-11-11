package com.scenecustomizer;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** One row of a session, changed in place by sightings. Used from one thread only. */
final class Thing
{
	/** The row's key. */
	final ThingKey key;
	/** Where the row came from. */
	Source source = Source.RECORDED;
	/** The ids kept, sorted, at most {@link Caps#IDS}. */
	final TreeSet<Integer> ids = new TreeSet<>();
	/** Ids turned away after the cap, so a later sighting of one is not counted again. */
	final Set<Integer> extraIds = new HashSet<>();
	/** How many distinct ids did not fit. */
	int moreIds;
	/** Times seen. */
	int seen;
	/** First seen, ms. */
	long firstSeenMs;
	/** Last seen, ms. */
	long lastSeenMs;
	/** The picture, or null. */
	BufferedImage picture;
	/** Places, distinct, first kept. */
	final LinkedHashSet<Place> places = new LinkedHashSet<>();
	/** Hint names, distinct, first kept. */
	final LinkedHashSet<String> hints = new LinkedHashSet<>();
	/** An NPC's own right-click options. */
	List<String> actions = new ArrayList<>();
	/** Times a picture was tried for and missed. */
	int pictureTries;
	/** Set once any id it was seen with, kept or past the id cap, is on the block list. */
	boolean blocked;

	Thing(ThingKey key, long atMs)
	{
		this.key = key;
		this.firstSeenMs = atMs;
		this.lastSeenMs = atMs;
	}

	/** Adds an id: kept up to the cap, counted in {@code moreIds} after it. */
	void addId(int id)
	{
		if (BlockList.isBlocked(id))
		{
			blocked = true;
		}
		if (ids.contains(id) || extraIds.contains(id))
		{
			return;
		}
		if (ids.size() < Caps.IDS)
		{
			ids.add(id);
		}
		else
		{
			extraIds.add(id);
			moreIds++;
		}
	}

	/** Adds a place up to the cap. */
	void addPlace(Place place)
	{
		if (place != null && places.size() < Caps.PLACES)
		{
			places.add(place);
		}
	}

	/** Adds a hint up to the cap. */
	void addHint(String hint)
	{
		if (hint != null && hints.size() < Caps.HINTS)
		{
			hints.add(hint);
		}
	}
}
