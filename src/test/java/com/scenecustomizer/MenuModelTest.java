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

/** {@link MenuModel}: the lines of a row's menu. */
public class MenuModelTest
{
	private static final List<Place> NONE = Collections.<Place>emptyList();

	private static ThingView npc(String name, int id)
	{
		final ThingTable table = new ThingTable();
		table.npcSeen(name, id, 0L);
		return table.sectionViews().get(Section.NPCS).get(0);
	}

	private static ThingView pillar(int id)
	{
		final ThingTable table = new ThingTable();
		table.objectSpawned("Pillar", id, new Place(3000, 3000, 0), 0L);
		return table.sectionViews().get(Section.OBJECTS).get(0);
	}

	private static void line(List<MenuModel.MenuLine> lines, int at, MenuModel.Type type, String text, boolean selected,
		RowAction action)
	{
		final MenuModel.MenuLine l = lines.get(at);
		assertEquals(type, l.type);
		assertEquals(text, l.text);
		assertEquals(selected, l.selected);
		assertEquals(action, l.action);
	}

	@Test
	public void aChangeableNpcWithNoRule()
	{
		final List<MenuModel.MenuLine> lines = MenuModel.of(npc("Zombified Spawn", 8062), null);
		assertEquals(9, lines.size());
		line(lines, 0, MenuModel.Type.HEADER, "Zombified Spawn", false, null);
		line(lines, 1, MenuModel.Type.SEPARATOR, "", false, null);
		line(lines, 2, MenuModel.Type.CHECK, "Outline", false, RowAction.OUTLINE);
		line(lines, 3, MenuModel.Type.COLOUR, "Colour", false, RowAction.COLOUR);
		assertEquals(0x00D6D6, lines.get(3).rgb);
		line(lines, 4, MenuModel.Type.WIDTH, "Width: 2", false, RowAction.WIDTH);
		line(lines, 5, MenuModel.Type.SEPARATOR, "", false, null);
		line(lines, 6, MenuModel.Type.CHECK, "Hide", false, RowAction.HIDE);
		line(lines, 7, MenuModel.Type.SEPARATOR, "", false, null);
		line(lines, 8, MenuModel.Type.ITEM, "Copy id", false, RowAction.COPY);
	}

	@Test
	public void aChangeableNpcWithARule()
	{
		final Rule rule = new Rule(Kind.NPC, "Zombified Spawn", new Outline(0x00C85A, 3), NONE, HideScope.EVERYWHERE, NONE);
		final List<MenuModel.MenuLine> lines = MenuModel.of(npc("Zombified Spawn", 8062), rule);
		assertTrue(lines.get(2).selected);
		assertEquals(0x00C85A, lines.get(3).rgb);
		assertEquals("Width: 3", lines.get(4).text);
		assertTrue(lines.get(6).selected);
	}

	@Test
	public void aChangeableObject()
	{
		final Rule rule = new Rule(Kind.OBJECT, "Pillar", null, NONE, HideScope.HERE, Arrays.asList(new Place(3000, 3000, 0)));
		final List<MenuModel.MenuLine> lines = MenuModel.of(pillar(1234), rule);
		assertEquals(11, lines.size());
		line(lines, 0, MenuModel.Type.HEADER, "Pillar", false, null);
		line(lines, 1, MenuModel.Type.SEPARATOR, "", false, null);
		line(lines, 2, MenuModel.Type.CHECK, "Outline here", false, RowAction.OUTLINE);
		line(lines, 3, MenuModel.Type.COLOUR, "Colour", false, RowAction.COLOUR);
		line(lines, 4, MenuModel.Type.WIDTH, "Width: 2", false, RowAction.WIDTH);
		line(lines, 5, MenuModel.Type.SEPARATOR, "", false, null);
		line(lines, 6, MenuModel.Type.RADIO, "Show", false, RowAction.SHOW);
		line(lines, 7, MenuModel.Type.RADIO, "Hide here", true, RowAction.HIDE_HERE);
		line(lines, 8, MenuModel.Type.RADIO, "Hide everywhere", false, RowAction.HIDE_EVERYWHERE);
		line(lines, 9, MenuModel.Type.SEPARATOR, "", false, null);
		line(lines, 10, MenuModel.Type.ITEM, "Copy id", false, RowAction.COPY);
	}

	@Test
	public void unchangeableRowsSayWhy()
	{
		final List<MenuModel.MenuLine> nameless = MenuModel.of(npc(null, 100), null);
		line(nameless, 2, MenuModel.Type.NOTE, "No name, so it can't be changed", false, null);
		assertEquals("Copy id", nameless.get(nameless.size() - 1).text);

		final List<MenuModel.MenuLine> blocked = MenuModel.of(pillar(ObjectID.TOB_BLOAT_PILLAR), null);
		line(blocked, 2, MenuModel.Type.NOTE, "The Plugin Hub doesn't allow changing this", false, null);

		final ThingTable table = new ThingTable();
		table.projectileSeen(1477, 0L);
		final List<MenuModel.MenuLine> projectile = MenuModel.of(table.sectionViews().get(Section.EFFECTS).get(0), null);
		line(projectile, 2, MenuModel.Type.NOTE, "Shown only", false, null);
		assertEquals("Copy id", projectile.get(projectile.size() - 1).text);

		final ThingTable spells = new ThingTable();
		spells.spellCast("Crumble Undead", 0L);
		final List<MenuModel.MenuLine> spell = MenuModel.of(spells.sectionViews().get(Section.SPELLS).get(0), null);
		assertEquals(3, spell.size());
		line(spell, 2, MenuModel.Type.NOTE, "Shown only", false, null);
		for (MenuModel.MenuLine l : spell)
		{
			assertFalse("Copy id".equals(l.text));
		}
	}

	@Test
	public void aWikiRowCanBeRemoved()
	{
		final ThingTable table = new ThingTable();
		table.wikiMonster("Vorkath", Arrays.asList(8058, 8059), Section.THIS_MONSTER);
		final List<MenuModel.MenuLine> lines = MenuModel.of(table.sectionViews().get(Section.THIS_MONSTER).get(0), null);
		final MenuModel.MenuLine last = lines.get(lines.size() - 1);
		assertEquals(MenuModel.Type.ITEM, last.type);
		assertEquals("Remove from this list", last.text);
		assertEquals(RowAction.REMOVE, last.action);
		assertNull(lines.get(0).action);
	}
}
