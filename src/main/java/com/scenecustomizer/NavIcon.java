package com.scenecustomizer;

import com.scenecustomizer.SceneCustomizerConfig.SidebarIcon;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import net.runelite.client.ui.ColorScheme;

/**
 * The sidebar button's icon, 16 x 16 px: the 2hBuilds coin top-left ({@link CoinGlyph}) and one of the ten pictures the
 * player can pick ({@link SidebarIcon}), clear of the coin. Drawn in code, one opaque pixel a letter of {@link #ROWS},
 * so it needs no image file. The settings menu shows the same image beside each choice.
 */
public final class NavIcon
{
	/** The sidebar icon size. */
	public static final int SIZE = 16;

	/**
	 * The pictures, one block of sixteen rows of sixteen per {@link SidebarIcon}, in the enum's order: {@code .} leaves
	 * the coin or the transparent background showing. The letters are the colours of {@link #colourOf}.
	 */
	private static final String[][] ROWS = {
		// OUTLINED_NPC (S92)
		{
			"................",
			"................",
			"................",
			"...........CCC..",
			"..........C...C.",
			"..........C...C.",
			"..........C...C.",
			"...........CCC..",
			"................",
			"..........CCCCC.",
			"..WWWW...C.....C",
			".WRRRRW.C......C",
			".WRPRRW.C......C",
			".WRRRRW.C......C",
			".WRRRrW.C......C",
			"..WWWW..CCCCCCCC"
		},
		// NPC_AND_BADGE (S51)
		{
			"................",
			"................",
			"................",
			"...........LCC..",
			"..........LCCCc.",
			"..........CCCCc.",
			"..........CCCCc.",
			"...........Ccc..",
			"................",
			"..........LCCCc.",
			"..WWWW...LCCCCCc",
			".WRRRRW.LCCCCCCc",
			".WRPRRW.CCCCCCCc",
			".WRRRRW.CCCCCCCc",
			".WRRRrW.CCCCCCCc",
			"..WWWW..CCCCCCcc"
		},
		// NPC_CRATE_BARREL (S55)
		{
			"............LC..",
			"...........LCCc.",
			"...........CCCc.",
			"............Cc..",
			"................",
			"...........LCCc.",
			"..........LCCCcc",
			"................",
			"................",
			"................",
			".QOOOOO....TTTT.",
			".OooooO...GGGGGG",
			".OoQQoO...TQTTTt",
			".OoQQoO...TQTTTt",
			".OooooO...GGGGGG",
			".OOOOOo....TTTt."
		},
		// THREE_TILES (S58)
		{
			"................",
			"..........OOOOOO",
			"..........O.LC.o",
			"..........O.CC.o",
			"..........OLCCco",
			"..........OCCCco",
			"..........Oooooo",
			"................",
			"................",
			"................",
			".QOOOOO....rRRr.",
			".OooooO...rPRRRr",
			".OoQQoO...RRRRRR",
			".OoQQoO...RRRRRR",
			".OooooO...rRRRRr",
			".OOOOOo....rRRr."
		},
		// DASHED_BOX (S62)
		{
			"................",
			"...........rRr..",
			"..........rPRRr.",
			"..........RRRRR.",
			"..........rRRRr.",
			"...........rRr..",
			"................",
			"................",
			".........OO..OO.",
			"........O..LC..O",
			"........O..CC..O",
			"................",
			"..........LCCc..",
			"........O.CCCc.O",
			"........O......O",
			".........OO..OO."
		},
		// LIST_AND_BADGE (S69)
		{
			"................",
			"..........OO.WWW",
			"..........OO.WWW",
			"................",
			"..........OO.WW.",
			"..........OO.WW.",
			"................",
			"................",
			"..........WWWW..",
			".........WRRRRW.",
			"...LC...WRPRRRRW",
			"..LCCc..WRRRRRRW",
			"...Cc...WRRRRRRW",
			"..LCCc..WRRRRRrW",
			".LCCCCc..WRRRrW.",
			".CCCCcc...WWWW.."
		},
		// NPC_OVER_LIST (S36)
		{
			"............LC..",
			"...........LCCc.",
			"...........CCCc.",
			"............Cc..",
			"................",
			"...........LCCc.",
			"..........LCCCcc",
			"................",
			"..........WWWW..",
			".........WRRRRW.",
			"OO.WWWW.WRPRRRRW",
			"OO.WWWW.WRRRRRRW",
			"........WRRRRRRW",
			"OO.WWW..WRRRRRrW",
			"OO.WWW...WRRRrW.",
			"..........WWWW.."
		},
		// DASHED_BOX_AND_CRATE (S106)
		{
			"................",
			"...........rRr..",
			"..........rPRRr.",
			"..........RRRRR.",
			"..........rRRRr.",
			"...........rRr..",
			"................",
			"................",
			".........OO..OO.",
			"........O..LC..O",
			".QOOOOO.O..CC..O",
			".OooooO.........",
			".OoQQoO...LCCc..",
			".OoQQoO.O.CCCc.O",
			".OooooO.O......O",
			".OOOOOo..OO..OO."
		},
		// BOXED_NPC_AND_LIST (S116)
		{
			"................",
			"..........OOOOOO",
			"..........O.LC.o",
			"..........O.CC.o",
			"..........OLCCco",
			"..........OCCCco",
			"..........Oooooo",
			"................",
			"..........WWWW..",
			".........WRRRRW.",
			"OO.WWWW.WRPRRRRW",
			"OO.WWWW.WRRRRRRW",
			"........WRRRRRRW",
			"OO.WWW..WRRRRRrW",
			"OO.WWW...WRRRrW.",
			"..........WWWW.."
		},
		// BOXED_NPC (S61)
		{
			"................",
			"...........rRr..",
			"..........rPRRr.",
			"..........RRRRR.",
			"..........rRRRr.",
			"...........rRr..",
			"................",
			"................",
			".........OOOOOO.",
			"........O..LC..o",
			"........O..CC..o",
			"........O......o",
			"........O.LCCc.o",
			"........OLCCCCco",
			"........OCCCCCco",
			".........oooooo."
		}
	};

	/** The list's bullets and the boxes' lines: RuneLite's brand orange. */
	private static final Color ORANGE = ColorScheme.BRAND_ORANGE;
	private static final Color ORANGE_DARK = new Color(150, 92, 0);
	private static final Color ORANGE_LIGHT = new Color(248, 186, 76);
	/** The NPC: cyan, with a light and a dark edge. */
	private static final Color NPC = new Color(0, 214, 214);
	private static final Color NPC_LIGHT = new Color(150, 242, 242);
	private static final Color NPC_DARK = new Color(0, 146, 158);
	/** The badge's ring and the list's lines. */
	private static final Color WHITE = new Color(240, 240, 240);
	/** The crate's and the barrel's metal. */
	private static final Color GREY = new Color(128, 132, 138);
	/** The record badge: red, with a highlight and a dark edge. */
	private static final Color BADGE = new Color(224, 66, 58);
	private static final Color BADGE_LIGHT = new Color(255, 142, 130);
	private static final Color BADGE_DARK = new Color(150, 36, 36);
	/** The barrel's wood. */
	private static final Color WOOD = new Color(160, 108, 56);
	private static final Color WOOD_DARK = new Color(110, 72, 36);

	private NavIcon()
	{
	}

	/** A new 16 x 16 ARGB image of the chosen icon. */
	public static BufferedImage create(SidebarIcon icon)
	{
		final String[] rows = ROWS[icon.ordinal()];
		final BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		try
		{
			CoinGlyph.paint(g);
			for (int y = 0; y < rows.length; y++)
			{
				for (int x = 0; x < rows[y].length(); x++)
				{
					final Color colour = colourOf(rows[y].charAt(x));
					if (colour != null)
					{
						g.setColor(colour);
						g.fillRect(x, y, 1, 1);
					}
				}
			}
		}
		finally
		{
			g.dispose();
		}
		return img;
	}

	private static Color colourOf(char letter)
	{
		switch (letter)
		{
			case 'O':
				return ORANGE;
			case 'o':
				return ORANGE_DARK;
			case 'Q':
				return ORANGE_LIGHT;
			case 'C':
				return NPC;
			case 'L':
				return NPC_LIGHT;
			case 'c':
				return NPC_DARK;
			case 'W':
				return WHITE;
			case 'G':
				return GREY;
			case 'R':
				return BADGE;
			case 'P':
				return BADGE_LIGHT;
			case 'r':
				return BADGE_DARK;
			case 'T':
				return WOOD;
			case 't':
				return WOOD_DARK;
			default:
				return null;
		}
	}
}
