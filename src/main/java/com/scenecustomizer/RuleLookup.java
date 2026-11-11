package com.scenecustomizer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The rules by kind and exact name, built once from a list of rules and never changed. */
final class RuleLookup
{
	private final Map<String, Rule> npcs = new HashMap<>();
	private final Map<String, Rule> objects = new HashMap<>();

	/** Choice: of two rules with the same kind and name the first wins (the store holds one of each). */
	RuleLookup(List<Rule> rules)
	{
		for (Rule rule : rules)
		{
			final Map<String, Rule> into = rule.kind == Kind.NPC ? npcs : objects;
			if (!into.containsKey(rule.name))
			{
				into.put(rule.name, rule);
			}
		}
	}

	/** The NPC rule of that exact name, or null. */
	Rule npc(String name)
	{
		return npcs.get(name);
	}

	/** The object rule of that exact name, or null. */
	Rule object(String name)
	{
		return objects.get(name);
	}

	/** True when any NPC rule exists. */
	boolean hasNpcRules()
	{
		return !npcs.isEmpty();
	}

	/** True when any object rule exists. */
	boolean hasObjectRules()
	{
		return !objects.isEmpty();
	}

	/** The names of the object rules. */
	Set<String> objectNames()
	{
		return Collections.unmodifiableSet(objects.keySet());
	}
}
