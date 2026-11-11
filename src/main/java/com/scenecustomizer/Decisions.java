package com.scenecustomizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.NPC;

/**
 * What the engine decided: which NPCs and objects are hidden and which are outlined. The engine builds a part fresh
 * on the client thread and publishes it whole through a {@code volatile} field; a published set is never changed, so
 * any thread may read it, the render thread above all. Choice: every part is copied on the way in, so the caller may
 * go on using what it passed; every part is empty at first.
 */
final class Decisions
{
	private volatile Set<NPC> hiddenNpcs = Collections.emptySet();
	private volatile Map<NPC, Outline> outlinedNpcs = Collections.emptyMap();
	private volatile Set<Integer> hiddenObjectIds = Collections.emptySet();
	private volatile Set<Long> hiddenObjectHashes = Collections.emptySet();
	private volatile List<OutlinedObject> outlinedObjects = Collections.emptyList();
	private volatile Set<Long> hiddenZones = Collections.emptySet();

	/** True when the renderable is an NPC in the hidden set; a player, anything else or null is not. */
	boolean npcHidden(Object renderable)
	{
		return renderable instanceof NPC && hiddenNpcs.contains(renderable);
	}

	/** True when the object is not on the block list and its id or its hash is hidden. */
	boolean objectHidden(int id, long hash)
	{
		return !BlockList.isBlocked(id) && (hiddenObjectIds.contains(id) || hiddenObjectHashes.contains(hash));
	}

	/** Publishes the NPC part: the hidden NPCs and the outlined ones with their outlines. */
	void setNpcs(Set<NPC> hidden, Map<NPC, Outline> outlined)
	{
		final Set<NPC> hiddenCopy = Collections.newSetFromMap(new IdentityHashMap<NPC, Boolean>());
		hiddenCopy.addAll(hidden);
		final Map<NPC, Outline> outlinedCopy = new IdentityHashMap<>(outlined);
		hiddenNpcs = Collections.unmodifiableSet(hiddenCopy);
		outlinedNpcs = Collections.unmodifiableMap(outlinedCopy);
	}

	/** Publishes the object part. */
	void setObjects(Set<Integer> ids, Set<Long> hashes, List<OutlinedObject> outlined, Set<Long> zones)
	{
		hiddenObjectIds = Collections.unmodifiableSet(new HashSet<>(ids));
		hiddenObjectHashes = Collections.unmodifiableSet(new HashSet<>(hashes));
		outlinedObjects = Collections.unmodifiableList(new ArrayList<>(outlined));
		hiddenZones = Collections.unmodifiableSet(new HashSet<>(zones));
	}

	/** The outlined NPCs; never changed. */
	Map<NPC, Outline> outlinedNpcs()
	{
		return outlinedNpcs;
	}

	/** The hidden object ids; never changed. */
	Set<Integer> hiddenObjectIds()
	{
		return hiddenObjectIds;
	}

	/** The hidden object hashes; never changed. */
	Set<Long> hiddenObjectHashes()
	{
		return hiddenObjectHashes;
	}

	/** The outlined objects, for the client thread only; never changed. */
	List<OutlinedObject> outlinedObjects()
	{
		return outlinedObjects;
	}

	/** The keys of the zones that hold a hidden object; never changed. */
	Set<Long> hiddenZones()
	{
		return hiddenZones;
	}

	/** Publishes every part empty. */
	void clear()
	{
		setNpcs(Collections.<NPC>emptySet(), Collections.<NPC, Outline>emptyMap());
		setObjects(Collections.<Integer>emptySet(), Collections.<Long>emptySet(),
			Collections.<OutlinedObject>emptyList(), Collections.<Long>emptySet());
	}
}
