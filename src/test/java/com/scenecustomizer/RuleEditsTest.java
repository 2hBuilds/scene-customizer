package com.scenecustomizer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** {@link RuleEdits}: what each menu press does to a rule. */
public class RuleEditsTest
{
	private static final List<Place> NONE = Collections.<Place>emptyList();
	private static final List<Place> TWO = Arrays.asList(new Place(3000, 3000, 0), new Place(3001, 3000, 0));

	private static ThingView npc()
	{
		final ThingTable table = new ThingTable();
		table.npcSeen("Zombified Spawn", 8062, 0L);
		return table.sectionViews().get(Section.NPCS).get(0);
	}

	private static ThingView pillar()
	{
		final ThingTable table = new ThingTable();
		table.objectSpawned("Pillar", 1234, TWO.get(0), 0L);
		table.objectSpawned("Pillar", 1234, TWO.get(1), 1L);
		return table.sectionViews().get(Section.OBJECTS).get(0);
	}

	@Test
	public void outlineOnAndOff()
	{
		final Rule on = RuleEdits.apply(npc(), null, RowAction.OUTLINE, 0);
		assertEquals(Kind.NPC, on.kind);
		assertEquals("Zombified Spawn", on.name);
		assertEquals(new Outline(0x00D6D6, 2), on.outline);
		assertEquals(HideScope.OFF, on.hide);
		assertTrue(RuleEdits.apply(npc(), on, RowAction.OUTLINE, 0).doesNothing());
	}

	@Test
	public void colourAndWidthMakeAnOutline()
	{
		final Rule width = RuleEdits.apply(npc(), null, RowAction.WIDTH, 3);
		assertEquals(new Outline(0x00D6D6, 3), width.outline);
		final Rule colour = RuleEdits.apply(npc(), width, RowAction.COLOUR, 0x00C85A);
		assertEquals(new Outline(0x00C85A, 3), colour.outline);
	}

	@Test
	public void objectHidesUseThePlaces()
	{
		final Rule here = RuleEdits.apply(pillar(), null, RowAction.HIDE_HERE, 0);
		assertEquals(HideScope.HERE, here.hide);
		assertEquals(TWO, here.hidePlaces);
		final Rule everywhere = RuleEdits.apply(pillar(), here, RowAction.HIDE_EVERYWHERE, 0);
		assertEquals(HideScope.EVERYWHERE, everywhere.hide);
		assertTrue(everywhere.hidePlaces.isEmpty());
		assertTrue(RuleEdits.apply(pillar(), everywhere, RowAction.SHOW, 0).doesNothing());
	}

	@Test
	public void anObjectsOutlineIsAtItsPlaces()
	{
		final Rule rule = RuleEdits.apply(pillar(), null, RowAction.OUTLINE, 0);
		assertEquals(TWO, rule.outlinePlaces);
	}

	@Test
	public void anNpcsHideToggles()
	{
		final Rule hidden = RuleEdits.apply(npc(), null, RowAction.HIDE, 0);
		assertEquals(HideScope.EVERYWHERE, hidden.hide);
		assertTrue(RuleEdits.apply(npc(), hidden, RowAction.HIDE, 0).doesNothing());
	}

	@Test
	public void anEditKeepsTheOtherHalf()
	{
		final Rule outlined = new Rule(Kind.NPC, "Zombified Spawn", new Outline(0x00C85A, 3), NONE, HideScope.OFF, NONE);
		final Rule both = RuleEdits.apply(npc(), outlined, RowAction.HIDE, 0);
		assertEquals(new Outline(0x00C85A, 3), both.outline);
		assertEquals(HideScope.EVERYWHERE, both.hide);
		final Rule hideOnly = RuleEdits.apply(npc(), both, RowAction.OUTLINE, 0);
		assertNull(hideOnly.outline);
		assertEquals(HideScope.EVERYWHERE, hideOnly.hide);
	}
}
