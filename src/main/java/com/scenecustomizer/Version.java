package com.scenecustomizer;

/**
 * The one place the plugin's version is written. The descriptor's description, the settings menu's header and the
 * start-up log line read it; the frame test checks it against {@code version=} in runelite-plugin.properties.
 */
public final class Version
{
	/** The version this build ships as, {@code major.minor.patch}. */
	public static final String CURRENT = "1.0.0";

	private Version()
	{
	}
}
