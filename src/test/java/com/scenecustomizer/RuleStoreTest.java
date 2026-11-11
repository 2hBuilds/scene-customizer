package com.scenecustomizer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** {@link RuleStore}, {@link Rule} and {@link Outline}. */
public class RuleStoreTest
{
	private final Gson gson = new Gson();
	private static final List<Place> NONE = Collections.<Place>emptyList();

	private static Rule npcOutline(String name)
	{
		return new Rule(Kind.NPC, name, new Outline(0x00C85A, 3), NONE, HideScope.OFF, NONE);
	}

	@Test
	public void putGetRemove()
	{
		final RuleStore s = new RuleStore(gson);
		s.put(npcOutline("Zombified Spawn"));
		s.put(new Rule(Kind.OBJECT, "Pillar", null, NONE, HideScope.EVERYWHERE, NONE));
		assertEquals("Pillar", s.all().get(0).name);
		assertEquals("Zombified Spawn", s.all().get(1).name);
		assertEquals(3, s.get(Kind.NPC, "Zombified Spawn").outline.width);
		s.remove(Kind.NPC, "Zombified Spawn");
		assertNull(s.get(Kind.NPC, "Zombified Spawn"));
	}

	@Test
	public void aRuleThatDoesNothingIsDeleted()
	{
		final RuleStore s = new RuleStore(gson);
		s.put(new Rule(Kind.NPC, "Goblin", null, NONE, HideScope.OFF, NONE));
		assertTrue(s.all().isEmpty());
		s.put(npcOutline("Goblin"));
		s.put(new Rule(Kind.NPC, "Goblin", null, NONE, HideScope.OFF, NONE));
		assertNull(s.get(Kind.NPC, "Goblin"));
		assertTrue(s.all().isEmpty());
	}

	@Test
	public void jsonRoundTrip()
	{
		final RuleStore s = new RuleStore(gson);
		s.put(new Rule(Kind.OBJECT, "Pillar", new Outline(0xFF0000, 2),
			Arrays.asList(new Place(3000, 3000, 0), new Place(3001, 3000, 0)), HideScope.HERE,
			Arrays.asList(new Place(3000, 3000, 0))));
		s.put(npcOutline("Zombified Spawn"));
		final RuleStore back = RuleStore.fromJson(gson, s.toJson());
		assertEquals(s.all(), back.all());
		assertEquals(2, back.all().size());
	}

	@Test
	public void badJsonGivesAnEmptyStore()
	{
		for (String bad : new String[] {null, "", "[]", "not json", "{\"a\":1}"})
		{
			assertTrue(RuleStore.fromJson(gson, bad).all().isEmpty());
		}
	}

	@Test
	public void capsOnRulesAndPlaces()
	{
		final RuleStore s = new RuleStore(gson);
		for (int i = 0; i < 501; i++)
		{
			s.put(npcOutline("N" + i));
		}
		assertEquals(500, s.all().size());
		assertNull(s.get(Kind.NPC, "N0"));
		assertEquals("N500", s.all().get(0).name);
		final List<Place> twenty = new ArrayList<>();
		for (int i = 0; i < 20; i++)
		{
			twenty.add(new Place(i, 0, 0));
		}
		final Rule r = new Rule(Kind.OBJECT, "Rocks", null, twenty, HideScope.HERE, twenty);
		assertEquals(16, r.hidePlaces.size());
		assertEquals(16, r.outlinePlaces.size());
		assertEquals(new Place(15, 0, 0), r.hidePlaces.get(15));
	}

	@Test
	public void widthIsClampedAndNpcRulesHaveNoPlaces()
	{
		assertEquals(1, new Outline(0x123456, 0).width);
		assertEquals(6, new Outline(0x123456, 9).width);
		final List<Place> places = Arrays.asList(new Place(1, 2, 0));
		final Rule npc = new Rule(Kind.NPC, "Goblin", new Outline(1, 1), places, HideScope.HERE, places);
		assertTrue(npc.outlinePlaces.isEmpty());
		assertTrue(npc.hidePlaces.isEmpty());
		try
		{
			new Rule(Kind.EFFECT, "x", new Outline(1, 1), NONE, HideScope.OFF, NONE);
			fail("an effect has no rule");
		}
		catch (IllegalArgumentException expected)
		{
			assertTrue(true);
		}
	}

	@Test
	public void namesMatchExactly()
	{
		final RuleStore s = new RuleStore(gson);
		s.put(npcOutline("Zombified Spawn"));
		assertNull(s.get(Kind.NPC, "zombified spawn"));
		assertNull(s.get(Kind.OBJECT, "Zombified Spawn"));
	}

	@Test
	public void theJsonHasExactlyTheAgreedKeys()
	{
		final RuleStore s = new RuleStore(gson);
		s.put(new Rule(Kind.OBJECT, "Pillar", new Outline(0xFF0000, 2), Arrays.asList(new Place(1, 2, 0)),
			HideScope.OFF, NONE));
		final JsonElement root = new JsonParser().parse(s.toJson());
		final JsonArray list = root.getAsJsonArray();
		assertEquals(1, list.size());
		final JsonObject o = list.get(0).getAsJsonObject();
		assertEquals(new HashSet<>(Arrays.asList("kind", "name", "outline", "outlinePlaces", "hide", "hidePlaces")),
			o.keySet());
		assertEquals(new HashSet<>(Arrays.asList("rgb", "width")), o.getAsJsonObject("outline").keySet());
	}

	@Test
	public void anObjectRuleWithoutPlacesDoesNothing()
	{
		final Rule outlined = new Rule(Kind.OBJECT, "Tree", new Outline(0x112233, 2), NONE, HideScope.OFF, NONE);
		assertNull(outlined.outline);
		assertTrue(outlined.doesNothing());
		final Rule hidden = new Rule(Kind.OBJECT, "Tree", null, NONE, HideScope.HERE, NONE);
		assertEquals(HideScope.OFF, hidden.hide);
		assertTrue(hidden.doesNothing());
		final RuleStore s = new RuleStore(gson);
		s.put(outlined);
		s.put(hidden);
		assertEquals(0, s.all().size());
		final Rule npc = new Rule(Kind.NPC, "Goblin", new Outline(1, 1), NONE, HideScope.OFF, NONE);
		assertEquals(new Outline(1, 1), npc.outline);
	}
}
