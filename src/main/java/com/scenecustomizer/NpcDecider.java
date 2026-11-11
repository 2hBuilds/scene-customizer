package com.scenecustomizer;

import java.util.Map;
import java.util.Set;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;

/** Works out which NPCs an NPC rule hides or outlines, by the name each carries now. Client thread only. */
final class NpcDecider
{
	private NpcDecider()
	{
	}

	/**
	 * Adds to {@code hidden} every NPC whose rule hides it everywhere and to {@code outlined} every NPC whose rule
	 * has an outline. The name is read from the transformed composition each time, so a form change is followed; an
	 * NPC with no composition or no name is skipped.
	 */
	static void decide(Iterable<? extends NPC> npcs, RuleLookup rules, Set<NPC> hidden, Map<NPC, Outline> outlined)
	{
		for (NPC npc : npcs)
		{
			final NPCComposition comp = npc == null ? null : npc.getTransformedComposition();
			if (comp == null)
			{
				continue;
			}
			final String name = Names.clean(comp.getName());
			final Rule rule = name == null ? null : rules.npc(name);
			if (rule == null)
			{
				continue;
			}
			if (rule.hide == HideScope.EVERYWHERE)
			{
				hidden.add(npc);
			}
			if (rule.outline != null)
			{
				outlined.put(npc, rule.outline);
			}
		}
	}
}
