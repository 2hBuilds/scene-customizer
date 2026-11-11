package com.scenecustomizer;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** {@link ThingTable}: counting, caps, pictures. */
public class ThingTableTest
{
	private static List<ThingView> list(ThingTable t, Section s)
	{
		final List<ThingView> views = t.sectionViews().get(s);
		return views == null ? new ArrayList<ThingView>() : views;
	}

	private static ThingView only(ThingTable t, Section s)
	{
		final List<ThingView> views = list(t, s);
		assertEquals(1, views.size());
		return views.get(0);
	}

	private static List<String> labels(ThingTable t, Section s)
	{
		final List<String> labels = new ArrayList<>();
		for (ThingView v : list(t, s))
		{
			labels.add(v.label());
		}
		return labels;
	}

	@Test
	public void aRespawnCountsOnTheSameRow()
	{
		final ThingTable t = new ThingTable();
		t.npcSeen("Vorkath", 8059, 0);
		t.npcSeen("Vorkath", 8061, 5000);
		t.npcSeen("Vorkath", 8059, 9000);
		final ThingView v = only(t, Section.NPCS);
		assertEquals(Arrays.asList(8059, 8061), v.ids());
		assertEquals(3, v.seen());
		assertEquals(0L, v.firstSeenMs());
		assertEquals(9000L, v.lastSeenMs());
		assertEquals(Section.NPCS, v.section());
		assertTrue(v.changeable());
	}

	@Test
	public void namelessNpcsAreKeyedById()
	{
		final ThingTable t = new ThingTable();
		t.npcSeen("null", 100, 0);
		t.npcSeen(null, 101, 0);
		t.npcSeen("null", 100, 10);
		final List<ThingView> views = list(t, Section.NPCS);
		assertEquals(2, views.size());
		assertEquals("Unnamed NPC 100", views.get(0).label());
		assertEquals(2, views.get(0).seen());
		assertEquals("Unnamed NPC 101", views.get(1).label());
		assertEquals(1, views.get(1).seen());
		for (ThingView v : views)
		{
			assertTrue(v.nameless());
			assertFalse(v.changeable());
		}
	}

	@Test
	public void capsDropAndCount()
	{
		final ThingTable t = new ThingTable();
		for (int i = 0; i < 300; i++)
		{
			assertNotNull(t.npcSeen("N" + i, i, 0));
		}
		assertNull(t.npcSeen("N300", 300, 0));
		assertEquals(1, t.dropped(Section.NPCS));
		for (int i = 0; i < 50; i++)
		{
			assertNotNull(t.npcSeen(null, 1000 + i, 0));
		}
		assertNull(t.npcSeen(null, 2000, 0));
		assertEquals(2, t.dropped(Section.NPCS));
		assertNotNull(t.npcSeen("N0", 0, 1));
		assertEquals(2, t.dropped(Section.NPCS));
		assertEquals(2, list(t, Section.NPCS).get(0).seen());
	}

	@Test
	public void idsArePinnedAt32()
	{
		final ThingTable t = new ThingTable();
		for (int id = 40; id >= 1; id--)
		{
			t.npcSeen("Goblin", id, 0);
		}
		final ThingView v = only(t, Section.NPCS);
		assertEquals(32, v.ids().size());
		assertEquals(Integer.valueOf(9), v.ids().get(0));
		assertEquals(Integer.valueOf(40), v.ids().get(31));
		assertEquals(8, v.moreIds());
	}

	@Test
	public void objectsAndSceneryAreSeparateRows()
	{
		final ThingTable t = new ThingTable();
		t.objectSpawned("Pillar", 32013, new Place(3000, 3000, 0), 0);
		t.sceneryFound("Pillar", 32013, new Place(3001, 3000, 0), 0);
		assertEquals(2, t.size());
		assertEquals(Kind.OBJECT, only(t, Section.OBJECTS).kind());
		assertEquals(Kind.OBJECT, only(t, Section.SCENERY).kind());
	}

	@Test
	public void placesArePinnedAt16()
	{
		final ThingTable t = new ThingTable();
		for (int i = 0; i < 20; i++)
		{
			t.objectSpawned("Rocks", 11, new Place(i, 0, 0), 0);
		}
		t.objectSpawned("Rocks", 11, new Place(0, 0, 0), 0);
		final ThingView v = only(t, Section.OBJECTS);
		assertEquals(16, v.places().size());
		for (int i = 0; i < 16; i++)
		{
			assertEquals(new Place(i, 0, 0), v.places().get(i));
		}
		assertEquals(21, v.seen());
	}

	@Test
	public void namelessSceneryIsLeftOut()
	{
		final ThingTable t = new ThingTable();
		assertNull(t.sceneryFound(null, 5, new Place(1, 1, 0), 0));
		assertNull(t.sceneryFound("null", 5, new Place(1, 1, 0), 0));
		assertEquals(0, t.size());
	}

	@Test
	public void effectsAnimationsAndSpells()
	{
		final ThingTable t = new ThingTable();
		t.projectileSeen(1477, 0);
		t.projectileSeen(1477, 0);
		t.graphicSeen(1483, null, 0);
		t.graphicSeen(1483, "Vorkath", 0);
		t.animationSeen(7952, "Vorkath", 0);
		t.animationSeen(7952, "Vorkath", 0);
		t.spellCast("<col=00ff00>Crumble Undead</col>", 0);
		final List<ThingView> effects = list(t, Section.EFFECTS);
		assertEquals(Arrays.asList("Projectile 1477", "Graphic 1483"), labels(t, Section.EFFECTS));
		assertEquals(2, effects.get(0).seen());
		assertEquals(2, effects.get(1).seen());
		assertEquals(Arrays.asList("Vorkath"), effects.get(1).hints());
		final ThingView anim = only(t, Section.ANIMATIONS);
		assertEquals("Animation 7952", anim.label());
		assertEquals(2, anim.seen());
		assertEquals(Arrays.asList("Vorkath"), anim.hints());
		final ThingView spell = only(t, Section.SPELLS);
		assertEquals("Crumble Undead", spell.label());
		for (ThingView v : effects)
		{
			assertFalse(v.changeable());
		}
		assertFalse(anim.changeable());
		assertFalse(spell.changeable());
	}

	@Test
	public void hintsArePinnedAtThree()
	{
		final ThingTable t = new ThingTable();
		for (String n : new String[] {"A", "B", "C", "D", "A"})
		{
			t.animationSeen(1, n, 0);
		}
		assertEquals(Arrays.asList("A", "B", "C"), only(t, Section.ANIMATIONS).hints());
	}

	@Test
	public void aBlockedIdMakesTheRowUnchangeable()
	{
		final ThingTable t = new ThingTable();
		t.objectSpawned("Pillar", ObjectID.TOB_BLOAT_PILLAR, new Place(1, 1, 0), 0);
		t.objectSpawned("Pillar", 32013, new Place(2, 1, 0), 0);
		final ThingView v = only(t, Section.OBJECTS);
		assertTrue(v.blocked());
		assertFalse(v.changeable());
	}

	/** T33, added by the lead for a bug found in the L1 review: a blocked id past the 32-id cap still blocks. */
	@Test
	public void aBlockedIdPastTheIdCapStillBlocks()
	{
		final ThingTable t = new ThingTable();
		for (int id = 1; id <= Caps.IDS; id++)
		{
			t.objectSpawned("Pillar", id, new Place(id, 1, 0), 0);
		}
		t.objectSpawned("Pillar", ObjectID.TOB_BLOAT_PILLAR, new Place(1, 2, 0), 0);
		final ThingView v = only(t, Section.OBJECTS);
		assertEquals(1, v.moreIds());
		assertTrue(v.blocked());
		assertFalse(v.changeable());
	}

	@Test
	public void sectionsKeepFirstSeenOrder()
	{
		final ThingTable t = new ThingTable();
		t.npcSeen("B", 1, 0);
		t.npcSeen("A", 2, 10);
		assertEquals(Arrays.asList("B", "A"), labels(t, Section.NPCS));
	}

	@Test
	public void wikiMonstersMergeByNameAndCanBeRemoved()
	{
		final ThingTable t = new ThingTable();
		t.wikiMonster("Vorkath", Arrays.asList(8058, 8060), Section.THIS_MONSTER);
		final ThingKey key = t.wikiMonster("Vorkath", Arrays.asList(8059, 8061), Section.THIS_MONSTER);
		final ThingView v = only(t, Section.THIS_MONSTER);
		assertEquals(Arrays.asList(8058, 8059, 8060, 8061), v.ids());
		assertEquals(Source.WIKI, v.source());
		assertEquals(0, v.seen());
		assertTrue(v.changeable());
		t.remove(key);
		assertEquals(0, t.size());
		assertTrue(t.sectionViews().isEmpty());
	}

	@Test
	public void pictureQueue()
	{
		final ThingTable t = new ThingTable();
		final ThingKey a = t.npcSeen("A", 1, 0);
		final ThingKey b = t.npcSeen("B", 2, 0);
		final ThingKey c = t.objectSpawned("C", 3, new Place(1, 1, 0), 0);
		t.projectileSeen(4, 0);
		assertEquals(Arrays.asList(a, b, c), t.needingPictures());
		t.pictureTaken(a, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
		assertEquals(Arrays.asList(b, c), t.needingPictures());
		t.pictureMissed(b);
		t.pictureMissed(b);
		t.pictureMissed(b);
		assertEquals(Arrays.asList(c), t.needingPictures());
	}

	@Test
	public void picturesStopAt400()
	{
		final ThingTable t = new ThingTable();
		for (int i = 0; i < 300; i++)
		{
			t.npcSeen("N" + i, i, 0);
		}
		for (int i = 0; i < 101; i++)
		{
			t.objectSpawned("O" + i, i, new Place(i, 0, 0), 0);
		}
		assertEquals(401, t.size());
		final List<ThingKey> wanted = t.needingPictures();
		assertEquals(401, wanted.size());
		final BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		for (int i = 0; i < 400; i++)
		{
			t.pictureTaken(wanted.get(i), img);
		}
		assertTrue(t.needingPictures().isEmpty());
	}

	@Test
	public void actionsDropNullsAndBlanks()
	{
		final ThingTable t = new ThingTable();
		final ThingKey k = t.npcSeen("Guard", 1, 0);
		t.setActions(k, Arrays.asList("Attack", null, "", "Examine"));
		assertEquals(Arrays.asList("Attack", "Examine"), only(t, Section.NPCS).actions());
	}

	@Test
	public void aDroppedThingCountsOnce()
	{
		final ThingTable t = new ThingTable();
		for (int i = 0; i < 300; i++)
		{
			t.npcSeen("N" + i, i, 0);
		}
		t.npcSeen("N300", 300, 0);
		t.npcSeen("N300", 300, 0);
		t.npcSeen("N300", 300, 0);
		assertEquals(1, t.dropped(Section.NPCS));
		t.npcSeen("N301", 301, 0);
		assertEquals(2, t.dropped(Section.NPCS));
	}

	@Test
	public void lastSeenMovesOnlyAKnownThing()
	{
		final ThingTable t = new ThingTable();
		final ThingKey k = t.npcSeen("Vorkath", 1, 0);
		t.lastSeen(k, 5000);
		assertEquals(5000, only(t, Section.NPCS).lastSeenMs());
		assertEquals(1, only(t, Section.NPCS).seen());
		t.lastSeen(ThingKey.named(Kind.NPC, Section.NPCS, "Nobody"), 9000);
		assertEquals(5000, only(t, Section.NPCS).lastSeenMs());
		assertEquals(1, list(t, Section.NPCS).size());
	}
}
