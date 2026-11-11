package com.scenecustomizer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** {@link ObjectRules}: what the object rules say about one object. */
public class ObjectRulesTest
{
	private static final List<Place> NO_PLACES = Collections.<Place>emptyList();
	private static final Place HERE = new Place(3000, 3000, 0);

	private static RuleLookup lookup(Rule rule)
	{
		return new RuleLookup(Arrays.asList(rule));
	}

	private static Rule object(String name, Outline outline, List<Place> outlinePlaces, HideScope hide,
		List<Place> hidePlaces)
	{
		return new Rule(Kind.OBJECT, name, outline, outlinePlaces, hide, hidePlaces);
	}

	@Test
	public void everywhereHidesById()
	{
		final RuleLookup rules = lookup(object("Pillar", null, NO_PLACES, HideScope.EVERYWHERE, NO_PLACES));
		assertTrue(ObjectRules.decide(32013, 5L, "Pillar", HERE, rules).hideId);
		assertTrue(ObjectRules.decide(32013, 5L, "Rocks", HERE, rules).isEmpty());
	}

	@Test
	public void hereHidesByHashAtItsPlacesOnly()
	{
		final RuleLookup rules = lookup(object("Pillar", null, NO_PLACES, HideScope.HERE, Arrays.asList(HERE)));
		assertTrue(ObjectRules.decide(32013, 5L, "Pillar", HERE, rules).hideHash);
		assertTrue(ObjectRules.decide(32013, 5L, "Pillar", new Place(3001, 3000, 0), rules).isEmpty());
		assertTrue(ObjectRules.decide(32013, 5L, "Pillar", null, rules).isEmpty());
	}

	@Test
	public void anOutlineOnlyAtItsPlaces()
	{
		final Outline outline = new Outline(0x112233, 4);
		final RuleLookup rules = lookup(object("Pillar", outline, Arrays.asList(HERE), HideScope.OFF, NO_PLACES));
		assertEquals(outline, ObjectRules.decide(32013, 5L, "Pillar", HERE, rules).outline);
		assertNull(ObjectRules.decide(32013, 5L, "Pillar", new Place(3001, 3000, 0), rules).outline);
	}

	@Test
	public void blockedIdsGetNothing()
	{
		final RuleLookup rules = lookup(object("Pillar", new Outline(0x112233, 4), Arrays.asList(HERE),
			HideScope.EVERYWHERE, NO_PLACES));
		assertTrue(ObjectRules.decide(ObjectID.TOB_BLOAT_PILLAR, 5L, "Pillar", HERE, rules).isEmpty());
	}

	@Test
	public void npcRulesNeverTouchObjects()
	{
		final Rule npc = new Rule(Kind.NPC, "Pillar", new Outline(1, 1), NO_PLACES, HideScope.EVERYWHERE, NO_PLACES);
		final ObjectDecision decision = ObjectRules.decide(32013, 5L, "Pillar", HERE, lookup(npc));
		assertTrue(decision.isEmpty());
		assertFalse(decision.hideId);
	}
}
