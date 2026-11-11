package com.scenecustomizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.NPCComposition;
import org.junit.Test;

/** {@link WikiNames}: the game's names and options win over the wiki's. */
public class WikiNamesTest
{
	@Test
	public void theGamesNameWins()
	{
		final Client client = mock(Client.class);
		final NPCComposition spawn = mock(NPCComposition.class);
		when(spawn.getName()).thenReturn("Zombified Spawn");
		when(spawn.getActions()).thenReturn(new String[]{"Attack", null, "Examine"});
		when(client.getNpcDefinition(8062)).thenReturn(spawn);
		when(client.getNpcDefinition(9999)).thenReturn(null);
		final BufferedImage picture = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final List<WikiMonster> monsters = Arrays.asList(
			new WikiMonster("Zombified spawn", Collections.singletonList(8062), null, picture),
			new WikiMonster("Mystery", Collections.singletonList(9999), null, null));

		final ThingTable table = new ThingTable();
		WikiNames.fill(client, mock(ModelPictures.class), table, monsters, Section.THIS_MONSTER, true);

		final List<ThingView> rows = table.sectionViews().get(Section.THIS_MONSTER);
		assertEquals(2, rows.size());
		assertEquals("Zombified Spawn", rows.get(0).label());
		assertEquals(Collections.singletonList(8062), rows.get(0).ids());
		assertEquals(Arrays.asList("Attack", "Examine"), rows.get(0).actions());
		assertNotNull(rows.get(0).picture());
		assertEquals(Source.WIKI, rows.get(0).source());
		assertEquals("Mystery", rows.get(1).label());
		assertTrue(rows.get(1).actions().isEmpty());

		final ThingTable linked = new ThingTable();
		WikiNames.fill(client, mock(ModelPictures.class), linked, monsters.subList(0, 1), Section.LINKED, false);
		assertTrue(linked.sectionViews().get(Section.LINKED).get(0).actions().isEmpty());
	}

	@Test
	public void aRemovedRowStaysRemoved()
	{
		final Client client = mock(Client.class);
		final BufferedImage picture = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final List<WikiMonster> own = Collections.singletonList(
			new WikiMonster("Cow", Collections.singletonList(2790), null, null));
		final List<WikiMonster> linked = Collections.singletonList(
			new WikiMonster("Cow calf", Collections.singletonList(2801), null, null));
		final ThingTable table = new ThingTable();
		WikiNames.fill(client, mock(ModelPictures.class), table, own, Section.THIS_MONSTER, true);
		WikiNames.fill(client, mock(ModelPictures.class), table, linked, Section.LINKED, false);
		table.remove(ThingKey.named(Kind.NPC, Section.LINKED, "Cow calf"));

		final List<WikiMonster> ownLater = Collections.singletonList(
			new WikiMonster("Cow", Collections.singletonList(2790), null, picture));
		final List<WikiMonster> linkedLater = Collections.singletonList(
			new WikiMonster("Cow calf", Collections.singletonList(2801), null, picture));
		WikiNames.pictures(client, table, ownLater, Section.THIS_MONSTER);
		WikiNames.pictures(client, table, linkedLater, Section.LINKED);

		final List<ThingView> cow = table.sectionViews().get(Section.THIS_MONSTER);
		assertEquals(1, cow.size());
		assertNotNull(cow.get(0).picture());
		final List<ThingView> calf = table.sectionViews().get(Section.LINKED);
		assertTrue(calf == null || calf.isEmpty());
	}
}
