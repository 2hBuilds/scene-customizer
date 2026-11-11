package com.scenecustomizer;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Things of one session, with the caps. Used from one thread only. Choice: a removed Thing leaves the table at
 * once and no longer counts toward its section's cap.
 */
final class ThingTable
{
	private static final int TURNED_AWAY_KEPT = 5000;

	private final Map<ThingKey, Thing> things = new LinkedHashMap<>();
	private final Map<Section, Integer> named = new EnumMap<>(Section.class);
	private final Map<Section, Integer> nameless = new EnumMap<>(Section.class);
	private final Map<Section, Integer> dropped = new EnumMap<>(Section.class);
	private final Map<Section, Set<ThingKey>> turnedAway = new EnumMap<>(Section.class);
	private int picturesHeld;

	/** Sees an NPC; returns its key, or null when a cap dropped it. */
	ThingKey npcSeen(String rawName, int id, long atMs)
	{
		return sighting(Kind.NPC, Section.NPCS, null, Names.clean(rawName), id, atMs, null);
	}

	/** Sees an object spawn at a place; returns its key, or null when a cap dropped it. */
	ThingKey objectSpawned(String rawName, int id, Place place, long atMs)
	{
		return sighting(Kind.OBJECT, Section.OBJECTS, null, Names.clean(rawName), id, atMs, place);
	}

	/** Finds a named object nearby; a nameless one returns null and adds nothing. */
	ThingKey sceneryFound(String rawName, int id, Place place, long atMs)
	{
		final String name = Names.clean(rawName);
		if (name == null)
		{
			return null;
		}
		return sighting(Kind.OBJECT, Section.SCENERY, null, name, id, atMs, place);
	}

	/** Sees a projectile. */
	ThingKey projectileSeen(int id, long atMs)
	{
		return effect(EffectKind.PROJECTILE, id, null, atMs);
	}

	/** Sees a graphic, perhaps on an NPC. */
	ThingKey graphicSeen(int id, String onRawName, long atMs)
	{
		return effect(EffectKind.GRAPHIC, id, onRawName, atMs);
	}

	/** Sees an animation, perhaps on an NPC. */
	ThingKey animationSeen(int id, String onRawName, long atMs)
	{
		final ThingKey key = sighting(Kind.ANIMATION, Section.ANIMATIONS, null, null, id, atMs, null);
		addHint(key, Names.clean(onRawName));
		return key;
	}

	/** The player casts a spell; a nameless target adds nothing. */
	ThingKey spellCast(String rawTarget, long atMs)
	{
		final String name = Names.clean(rawTarget);
		if (name == null)
		{
			return null;
		}
		return sighting(Kind.SPELL, Section.SPELLS, null, name, -1, atMs, null);
	}

	/** A monster read from the wiki, merged by name; seen stays 0. */
	ThingKey wikiMonster(String name, Collection<Integer> ids, Section section)
	{
		final String clean = Names.clean(name);
		if (clean == null)
		{
			return null;
		}
		final ThingKey key = ThingKey.named(Kind.NPC, section, clean);
		Thing thing = things.get(key);
		if (thing == null)
		{
			thing = add(key, 0L);
			if (thing == null)
			{
				return null;
			}
			thing.source = Source.WIKI;
		}
		for (int id : ids)
		{
			thing.addId(id);
		}
		return key;
	}

	/** Takes the Thing out of every view. */
	void remove(ThingKey key)
	{
		final Thing thing = things.remove(key);
		if (thing == null)
		{
			return;
		}
		count(isNameless(key) ? nameless : named, key.section, -1);
		if (thing.picture != null)
		{
			picturesHeld--;
		}
	}

	/** Moves the last-seen time of a Thing already in the table; an unknown key does nothing, and seen is unchanged. */
	void lastSeen(ThingKey key, long atMs)
	{
		final Thing thing = things.get(key);
		if (thing != null)
		{
			thing.lastSeenMs = atMs;
		}
	}

	/** Sets an NPC's own options; nulls and blanks are dropped, the order kept. */
	void setActions(ThingKey key, List<String> actions)
	{
		final Thing thing = things.get(key);
		if (thing == null)
		{
			return;
		}
		final List<String> kept = new ArrayList<>();
		for (String action : actions)
		{
			if (action != null && !action.trim().isEmpty())
			{
				kept.add(action);
			}
		}
		thing.actions = kept;
	}

	/** NPC and object Things with no picture and tries left, in first-seen order; empty at the picture cap. */
	List<ThingKey> needingPictures()
	{
		final List<ThingKey> wanted = new ArrayList<>();
		if (picturesHeld >= Caps.PICTURES)
		{
			return wanted;
		}
		for (Thing t : things.values())
		{
			if ((t.key.kind == Kind.NPC || t.key.kind == Kind.OBJECT) && t.picture == null
				&& t.pictureTries < Caps.PICTURE_TRIES)
			{
				wanted.add(t.key);
			}
		}
		return wanted;
	}

	/** Stores a picture for the Thing, if it has none and the cap allows. */
	void pictureTaken(ThingKey key, BufferedImage picture)
	{
		final Thing thing = things.get(key);
		if (thing != null && thing.picture == null && picturesHeld < Caps.PICTURES)
		{
			thing.picture = picture;
			picturesHeld++;
		}
	}

	/** Counts one failed try for a picture. */
	void pictureMissed(ThingKey key)
	{
		final Thing thing = things.get(key);
		if (thing != null)
		{
			thing.pictureTries++;
		}
	}

	/** How many Things of the section a cap turned away. */
	int dropped(Section section)
	{
		final Integer n = dropped.get(section);
		return n == null ? 0 : n;
	}

	/** How many Things are in the table. */
	int size()
	{
		return things.size();
	}

	/** Frozen copies by section, in {@link Section} order, only sections with Things, each in first-seen order. */
	Map<Section, List<ThingView>> sectionViews()
	{
		final Map<Section, List<ThingView>> bySection = new EnumMap<>(Section.class);
		for (Thing t : things.values())
		{
			List<ThingView> list = bySection.get(t.key.section);
			if (list == null)
			{
				list = new ArrayList<>();
				bySection.put(t.key.section, list);
			}
			list.add(new ThingView(t));
		}
		final Map<Section, List<ThingView>> ordered = new LinkedHashMap<>();
		for (Section s : Section.values())
		{
			final List<ThingView> list = bySection.get(s);
			if (list != null)
			{
				ordered.put(s, Collections.unmodifiableList(list));
			}
		}
		return Collections.unmodifiableMap(ordered);
	}

	private ThingKey effect(EffectKind effectKind, int id, String onRawName, long atMs)
	{
		final ThingKey key = sighting(Kind.EFFECT, Section.EFFECTS, effectKind, null, id, atMs, null);
		addHint(key, Names.clean(onRawName));
		return key;
	}

	private void addHint(ThingKey key, String hint)
	{
		if (key != null && hint != null)
		{
			things.get(key).addHint(hint);
		}
	}

	private ThingKey sighting(Kind kind, Section section, EffectKind effectKind, String name, int id, long atMs,
		Place place)
	{
		final ThingKey key = name != null ? ThingKey.named(kind, section, name)
			: ThingKey.byId(kind, section, effectKind, id);
		Thing thing = things.get(key);
		if (thing == null)
		{
			thing = add(key, atMs);
			if (thing == null)
			{
				return null;
			}
		}
		thing.seen++;
		thing.lastSeenMs = atMs;
		if (key.name == null || kind == Kind.NPC || kind == Kind.OBJECT)
		{
			if (id >= 0)
			{
				thing.addId(id);
			}
		}
		thing.addPlace(place);
		return key;
	}

	private Thing add(ThingKey key, long atMs)
	{
		final boolean nameLess = isNameless(key);
		final Map<Section, Integer> counts = nameLess ? nameless : named;
		final Integer have = counts.get(key.section);
		if ((have == null ? 0 : have) >= limit(key.section, nameLess))
		{
			turnAway(key);
			return null;
		}
		count(counts, key.section, 1);
		final Thing thing = new Thing(key, atMs);
		things.put(key, thing);
		return thing;
	}

	/** Counts a turned-away key once; a section remembers at most 5,000 keys, then stops counting. */
	private void turnAway(ThingKey key)
	{
		Set<ThingKey> keys = turnedAway.get(key.section);
		if (keys == null)
		{
			keys = new HashSet<>();
			turnedAway.put(key.section, keys);
		}
		if (keys.size() < TURNED_AWAY_KEPT && keys.add(key))
		{
			count(dropped, key.section, 1);
		}
	}

	private static boolean isNameless(ThingKey key)
	{
		return key.name == null && (key.kind == Kind.NPC || key.kind == Kind.OBJECT);
	}

	private static void count(Map<Section, Integer> counts, Section section, int by)
	{
		final Integer have = counts.get(section);
		counts.put(section, (have == null ? 0 : have) + by);
	}

	private static int limit(Section section, boolean nameLess)
	{
		switch (section)
		{
			case NPCS:
				return nameLess ? Caps.NPC_NAMELESS : Caps.NPC_NAMED;
			case OBJECTS:
				return nameLess ? Caps.OBJECT_NAMELESS : Caps.OBJECT_NAMED;
			case SCENERY:
				return Caps.SCENERY;
			case EFFECTS:
				return Caps.EFFECTS;
			case ANIMATIONS:
				return Caps.ANIMATIONS;
			case SPELLS:
				return Caps.SPELLS;
			case THIS_MONSTER:
				return Caps.THIS_MONSTER;
			default:
				return Caps.LINKED;
		}
	}
}
