package com.scenecustomizer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

/**
 * The plugin's settings. Flat on purpose: every item is declared here and this interface extends {@link Config}
 * alone, because RuneLite stores defaults only for the methods declared on the interface it is given. The group and
 * the keys are frozen at first release; renaming one silently discards every player's setting.
 */
@ConfigGroup(SceneCustomizerConfig.GROUP)
public interface SceneCustomizerConfig extends Config
{
	String GROUP = "scene-customizer";

	/** The drop-down that picks the sidebar button's picture. */
	String SIDEBAR_ICON_KEY = "sidebarIcon";

	/** Whether the sidebar's look-up bar may ask the OSRS wiki. */
	@ConfigItem(
		keyName = "useWiki",
		name = "Use the OSRS wiki",
		description = "Look up monsters by name on the OSRS wiki. Sends only what you type to oldschool.runescape.wiki.",
		position = 1
	)
	default boolean useWiki()
	{
		return true;
	}

	/** Which picture the sidebar button shows; "Three tiles" unless the player picks another. */
	@ConfigItem(
		keyName = SIDEBAR_ICON_KEY,
		name = "Sidebar icon",
		description = "Which picture the plugin's sidebar button shows.",
		position = 2
	)
	default SidebarIcon sidebarIcon()
	{
		return SidebarIcon.THREE_TILES;
	}

	/** The pictures the sidebar button can show, in the menu's order; each is shown as its words. */
	enum SidebarIcon
	{
		OUTLINED_NPC("Outlined NPC"),
		NPC_AND_BADGE("NPC and badge"),
		NPC_CRATE_BARREL("NPC, crate and barrel"),
		THREE_TILES("Three tiles"),
		DASHED_BOX("Dashed box"),
		LIST_AND_BADGE("List and badge"),
		NPC_OVER_LIST("NPC over a list"),
		DASHED_BOX_AND_CRATE("Dashed box and crate"),
		BOXED_NPC_AND_LIST("Boxed NPC and list"),
		BOXED_NPC("Boxed NPC");

		private final String words;

		SidebarIcon(String words)
		{
			this.words = words;
		}

		@Override
		public String toString()
		{
			return words;
		}
	}
}
