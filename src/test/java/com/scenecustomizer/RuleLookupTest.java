package com.scenecustomizer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** {@link RuleLookup}: rules by kind and exact name. */
public class RuleLookupTest
{
	@Test
	public void byKindAndExactName()
	{
		final List<Place> none = Collections.<Place>emptyList();
		final Rule npc = new Rule(Kind.NPC, "Pillar", new Outline(1, 1), none, HideScope.OFF, none);
		final Rule object = new Rule(Kind.OBJECT, "Pillar", null, none, HideScope.EVERYWHERE, none);
		final RuleLookup lookup = new RuleLookup(Arrays.asList(npc, object));
		assertSame(npc, lookup.npc("Pillar"));
		assertSame(object, lookup.object("Pillar"));
		assertNull(lookup.object("pillar"));
		assertTrue(lookup.hasNpcRules());
		assertTrue(lookup.hasObjectRules());
		final RuleLookup empty = new RuleLookup(Collections.<Rule>emptyList());
		assertFalse(empty.hasNpcRules());
		assertFalse(empty.hasObjectRules());
	}
}
