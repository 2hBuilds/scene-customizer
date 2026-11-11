package com.scenecustomizer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/** {@link ChangesText}: the words under a rule in the Changes list. */
public class ChangesTextTest
{
	private static final List<Place> NO_PLACES = Collections.<Place>emptyList();

	@Test
	public void describes()
	{
		final Outline three = new Outline(0x123456, 3);
		assertEquals("Outline, width 3",
			ChangesText.describe(new Rule(Kind.NPC, "x", three, NO_PLACES, HideScope.OFF, NO_PLACES)));
		assertEquals("Hidden",
			ChangesText.describe(new Rule(Kind.NPC, "x", null, NO_PLACES, HideScope.EVERYWHERE, NO_PLACES)));
		assertEquals("Outline, width 3 - Hidden",
			ChangesText.describe(new Rule(Kind.NPC, "x", three, NO_PLACES, HideScope.EVERYWHERE, NO_PLACES)));
		final List<Place> one = Arrays.asList(new Place(1, 2, 0));
		final List<Place> two = Arrays.asList(new Place(1, 2, 0), new Place(3, 4, 0));
		assertEquals("Outline here, width 2", ChangesText.describe(
			new Rule(Kind.OBJECT, "x", new Outline(0x123456, 2), one, HideScope.OFF, NO_PLACES)));
		assertEquals("Hidden here (2 places)",
			ChangesText.describe(new Rule(Kind.OBJECT, "x", null, NO_PLACES, HideScope.HERE, two)));
		assertEquals("Hidden here",
			ChangesText.describe(new Rule(Kind.OBJECT, "x", null, NO_PLACES, HideScope.HERE, one)));
		assertEquals("Hidden everywhere",
			ChangesText.describe(new Rule(Kind.OBJECT, "x", null, NO_PLACES, HideScope.EVERYWHERE, NO_PLACES)));
	}
}
