package com.scenecustomizer;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/** {@link RowText}: the words of a row's second line. */
public class RowTextTest
{
	private static Thing thing(ThingKey key, int seen, long firstSeenMs)
	{
		final Thing t = new Thing(key, firstSeenMs);
		t.seen = seen;
		return t;
	}

	@Test
	public void timeIsMinutesAndSeconds()
	{
		assertEquals("0:00", RowText.time(0));
		assertEquals("1:12", RowText.time(72000));
		assertEquals("1:02:03", RowText.time(3723000));
	}

	@Test
	public void idsShowThreeThenACount()
	{
		assertEquals("8059, 8061", RowText.ids(Arrays.asList(8059, 8061), 0));
		assertEquals("1, 2, 3 +4", RowText.ids(Arrays.asList(1, 2, 3, 4, 5), 2));
		assertEquals("", RowText.ids(Collections.<Integer>emptyList(), 0));
	}

	@Test
	public void aRecordedNpc()
	{
		final Thing t = thing(ThingKey.named(Kind.NPC, Section.NPCS, "Zombified Spawn"), 6, 72000L);
		t.addId(8062);
		t.addId(8063);
		assertEquals("8062, 8063   seen 6   1:12", RowText.secondLine(new ThingView(t)));
	}

	@Test
	public void effects()
	{
		final Thing projectile = thing(ThingKey.byId(Kind.EFFECT, Section.EFFECTS, EffectKind.PROJECTILE, 1477), 24, 5000L);
		projectile.addHint("Vorkath");
		assertEquals("on Vorkath   seen 24   0:05", RowText.secondLine(new ThingView(projectile)));
		final Thing graphic = thing(ThingKey.byId(Kind.EFFECT, Section.EFFECTS, EffectKind.GRAPHIC, 1483), 8, 40000L);
		assertEquals("seen 8   0:40", RowText.secondLine(new ThingView(graphic)));
	}

	@Test
	public void aWikiRow()
	{
		final Thing t = thing(ThingKey.named(Kind.NPC, Section.THIS_MONSTER, "Vorkath"), 0, 0L);
		t.source = Source.WIKI;
		for (int id = 8058; id <= 8061; id++)
		{
			t.addId(id);
		}
		assertEquals("From the wiki   8058, 8059, 8060 +1", RowText.secondLine(new ThingView(t)));
	}

	@Test
	public void aSpell()
	{
		final Thing t = thing(ThingKey.named(Kind.SPELL, Section.SPELLS, "Crumble Undead"), 3, 80000L);
		assertEquals("seen 3   1:20", RowText.secondLine(new ThingView(t)));
	}
}
