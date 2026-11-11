package com.scenecustomizer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.ObjectComposition;

/** Object names by id, cached. Client thread only. */
final class ObjectNames
{
	private final Client client;
	private final Map<Integer, String> cache = new HashMap<>();
	private final Set<Integer> withImpostors = new HashSet<>();

	ObjectNames(Client client)
	{
		this.client = client;
	}

	/** The cleaned name of the object, the impostor's when it has one; null when nameless or unknown. */
	String nameOf(int id)
	{
		if (cache.containsKey(id))
		{
			return cache.get(id);
		}
		String name = null;
		final ObjectComposition def = client.getObjectDefinition(id);
		if (def != null)
		{
			ObjectComposition used = def;
			if (def.getImpostorIds() != null)
			{
				withImpostors.add(id);
				final ObjectComposition impostor = def.getImpostor();
				if (impostor != null)
				{
					used = impostor;
				}
			}
			name = Names.clean(used.getName());
		}
		cache.put(id, name);
		return name;
	}

	/** Forgets the cached names of objects that have impostors, since a varbit can change them; called before a scan. */
	void refreshImpostors()
	{
		for (Integer id : withImpostors)
		{
			cache.remove(id);
		}
		withImpostors.clear();
	}

	/** Empties the cache; called at every LOADING. */
	void clear()
	{
		cache.clear();
		withImpostors.clear();
	}
}
