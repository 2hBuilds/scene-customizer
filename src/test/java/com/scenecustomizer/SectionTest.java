package com.scenecustomizer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/** {@link Section}: the headings; none starts folded (Scenery nearby opened at the user's wish, 2026-11-10). */
public class SectionTest
{
	@Test
	public void titlesAndFolding()
	{
		final String[] titles = {"NPCs", "Objects", "Scenery nearby", "Effects", "Animations", "Your spells",
			"This monster", "Linked on its wiki page"};
		final Section[] sections = Section.values();
		assertEquals(titles.length, sections.length);
		for (int i = 0; i < titles.length; i++)
		{
			assertEquals(titles[i], sections[i].title());
			assertEquals(false, sections[i].foldedAtFirst());
		}
	}
}
