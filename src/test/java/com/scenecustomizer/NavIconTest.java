package com.scenecustomizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.scenecustomizer.SceneCustomizerConfig.SidebarIcon;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

/**
 * {@link NavIcon}, in memory, for all ten icons: the size, the coin exactly as {@link CoinGlyph} draws it, the picture
 * clear of it, every pixel against the test's own copy of the rows.
 */
public class NavIconTest
{
	/** The coin's centre and the farthest its jittered rim reaches from it, with a pixel of margin. */
	private static final double COIN_CENTRE = 4.5;
	private static final double CLEAR_DISTANCE = 6.0;

	/** The test's own thirteen colours, by letter. */
	private static final String LETTERS = "OoQCcLWGRrPTt";
	private static final int[] RGBS = {
		rgb(220, 138, 0), rgb(150, 92, 0), rgb(248, 186, 76), rgb(0, 214, 214), rgb(0, 146, 158), rgb(150, 242, 242),
		rgb(240, 240, 240), rgb(128, 132, 138), rgb(224, 66, 58), rgb(150, 36, 36), rgb(255, 142, 130),
		rgb(160, 108, 56), rgb(110, 72, 36)
	};

	private static int rgb(int r, int g, int b)
	{
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	private static BufferedImage coinAlone()
	{
		final BufferedImage img = new BufferedImage(NavIcon.SIZE, NavIcon.SIZE, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		try
		{
			CoinGlyph.paint(g);
		}
		finally
		{
			g.dispose();
		}
		return img;
	}

	/** The test's copy of the ten blocks' rows, in file order. */
	private static List<List<String>> blocks() throws IOException
	{
		final List<List<String>> found = new ArrayList<>();
		try (InputStream in = NavIconTest.class.getResourceAsStream("nav-icons.txt");
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.startsWith("== "))
				{
					found.add(new ArrayList<>());
				}
				else if (!line.trim().isEmpty())
				{
					found.get(found.size() - 1).add(line.trim());
				}
			}
		}
		return found;
	}

	@Test
	public void everyIconIsSixteenBySixteenArgb()
	{
		for (SidebarIcon which : SidebarIcon.values())
		{
			final BufferedImage icon = NavIcon.create(which);
			assertEquals(which.name(), 16, icon.getWidth());
			assertEquals(which.name(), 16, icon.getHeight());
			assertEquals(which.name(), BufferedImage.TYPE_INT_ARGB, icon.getType());
		}
	}

	@Test
	public void theCoinIsDrawnPixelForPixelOnEveryIcon()
	{
		final BufferedImage coin = coinAlone();
		int coinPixels = 0;
		for (SidebarIcon which : SidebarIcon.values())
		{
			final BufferedImage icon = NavIcon.create(which);
			for (int y = 0; y < NavIcon.SIZE; y++)
			{
				for (int x = 0; x < NavIcon.SIZE; x++)
				{
					if ((coin.getRGB(x, y) >>> 24) != 0)
					{
						coinPixels++;
						assertEquals(which + " coin pixel " + x + "," + y, coin.getRGB(x, y), icon.getRGB(x, y));
					}
				}
			}
		}
		assertTrue("the coin drew something", coinPixels > 0);
	}

	@Test
	public void everyPictureLiesClearOfTheCoinsCircle()
	{
		final BufferedImage coin = coinAlone();
		for (SidebarIcon which : SidebarIcon.values())
		{
			final BufferedImage icon = NavIcon.create(which);
			int pictureOnly = 0;
			for (int y = 0; y < NavIcon.SIZE; y++)
			{
				for (int x = 0; x < NavIcon.SIZE; x++)
				{
					if (icon.getRGB(x, y) != coin.getRGB(x, y))
					{
						pictureOnly++;
						final double dx = x + 0.5 - COIN_CENTRE;
						final double dy = y + 0.5 - COIN_CENTRE;
						assertTrue(which + " picture pixel " + x + "," + y + " is too near the coin",
							Math.sqrt(dx * dx + dy * dy) >= CLEAR_DISTANCE);
					}
				}
			}
			assertTrue(which + " drew something", pictureOnly > 0);
		}
	}

	@Test
	public void everyIconIsItsPicturePixelForPixel() throws IOException
	{
		final BufferedImage coin = coinAlone();
		final List<List<String>> blocks = blocks();
		assertEquals(SidebarIcon.values().length, blocks.size());
		for (SidebarIcon which : SidebarIcon.values())
		{
			final List<String> rows = blocks.get(which.ordinal());
			final BufferedImage icon = NavIcon.create(which);
			assertEquals(which + " rows", 16, rows.size());
			for (int y = 0; y < NavIcon.SIZE; y++)
			{
				assertEquals(which + " row " + y, 16, rows.get(y).length());
				for (int x = 0; x < NavIcon.SIZE; x++)
				{
					final char letter = rows.get(y).charAt(x);
					final int want;
					if (letter == '.')
					{
						want = coin.getRGB(x, y);
					}
					else
					{
						final int at = LETTERS.indexOf(letter);
						assertTrue("known letter " + letter, at >= 0);
						want = RGBS[at];
					}
					assertEquals(which + " pixel " + x + "," + y, want, icon.getRGB(x, y));
				}
			}
		}
	}

	@Test
	public void theTenIconsAllDiffer()
	{
		final Set<String> seen = new HashSet<>();
		for (SidebarIcon which : SidebarIcon.values())
		{
			final BufferedImage icon = NavIcon.create(which);
			final StringBuilder pixels = new StringBuilder();
			for (int y = 0; y < NavIcon.SIZE; y++)
			{
				for (int x = 0; x < NavIcon.SIZE; x++)
				{
					pixels.append(icon.getRGB(x, y)).append(',');
				}
			}
			assertTrue(which + " is the same picture as another", seen.add(pixels.toString()));
		}
		assertEquals(10, seen.size());
	}

	@Test
	public void theOrangeIsRuneLitesBrandOrange()
	{
		final java.awt.Color orange = net.runelite.client.ui.ColorScheme.BRAND_ORANGE;
		assertEquals(0xFF000000 | (orange.getRGB() & 0xFFFFFF), NavIcon.create(SidebarIcon.NPC_OVER_LIST).getRGB(0, 10));
	}
}
