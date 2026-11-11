package com.scenecustomizer;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A frozen copy of a {@link Thing}, safe to hand to another thread. */
final class ThingView
{
	private final ThingKey key;
	private final List<Integer> ids;
	private final int moreIds;
	private final int seen;
	private final long firstSeenMs;
	private final long lastSeenMs;
	private final BufferedImage picture;
	private final Source source;
	private final List<Place> places;
	private final List<String> hints;
	private final List<String> actions;
	private final boolean blocked;

	ThingView(Thing t)
	{
		key = t.key;
		blocked = t.blocked;
		ids = Collections.unmodifiableList(new ArrayList<>(t.ids));
		moreIds = t.moreIds;
		seen = t.seen;
		firstSeenMs = t.firstSeenMs;
		lastSeenMs = t.lastSeenMs;
		picture = t.picture;
		source = t.source;
		places = Collections.unmodifiableList(new ArrayList<>(t.places));
		hints = Collections.unmodifiableList(new ArrayList<>(t.hints));
		actions = Collections.unmodifiableList(new ArrayList<>(t.actions));
	}

	/** The row's key. */
	ThingKey key()
	{
		return key;
	}

	/** The kind. */
	Kind kind()
	{
		return key.kind;
	}

	/** The section. */
	Section section()
	{
		return key.section;
	}

	/**
	 * The words shown for the row: the name; for a nameless NPC or object {@code Unnamed NPC 100} or
	 * {@code Unnamed object 100}; for an effect or animation {@code Projectile 1477}, {@code Graphic 1483},
	 * {@code Animation 7952}.
	 */
	String label()
	{
		if (key.name != null)
		{
			return key.name;
		}
		switch (key.kind)
		{
			case NPC:
				return "Unnamed NPC " + key.id;
			case OBJECT:
				return "Unnamed object " + key.id;
			case EFFECT:
				return (key.effectKind == EffectKind.PROJECTILE ? "Projectile " : "Graphic ") + key.id;
			default:
				return "Animation " + key.id;
		}
	}

	/** The game's name, or null when it has none. */
	String name()
	{
		return key.name;
	}

	/** The ids, sorted, at most 32. */
	List<Integer> ids()
	{
		return ids;
	}

	/** How many more distinct ids were seen than are listed. */
	int moreIds()
	{
		return moreIds;
	}

	/** Times seen. */
	int seen()
	{
		return seen;
	}

	/** First seen, ms. */
	long firstSeenMs()
	{
		return firstSeenMs;
	}

	/** Last seen, ms. */
	long lastSeenMs()
	{
		return lastSeenMs;
	}

	/** The picture, or null. */
	BufferedImage picture()
	{
		return picture;
	}

	/** Where the row came from. */
	Source source()
	{
		return source;
	}

	/** Places, at most 16. */
	List<Place> places()
	{
		return places;
	}

	/** Hint names, at most 3. */
	List<String> hints()
	{
		return hints;
	}

	/** An NPC's own right-click options. */
	List<String> actions()
	{
		return actions;
	}

	/** True when the row has no name. */
	boolean nameless()
	{
		return key.name == null;
	}

	/** True when any id it was seen with is on the block list, the ones past the id cap included. */
	boolean blocked()
	{
		return blocked;
	}

	/** True for an NPC or object with a name and no blocked id. */
	boolean changeable()
	{
		return (key.kind == Kind.NPC || key.kind == Kind.OBJECT) && key.name != null && !blocked();
	}
}
