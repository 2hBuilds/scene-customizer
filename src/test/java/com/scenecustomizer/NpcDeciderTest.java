package com.scenecustomizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** {@link NpcDecider}: NPC rules by exact name, read from the composition each time. */
public class NpcDeciderTest
{
	private static NPC npc(String name)
	{
		final NPC npc = mock(NPC.class);
		final NPCComposition comp = mock(NPCComposition.class);
		when(comp.getName()).thenReturn(name);
		when(npc.getTransformedComposition()).thenReturn(comp);
		return npc;
	}

	private static Rule rule(String name, Outline outline, HideScope hide)
	{
		return new Rule(Kind.NPC, name, outline, Collections.<Place>emptyList(), hide, Collections.<Place>emptyList());
	}

	private static Set<NPC> identitySet()
	{
		return Collections.newSetFromMap(new IdentityHashMap<NPC, Boolean>());
	}

	@Test
	public void rulesByExactName()
	{
		final Outline green = new Outline(0x00C85A, 3);
		final RuleLookup rules = new RuleLookup(Arrays.asList(rule("Zombified Spawn", green, HideScope.EVERYWHERE)));
		final NPC spawn = npc("Zombified Spawn");
		final List<NPC> all = new ArrayList<>(Arrays.asList(spawn, npc("Vorkath"), npc("zombified spawn"), npc("null")));
		all.add(mock(NPC.class));
		final Set<NPC> hidden = identitySet();
		final Map<NPC, Outline> outlined = new IdentityHashMap<>();
		NpcDecider.decide(all, rules, hidden, outlined);
		assertEquals(1, hidden.size());
		assertTrue(hidden.contains(spawn));
		assertEquals(1, outlined.size());
		assertEquals(green, outlined.get(spawn));
	}

	@Test
	public void aFormChangeIsFollowed()
	{
		final NPC npc = mock(NPC.class);
		final NPCComposition a = mock(NPCComposition.class);
		final NPCComposition b = mock(NPCComposition.class);
		when(a.getName()).thenReturn("A");
		when(b.getName()).thenReturn("B");
		when(npc.getTransformedComposition()).thenReturn(a, b);
		final RuleLookup rules = new RuleLookup(Arrays.asList(rule("B", null, HideScope.EVERYWHERE)));
		final Set<NPC> first = identitySet();
		NpcDecider.decide(Collections.singletonList(npc), rules, first, new IdentityHashMap<NPC, Outline>());
		assertFalse(first.contains(npc));
		final Set<NPC> second = identitySet();
		NpcDecider.decide(Collections.singletonList(npc), rules, second, new IdentityHashMap<NPC, Outline>());
		assertTrue(second.contains(npc));
	}
}
