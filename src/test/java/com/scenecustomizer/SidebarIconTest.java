package com.scenecustomizer;

import static org.junit.Assert.assertEquals;

import com.scenecustomizer.SceneCustomizerConfig.SidebarIcon;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.mockito.Mockito;

/** The sidebar icon setting: the ten choices, their words and order, the default and the key. */
public class SidebarIconTest
{
	/** The test's own copy of the ten icons' header lines: {@code == CONSTANT | words | sheet}. */
	private static List<String[]> headers() throws IOException
	{
		final List<String[]> found = new ArrayList<>();
		try (InputStream in = SidebarIconTest.class.getResourceAsStream("nav-icons.txt");
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.startsWith("== "))
				{
					final String[] parts = line.substring(3).split("\\|");
					found.add(new String[] {parts[0].trim(), parts[1].trim()});
				}
			}
		}
		return found;
	}

	@Test
	public void theTenIconsInOrderWithTheirWords() throws IOException
	{
		final List<String[]> want = headers();
		assertEquals(10, want.size());
		final SidebarIcon[] have = SidebarIcon.values();
		assertEquals(want.size(), have.length);
		for (int i = 0; i < have.length; i++)
		{
			assertEquals("constant " + i, want.get(i)[0], have[i].name());
			assertEquals("words " + i, want.get(i)[1], have[i].toString());
		}
	}

	@Test
	public void theDefaultIsTheThreeTiles()
	{
		final SceneCustomizerConfig config = Mockito.mock(SceneCustomizerConfig.class, Mockito.CALLS_REAL_METHODS);
		assertEquals(SidebarIcon.THREE_TILES, config.sidebarIcon());
	}

	@Test
	public void theKeyIsSidebarIcon()
	{
		assertEquals("sidebarIcon", SceneCustomizerConfig.SIDEBAR_ICON_KEY);
		// That the config item is declared with this very constant is checked by the local-only SceneCustomizerConfigTest,
		// which may read the annotation; the shipped tests may not.
	}
}
